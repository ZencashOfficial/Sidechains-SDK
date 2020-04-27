package com.horizen.storage

import java.util.Optional
import java.util.{ArrayList => JArrayList, List => JList}
import com.horizen.utils.{Pair => JPair}

import scala.collection.JavaConverters._

import com.horizen.SidechainTypes
import com.horizen.companion.SidechainTransactionsCompanion
import com.horizen.utils.ByteArrayWrapper

import scorex.crypto.hash.Blake2b256
import scorex.util.ScorexLogging
import scorex.util.idToBytes


import scala.util.{Failure, Success, Try}

class SidechainWalletTransactionStorage (storage : Storage, sidechainTransactionsCompanion: SidechainTransactionsCompanion)
extends SidechainTypes
with ScorexLogging
{
  // Version - block Id
  // Key - byte array transaction Id
  // No remove operation

  require(storage != null, "Storage must be NOT NULL.")
  require(sidechainTransactionsCompanion != null, "SidechainTransactionsCompanion must be NOT NULL.")

  def calculateKey(transactionId : Array[Byte]) : ByteArrayWrapper = {
    new ByteArrayWrapper(Blake2b256.hash(transactionId))
  }

  def get (transactionId : Array[Byte]) : Option[SidechainTypes#SCBT] = {
    storage.get(calculateKey(transactionId)) match {
      case v if v.isPresent => {
        sidechainTransactionsCompanion.parseBytesTry(v.get().data) match {
          case Success(transaction) => Option(transaction.asInstanceOf[SidechainTypes#SCBT])
          case Failure(exception) => {
            log.error("Error while Transaction parsing.", exception)
            Option.empty
          }
        }
      }
      case _ => Option.empty
    }
  }

  def getAll : List[SidechainTypes#SCBT] = {
    storage.getAll.asScala.map(baw => {
      sidechainTransactionsCompanion.parseBytes(baw.getValue.data).asInstanceOf[SidechainTypes#SCBT]
    }).toList
  }

  def update (version : ByteArrayWrapper, transactionUpdateList : Seq[SidechainTypes#SCBT]) : Try[SidechainWalletTransactionStorage] = Try {
    require(transactionUpdateList != null, "List of Transactions to add/update must be NOT NULL. Use empty List instead.")
    require(!transactionUpdateList.contains(null), "Transactions to add/update must be NOT NULL.")

    val updateList = new JArrayList[JPair[ByteArrayWrapper,ByteArrayWrapper]]()

    for (tx <- transactionUpdateList)
      updateList.add(new JPair[ByteArrayWrapper, ByteArrayWrapper](calculateKey(idToBytes(tx.id)),
        new ByteArrayWrapper(sidechainTransactionsCompanion.toBytes(tx))))

    storage.update(version,
      updateList,
      new JArrayList[ByteArrayWrapper]())

    this
  }

  def lastVersionId : Optional[ByteArrayWrapper] = {
    storage.lastVersionID()
  }

  def rollbackVersions : List[ByteArrayWrapper] = {
    storage.rollbackVersions().asScala.toList
  }

  def rollback (version : ByteArrayWrapper) : Try[SidechainWalletTransactionStorage] = Try {
    require(version != null, "Version to rollback to must be NOT NULL.")
    storage.rollback(version)
    this
  }

  def isEmpty: Boolean = storage.isEmpty

}
