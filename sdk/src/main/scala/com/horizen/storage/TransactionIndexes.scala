package com.horizen.storage

import java.util.{List => JList}

import com.horizen.block.SidechainBlock
import com.horizen.utils.{ByteArrayWrapper, Pair => JPair, _}
import scorex.util._

import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._
import scala.util.Random

/**
 * @param transactionIndexes - storage for transaction indexes
 */
class TransactionIndexes(transactionIndexes: Storage) {

    def getBlockIdByTransactionId(transactionId: ModifierId): Option[ModifierId] = {
      val id: ByteArrayWrapper = idToBytes(transactionId)
      transactionIndexes.get(id).asScala.map(id => bytesToId(id.data))
    }

    private def nextVersion(): Array[Byte] = {
      val version = new Array[Byte](32)
      Random.nextBytes(version)
      version
    }

    def indexBlock(block: SidechainBlock): Unit = {
      val blockId: ByteArrayWrapper = idToBytes(block.id) // used as version as well
      val indexes: JList[JPair[ByteArrayWrapper, ByteArrayWrapper]] = block.transactions.map{transaction =>
        new JPair(new ByteArrayWrapper(idToBytes(transaction.id)), blockId)
      }.asJava

      //TODO Don't use not versioned version of a storage
      transactionIndexes.update(nextVersion(), indexes, java.util.Collections.emptyList())
    }
}
