package com.horizen.wallet
import java.time.{Instant => JInstant, LocalDate => JLocalDate, ZoneId => JZoneId}
import java.util.{List => JList}

import com.horizen.{OpenedWalletBox, SidechainTypes, WalletBox}
import com.horizen.box.Box
import com.horizen.proposition.Proposition
import com.horizen.transaction.BoxTransaction

import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

class SidechainWalletTransactionAPI
  extends WalletDataAPI
{

  private val transactions = new mutable.LinkedHashMap[JLocalDate, ListBuffer[SidechainTypes#SCBT]]()

  private def appendTransaction(tx: SidechainTypes#SCBT) : Unit = {
    val txDate = JInstant.ofEpochMilli(tx.timestamp).atZone(JZoneId.systemDefault()).toLocalDate
    transactions.get(txDate) match {
      case Some(txList) => txList.append(tx)
      case None => {
        val l = new ListBuffer[SidechainTypes#SCBT]()
        l.append(tx)
        transactions.put(txDate, l)
      }
    }
  }

  private def loadTransactions(walletReader: SidechainWalletReader) : Unit = {
    for ( tx <- walletReader.getAllTransaction.asScala) {
      appendTransaction(tx)
    }
  }

  override def onStartup(walletReader: SidechainWalletReader): Unit = {
    loadTransactions(walletReader)
  }

  override def update(walletReader: SidechainWalletReader, version: Array[Byte],
                      newBoxes: JList[WalletBox], openedBoxes: JList[OpenedWalletBox],
                      transactions: JList[BoxTransaction[Proposition, Box[Proposition]]]): Unit = {
    for (tx <- transactions.asScala)
      appendTransaction(tx)
  }

  override def rollback(walletReader: SidechainWalletReader, version: Array[Byte]): Unit = {
    transactions.clear()
    loadTransactions(walletReader)
  }

  def getTransactions(date: JLocalDate) : List[SidechainTypes#SCBT] = {
    transactions.get(date) match {
      case Some(txList) => txList.toList
      case None => List()
    }
  }
}
