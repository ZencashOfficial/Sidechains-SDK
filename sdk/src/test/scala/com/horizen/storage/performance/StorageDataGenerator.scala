package com.horizen.storage.performance
import com.horizen.fixtures.TransactionFixture
import com.horizen.proposition.PublicKey25519Proposition
import com.horizen.secret.PrivateKey25519
import com.horizen.transaction.RegularTransaction
import com.horizen.utils.{ByteArrayWrapper, byteArrayToWrapper, Pair => JPair, _}
import scorex.util.idToBytes

import scala.util.Random

trait StorageDataGenerator {
  def buildGeneratedDataStream(): Stream[JPair[ByteArrayWrapper, ByteArrayWrapper]]
  def copyGenerator(): StorageDataGenerator
  def expectedAverageDataLen: Int
}

object StorageDataGenerator {
  @inline
  def mutateData(counter: Int, initialData: Array[Byte]): Array[Byte] = {
    val mutatedData: Array[Byte] = new Array[Byte](initialData.length)
    Array.copy(initialData, 0, mutatedData, 0, initialData.length)

    var index = 0
    var adding = counter

    while (adding != 0) {
      mutatedData(index) = (mutatedData(index) + (adding & 0xFF)).asInstanceOf[Byte]
      index += 1
      adding = adding >> 8
    }

    mutatedData
  }

  def mutateStorageData(initialData: JPair[ByteArrayWrapper, ByteArrayWrapper], counter: Int): JPair[ByteArrayWrapper, ByteArrayWrapper] = {
    val newKey: Array[Byte] = mutateData(counter, initialData.getKey)
    val newValue: Array[Byte] = mutateData(counter, initialData.getValue)
    new JPair(newKey, newValue)
  }
}

private class LruCache[K, V](val cacheSize: Int) extends java.util.LinkedHashMap[K, V] {
  override def removeEldestEntry(entry: java.util.Map.Entry[K, V]): Boolean = cacheSize < size()
}

case class TransactionStorageDataGenerator(inputTransactionsSizeRange: Range,
                                           outputTransactionsSizeRange: Range,
                                           transactionMultiplier: Int)
  extends StorageDataGenerator with TransactionFixture {

  private val generatedTransactions: LruCache[Int, JPair[ByteArrayWrapper, ByteArrayWrapper]] =
    new LruCache[Int, JPair[ByteArrayWrapper, ByteArrayWrapper]](10)

  require(inputTransactionsSizeRange.step == 1)
  require(outputTransactionsSizeRange.step == 1)

  private def generateTransactionEntry(): JPair[ByteArrayWrapper, ByteArrayWrapper] = {
    val transaction: RegularTransaction = generateRegularTransaction(inputTransactionsSizeRange, outputTransactionsSizeRange)
    val transactionData: ByteArrayWrapper =  byteArrayToWrapper(transaction.bytes())
    val transactionId: ByteArrayWrapper = idToBytes(transaction.id)
    new JPair(transactionId, transactionData)
  }

  private def generateDataStream(index: Int, counter: Int): Stream[JPair[ByteArrayWrapper, ByteArrayWrapper]] = {
    val (actualIndex, actualCounter) = if (counter == transactionMultiplier) {(index + 1, 0)} else {(index, counter)}

    val realTransaction = generatedTransactions.computeIfAbsent(actualIndex, _ => generateTransactionEntry())
    StorageDataGenerator.mutateStorageData(realTransaction, actualCounter) #:: generateDataStream(actualIndex, actualCounter + 1)
  }

  override def buildGeneratedDataStream(): Stream[JPair[ByteArrayWrapper, ByteArrayWrapper]] = {
    generateDataStream(0, 0)
  }

  override def copyGenerator(): StorageDataGenerator = this.copy()

  override def expectedAverageDataLen: Int = {
    val inputTransactionAverageCount = (inputTransactionsSizeRange.size / 2) + inputTransactionsSizeRange.start
    val inputTransactionAverageSize = inputTransactionAverageCount * (PrivateKey25519.KEY_LENGTH * 2)

    val outputTransactionAverageCount = (outputTransactionsSizeRange.size / 2) + outputTransactionsSizeRange.start
    val outputTransactionAverageSize = outputTransactionAverageCount * PublicKey25519Proposition.getLength

    inputTransactionAverageSize + outputTransactionAverageSize
  }
}

case class ByteDataGenerator(byteDataSizeRange: Range,
                             dataMultiplier: Int)
  extends StorageDataGenerator {

  require(byteDataSizeRange.step == 1)

  private val keyLen = 32
  private val generatedData: LruCache[Int, JPair[ByteArrayWrapper, ByteArrayWrapper]] =
    new LruCache[Int, JPair[ByteArrayWrapper, ByteArrayWrapper]](10)

  private def getRandomByteArrayWrapper(length: Int): ByteArrayWrapper = {
    val generatedData: Array[Byte] = new Array[Byte](length)
    util.Random.nextBytes(generatedData)
    generatedData
  }

  private def generateData(): JPair[ByteArrayWrapper, ByteArrayWrapper] = {
    val dataLength = Random.nextInt(byteDataSizeRange.size) + byteDataSizeRange.start
    new JPair(getRandomByteArrayWrapper(keyLen), getRandomByteArrayWrapper(dataLength))
  }

  private def generateDataStream(index: Int, counter: Int): Stream[JPair[ByteArrayWrapper, ByteArrayWrapper]] = {
    val (actualIndex, actualCounter) = if (counter == dataMultiplier) {
      (index + 1, 0)
    } else {
      (index, counter)
    }

    val generatedEntry = generatedData.computeIfAbsent(index, _ => generateData())
    StorageDataGenerator.mutateStorageData(generatedEntry, actualCounter) #:: generateDataStream(actualIndex, actualCounter + 1)
  }

  override def buildGeneratedDataStream(): Stream[JPair[ByteArrayWrapper, ByteArrayWrapper]] = generateDataStream(0, 0)

  override def copyGenerator(): StorageDataGenerator = this.copy()

  override def expectedAverageDataLen: Int = {
    (byteDataSizeRange.size / 2) + byteDataSizeRange.start
  }
}