package com.horizen

import java.io.{File => JFile}
import java.lang
import java.time.{Instant, ZoneId}
import java.util.{List => JList, Optional => JOptional}

import com.horizen.block.SidechainBlock
import com.horizen.box.Box
import com.horizen.companion.{SidechainBoxesCompanion, SidechainSecretsCompanion}
import com.horizen.wallet.{ApplicationWallet, SidechainWalletReader, SidechainWalletTransactionAPI}
import com.horizen.node.NodeWallet
import com.horizen.params.StorageParams
import com.horizen.proposition.Proposition
import com.horizen.secret.{PrivateKey25519, PrivateKey25519Creator, Secret}
import com.horizen.storage.{IODBStoreAdapter, SidechainOpenedWalletBoxStorage, SidechainSecretStorage, SidechainWalletBoxStorage, SidechainWalletTransactionStorage}
import com.horizen.transaction.{BoxTransaction, Transaction}
import com.horizen.utils.{ByteArrayWrapper, BytesUtils}
import io.iohk.iodb.LSMStore
import scorex.core.{VersionTag, bytesToVersion, idToVersion}
import scorex.util.ScorexLogging
import com.horizen.utils.BytesUtils

import scala.util.{Failure, Random, Success, Try}
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer


trait Wallet[S <: Secret, P <: Proposition, TX <: Transaction, PMOD <: scorex.core.PersistentNodeViewModifier, W <: Wallet[S, P, TX, PMOD, W]]
  extends scorex.core.transaction.wallet.Vault[TX, PMOD, W] {
  self: W =>

  def addSecret(secret: S): Try[W]

  def removeSecret(publicImage: P): Try[W]

  def secret(publicImage: P): Option[S]

  def secrets(): Set[S]

  def boxes(): Seq[WalletBox]

  def publicKeys(): Set[P]
}

case class UpdateException (val e: Throwable, val rollbackOperations: Seq[(ByteArrayWrapper, ByteArrayWrapper => Try[Any])])
  extends Throwable

class SidechainWallet private[horizen] (seed: Array[Byte],
                                        walletBoxStorage: SidechainWalletBoxStorage,
                                        secretStorage: SidechainSecretStorage,
                                        walletTransactionStorage: SidechainWalletTransactionStorage,
                                        openedWalletBoxStorage: SidechainOpenedWalletBoxStorage,
                                        applicationWallet: ApplicationWallet)
  extends Wallet[SidechainTypes#SCS,
                 SidechainTypes#SCP,
                 SidechainTypes#SCBT,
                 SidechainBlock,
                 SidechainWallet]
  with SidechainTypes
  with NodeWallet
  with SidechainWalletReader
{
  override type NVCT = SidechainWallet

  require(applicationWallet != null, "ApplicationWallet must be NOT NULL.")

  private val transactionsAPI = new SidechainWalletTransactionAPI()

  transactionsAPI.onStartup(this)

  // 1) check for existence
  // 2) try to store in SecretStore using SidechainSecretsCompanion
  override def addSecret(secret: SidechainTypes#SCS): Try[SidechainWallet] = Try {
    require(secret != null, "Secret must be NOT NULL.")
    secretStorage.add(secret).get
    //TODO (Alberto) should we catch user exception here (and don't return it outside)???
    applicationWallet.onAddSecret(secret)
    this
  }

  // 1) check for existence
  // 2) remove from SecretStore (note: provide a unique version to SecretStore)
  override def removeSecret(publicImage: SidechainTypes#SCP): Try[SidechainWallet] = Try {
    require(publicImage != null, "PublicImage must be NOT NULL.")
    secretStorage.remove(publicImage).get
    applicationWallet.onRemoveSecret(publicImage)
    this
  }

  override def secret(publicImage: SidechainTypes#SCP): Option[SidechainTypes#SCS] = {
    secretStorage.get(publicImage)
  }

  override def secrets(): Set[SidechainTypes#SCS] = {
    secretStorage.getAll.toSet
  }

  override def boxes(): Seq[WalletBox] = {
    walletBoxStorage.getAll
  }

  override def publicKeys(): Set[SidechainTypes#SCP] = {
    secretStorage.getAll.map(_.publicImage()).toSet
  }

  // just do nothing, we don't need to care about offchain objects inside the wallet
  override def scanOffchain(tx: SidechainTypes#SCBT): SidechainWallet = this

  // just do nothing, we don't need to care about offchain objects inside the wallet
  override def scanOffchain(txs: Seq[SidechainTypes#SCBT]): SidechainWallet = this

  // scan like in HybridApp, but in more general way.
  // update boxes in BoxStore
  override def scanPersistent(modifier: SidechainBlock): SidechainWallet = {
    //require(modifier != null, "SidechainBlock must be NOT NULL.")
    val version = BytesUtils.fromHexString(modifier.id)
    val changes = SidechainState.changes(modifier).get
    val pubKeys = publicKeys()
    val boxesInWallet = boxes().map(_.box.id())

    val rollbackList = new ListBuffer[(ByteArrayWrapper, ByteArrayWrapper => Try[Any])]

    val txBoxes: Map[ByteArrayWrapper, SidechainTypes#SCBT] = modifier.transactions
      .foldLeft(Map.empty[ByteArrayWrapper, SidechainTypes#SCBT]) {
        (accMap, tx) => accMap ++ tx.boxIdsToOpen().asScala.map(boxId => boxId -> tx) ++
          tx.newBoxes().asScala.map(b => new ByteArrayWrapper(b.id()) -> tx)
      }
    
    val newBoxes = changes.toAppend.filter(s => pubKeys.contains(s.box.proposition()))
        .map(_.box)
        .map { box =>
               val boxTransaction = txBoxes(new ByteArrayWrapper(box.id()))
               new WalletBox(box, boxTransaction.id, boxTransaction.timestamp())
        }

    val boxIdsToRemove = changes.toRemove.map(_.boxId.array)
      .filter(boxId => boxesInWallet.exists(b => java.util.Arrays.equals(boxId, b)))

    val transactions = (for (boxId <- (newBoxes.map(_.box.id()) ++ boxIdsToRemove))
      yield txBoxes(new ByteArrayWrapper(boxId))).distinct

    val openedWalletBoxes = boxes().filter(wb => boxIdsToRemove.exists(b => java.util.Arrays.equals(wb.box.id(), b)))
      .map(wb => new OpenedWalletBox(wb, txBoxes(new ByteArrayWrapper(wb.box.id()))))

    try {
      var ver = walletBoxStorage.lastVersionId

      walletBoxStorage.update(new ByteArrayWrapper(version), newBoxes.toList, boxIdsToRemove.toList) match {
        case Success(s) if ver.isPresent => rollbackList.append((ver.get(), ver => s.rollback(ver)))
        case Failure(e) => throw UpdateException(e, rollbackList)
      }

      ver = walletTransactionStorage.lastVersionId

      walletTransactionStorage.update(new ByteArrayWrapper(version), transactions) match {
        case Success(s) if ver.isPresent => rollbackList.append((ver.get(), ver => s.rollback(ver)))
        case Failure(e) => throw UpdateException(e, rollbackList)
      }

      ver = openedWalletBoxStorage.lastVersionId

      openedWalletBoxStorage.update(new ByteArrayWrapper(version), openedWalletBoxes.toSet) match {
        case Success(s) if ver.isPresent => rollbackList.append((ver.get(), ver => s.rollback(ver)))
        case Failure(e) => throw UpdateException(e, rollbackList)
      }

      transactionsAPI.update(this, version, newBoxes.toList.asJava, openedWalletBoxes.toList.asJava, transactions.toList.asJava)

    } catch {
      case exception: UpdateException => exception.rollbackOperations.foreach(f => f._2(f._1))
        throw exception.e
    }

    applicationWallet.onChangeBoxes(version, newBoxes.map(_.box).toList.asJava,
      boxIdsToRemove.toList.asJava)

    this
  }

  // rollback BoxStore only. SecretStore must not changed
  override def rollback(to: VersionTag): Try[SidechainWallet] = Try {
    require(to != null, "Version to rollback to must be NOT NULL.")
    val version = BytesUtils.fromHexString(to)
    walletBoxStorage.rollback(new ByteArrayWrapper(version)).get
    applicationWallet.onRollback(version)
    transactionsAPI.rollback(this, version)
    this
  }

  // Java NodeWallet interface definition
  override def allBoxes : JList[Box[Proposition]] = {
    walletBoxStorage.getAll.map(_.box).asJava
  }

  override def allBoxes(boxIdsToExclude: JList[Array[Byte]]): JList[Box[Proposition]] = {
    walletBoxStorage.getAll
      .filter((wb : WalletBox) => !BytesUtils.contains(boxIdsToExclude, wb.box.id()))
      .map(_.box)
      .asJava
  }

  override def boxesOfType(boxType: Class[_ <: Box[_ <: Proposition]]): JList[Box[Proposition]] = {
    walletBoxStorage.getByType(boxType)
      .map(_.box)
      .asJava
  }

  override def boxesOfType(boxType: Class[_ <: Box[_ <: Proposition]], boxIdsToExclude: JList[Array[Byte]]): JList[Box[Proposition]] = {
    walletBoxStorage.getByType(boxType)
      .filter((wb : WalletBox) => !BytesUtils.contains(boxIdsToExclude, wb.box.id()))
      .map(_.box)
      .asJava
  }

  override def boxesBalance(boxType: Class[_ <: Box[_ <: Proposition]]): java.lang.Long = {
    walletBoxStorage.getBoxesBalance(boxType)
  }

  override def secretByPublicKey(publicKey: Proposition): JOptional[Secret] = {
    secretStorage.get(publicKey) match {
      case Some(secret) => JOptional.of(secret)
      case None => JOptional.empty()
    }
  }

  override def allSecrets(): JList[Secret] = {
    secretStorage.getAll.asJava
  }

  override def secretsOfType(secretType: Class[_ <: Secret]): JList[Secret] = {
    secretStorage.getAll.filter(_.getClass.equals(secretType)).asJava
  }

  override def allBoxesBalance(): lang.Long = {
    walletBoxStorage.getAll.map(_.box.value()).sum
  }

  override def walletSeed(): Array[Byte] = seed

  override def getWalletBox(boxId: Array[Byte]): JOptional[WalletBox] = {
    walletBoxStorage.get(boxId).orElse(openedWalletBoxStorage.get(boxId)) match {
      case Some(wb) => JOptional.of(wb)
      case None => JOptional.empty()
    }
  }

  override def getAllWalletBoxes: JList[WalletBox] = {
    (walletBoxStorage.getAll ++ openedWalletBoxStorage.getAll).asJava
  }

  override def getOpenedWalletBox(boxId: Array[Byte]): JOptional[OpenedWalletBox] = {
    openedWalletBoxStorage.get(boxId) match {
      case Some(wb) => JOptional.of(wb)
      case None => JOptional.empty()
    }
  }

  override def getAllOpenedWalletBoxes: JList[OpenedWalletBox] = {
    openedWalletBoxStorage.getAll.asJava
  }

  override def getClosedWalletBox(boxId: Array[Byte]): JOptional[WalletBox] = {
    walletBoxStorage.get(boxId) match {
      case Some(wb) => JOptional.of(wb)
      case None => JOptional.empty()
    }
  }

  override def getAllClosedWalletBoxes: JList[WalletBox] = {
    walletBoxStorage.getAll.asJava
  }

  override def getTransaction(transactionId: Array[Byte]): JOptional[BoxTransaction[SCP, Box[SCP]]] = {
    walletTransactionStorage.get(transactionId) match {
      case Some(transaction) => JOptional.of(transaction)
      case None => JOptional.empty()
    }
  }

  override def getAllTransaction: JList[BoxTransaction[SCP, Box[SCP]]] = {
    walletTransactionStorage.getAll.asJava
  }
}

object SidechainWallet
{
  private[horizen] def restoreWallet(seed: Array[Byte],
                                     walletBoxStorage: SidechainWalletBoxStorage,
                                     secretStorage: SidechainSecretStorage,
                                     walletTransactionStorage: SidechainWalletTransactionStorage,
                                     openedWalletBoxStorage: SidechainOpenedWalletBoxStorage,
                                     applicationWallet: ApplicationWallet) : Option[SidechainWallet] = {

    if (!walletBoxStorage.isEmpty)
      Some(new SidechainWallet(seed, walletBoxStorage, secretStorage, walletTransactionStorage,
        openedWalletBoxStorage, applicationWallet))
    else
      None
  }

  private[horizen] def genesisWallet(seed: Array[Byte],
                                     walletBoxStorage: SidechainWalletBoxStorage,
                                     secretStorage: SidechainSecretStorage,
                                     walletTransactionStorage: SidechainWalletTransactionStorage,
                                     openedWalletBoxStorage: SidechainOpenedWalletBoxStorage,
                                     applicationWallet: ApplicationWallet,
                                     genesisBlock: SidechainBlock) : Try[SidechainWallet] = Try {

    if (walletBoxStorage.isEmpty)
      new SidechainWallet(seed, walletBoxStorage, secretStorage, walletTransactionStorage,
        openedWalletBoxStorage, applicationWallet)
        .scanPersistent(genesisBlock)
    else
      throw new RuntimeException("WalletBox storage is not empty!")
  }
}
