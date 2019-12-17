package com.horizen.fixtures

import java.lang.{Byte => JByte}
import java.util.{HashMap => JHashMap}

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler}
import akka.stream.ActorMaterializer
import com.horizen.api.http.{SidechainApiErrorHandler, SidechainTransactionActorRef, SidechainTransactionApiRoute}
import com.horizen.block.{ProofOfWorkVerifier, SidechainBlock, SidechainBlockSerializer}
import com.horizen.box.BoxSerializer
import com.horizen.companion.{SidechainBoxesCompanion, SidechainSecretsCompanion, SidechainTransactionsCompanion}
import com.horizen.customtypes.{DefaultApplicationState, DefaultApplicationWallet}
import com.horizen.params.{MainNetParams, NetworkParams, RegTestParams, TestNetParams}
import com.horizen.secret.{PrivateKey25519Serializer, SecretSerializer}
import com.horizen.state.ApplicationState
import com.horizen.storage._
import com.horizen.transaction.TransactionSerializer
import com.horizen.utils.BytesUtils
import com.horizen.wallet.ApplicationWallet
import com.horizen.{SidechainNodeViewHolderRef, SidechainSettings, SidechainSettingsReader, SidechainTypes}
import scorex.core.api.http.ApiRejectionHandler
import scorex.core.utils.NetworkTimeProvider

import scala.concurrent.ExecutionContext

trait SidechainNodeViewHolderFixture
  extends IODBStoreFixture
{

  val classLoader: ClassLoader = getClass.getClassLoader

  val sidechainSettings: SidechainSettings = SidechainSettingsReader.read(classLoader.getResource("sc_node_holder_fixter_settings.conf").getFile, None)

  implicit def exceptionHandler: ExceptionHandler = SidechainApiErrorHandler.exceptionHandler
  implicit def rejectionHandler: RejectionHandler = ApiRejectionHandler.rejectionHandler

  implicit val actorSystem: ActorSystem = ActorSystem(sidechainSettings.scorexSettings.network.agentName)
  implicit val executionContext: ExecutionContext = actorSystem.dispatchers.lookup("scorex.executionContext")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val timeProvider = new NetworkTimeProvider(sidechainSettings.scorexSettings.ntp)

  val sidechainBoxesCompanion: SidechainBoxesCompanion =  SidechainBoxesCompanion(new JHashMap[JByte, BoxSerializer[SidechainTypes#SCB]]())
  val sidechainSecretsCompanion: SidechainSecretsCompanion = SidechainSecretsCompanion(new JHashMap[JByte, SecretSerializer[SidechainTypes#SCS]]())
  val sidechainTransactionsCompanion: SidechainTransactionsCompanion = SidechainTransactionsCompanion(new JHashMap[JByte, TransactionSerializer[SidechainTypes#SCBT]]())
  val defaultApplicationWallet: ApplicationWallet = new DefaultApplicationWallet()
  val defaultApplicationState: ApplicationState = new DefaultApplicationState()


  val genesisBlock: SidechainBlock = new SidechainBlockSerializer(sidechainTransactionsCompanion).parseBytes(
    BytesUtils.fromHexString(sidechainSettings.genesisData.scGenesisBlockHex)
  )
  val genesisPowData: Seq[(Int, Int)] = ProofOfWorkVerifier.parsePowData(sidechainSettings.genesisData.powData)

  val params: NetworkParams = sidechainSettings.genesisData.mcNetwork match {
    case "regtest" => RegTestParams(
      BytesUtils.fromHexString(sidechainSettings.genesisData.scId),
      genesisBlock.id,
      genesisBlock.mainchainBlocks.head.hash,
      genesisPowData,
      sidechainSettings.genesisData.mcBlockHeight,
      sidechainSettings.genesisData.withdrawalEpochLength
    )
    case "testnet" => TestNetParams(
      BytesUtils.fromHexString(sidechainSettings.genesisData.scId),
      genesisBlock.id,
      genesisBlock.mainchainBlocks.head.hash,
      genesisPowData,
      sidechainSettings.genesisData.mcBlockHeight
    )
    case "mainnet" => MainNetParams(
      BytesUtils.fromHexString(sidechainSettings.genesisData.scId),
      genesisBlock.id,
      genesisBlock.mainchainBlocks.head.hash,
      genesisPowData,
      sidechainSettings.genesisData.mcBlockHeight,
      sidechainSettings.genesisData.withdrawalEpochLength
    )
    case _ => throw new IllegalArgumentException("Configuration file scorex.genesis.mcNetwork parameter contains inconsistent value.")
  }

  val sidechainSecretStorage = new SidechainSecretStorage(
    getStorage(),
    sidechainSecretsCompanion)
  val sidechainWalletBoxStorage = new SidechainWalletBoxStorage(
    getStorage(),
    sidechainBoxesCompanion)
  val sidechainStateStorage = new SidechainStateStorage(
    getStorage(),
    sidechainBoxesCompanion)
  val sidechainHistoryStorage = new SidechainBlocks(
    getStorage(),
    sidechainTransactionsCompanion, params)
  val sidechainWalletTransactionStorage = new SidechainWalletTransactionStorage(
    getStorage(),
    sidechainTransactionsCompanion)
  val transactionIndexesStorage = new TransactionIndexes(getStorage())

  // Append genesis secrets if we start the node first time
  if(sidechainSecretStorage.isEmpty) {
    for(secretHex <- sidechainSettings.wallet.genesisSecrets)
      sidechainSecretStorage.add(PrivateKey25519Serializer.getSerializer.parseBytes(BytesUtils.fromHexString(secretHex)))
  }

  val nodeViewHolderRef: ActorRef = SidechainNodeViewHolderRef(
    sidechainSettings,
    sidechainHistoryStorage,
    transactionIndexesStorage,
    sidechainStateStorage,
    sidechainWalletBoxStorage, sidechainSecretStorage, sidechainWalletTransactionStorage, params, timeProvider,
    defaultApplicationWallet, defaultApplicationState, genesisBlock)

  val sidechainTransactionActorRef : ActorRef = SidechainTransactionActorRef(nodeViewHolderRef)

  def getSidechainNodeViewHolderRef : ActorRef = {
    nodeViewHolderRef
  }

  def getSidechainTransactionApiRoute : SidechainTransactionApiRoute = {
    SidechainTransactionApiRoute(sidechainSettings.scorexSettings.restApi, nodeViewHolderRef, sidechainTransactionActorRef)
  }

}
