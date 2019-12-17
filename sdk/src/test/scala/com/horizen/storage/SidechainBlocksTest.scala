package com.horizen.storage

import java.lang.{Byte => JByte}
import java.util.{ArrayList => JArrayList, HashMap => JHashMap, List => JList, Optional => JOptional}

import com.horizen.SidechainTypes
import com.horizen.block.SidechainBlock
import com.horizen.chain.SidechainBlockInfo
import com.horizen.companion.SidechainTransactionsCompanion
import com.horizen.fixtures.{SidechainBlockFixture, SidechainBlockInfoFixture}
import com.horizen.params.{MainNetParams, NetworkParams}
import com.horizen.transaction.TransactionSerializer
import com.horizen.utils._
import com.horizen.utils.Pair
import org.junit.Assert.{assertEquals, assertFalse, assertTrue}
import org.junit._
import org.mockito._
import org.scalatest.junit.JUnitSuite
import org.scalatest.mockito._
import scorex.core.consensus.ModifierSemanticValidity
import scorex.crypto.hash.Blake2b256
import scorex.util.{ModifierId, idToBytes}

import scala.collection.mutable.ListBuffer
import scala.util.Try

class SidechainBlocksTest extends JUnitSuite with MockitoSugar with SidechainBlockFixture with SidechainBlockInfoFixture {

  val mockedStorage : Storage = mock[IODBStoreAdapter]
  val customTransactionSerializers: JHashMap[JByte, TransactionSerializer[SidechainTypes#SCBT]] = new JHashMap()
  val sidechainTransactionsCompanion = SidechainTransactionsCompanion(customTransactionSerializers)
  var params: NetworkParams = _

  val height = 10
  val activeChainBlockList = new ListBuffer[SidechainBlock]()
  val activeChainBlockInfoList = new ListBuffer[SidechainBlockInfo]()
  val forkChainBlockList = new ListBuffer[SidechainBlock]()
  val forkChainBlockInfoList = new ListBuffer[SidechainBlockInfo]()
  val storedDataList = new ListBuffer[Pair[ByteArrayWrapper, ByteArrayWrapper]]()

  def generateStoredData(blockList: ListBuffer[(SidechainBlock, SidechainBlockInfo)]): ListBuffer[Pair[ByteArrayWrapper, ByteArrayWrapper]] = {
    val dataList = new ListBuffer[Pair[ByteArrayWrapper, ByteArrayWrapper]]()

    // calculate data emulating storage data for each data
    for(i <- blockList.indices) {
      // block info data
      dataList += new Pair(
        new ByteArrayWrapper(Blake2b256(s"blockInfo${blockList(i)._1.id}")),
        blockList(i)._2.bytes
      )

      // block data
      dataList += new Pair(
        new ByteArrayWrapper(idToBytes(blockList(i)._1.id)),
        new ByteArrayWrapper(blockList(i)._1.bytes)
      )
    }
    dataList
  }

  private def checkMainchainReferences(historyStorage: SidechainBlocks, blocks: Seq[SidechainBlock], blockIndex: Int): Unit = {
    val mainchainLength = blocks.take(blockIndex).map(b => b.mainchainBlocks.length).sum + params.mainchainCreationBlockHeight

    blocks(blockIndex).mainchainBlocks.zipWithIndex.foreach { case (mcBlock, mainchainReferenceIndex) =>
      assertEquals("Storage shall return correct sidechain block by mainchain block reference hash",
        blocks(blockIndex).id, historyStorage.getSidechainBlockByMainchainBlockReferenceHash(mcBlock.hash).get.id)

      assertTrue("Storage shall return correct mainchain block reference by mainchain block reference id",
        mcBlock.header.mainchainHeaderBytes.sameElements(historyStorage.getMainchainBlockReferenceByHash(mcBlock.hash).get.header.mainchainHeaderBytes))

      val mainchainHeight = mainchainLength + mainchainReferenceIndex
      val receivedInfoByHeight = historyStorage.getMainchainBlockReferenceInfoByMainchainBlockHeight(mainchainHeight).get
      val receivedInfoByHash = historyStorage.getMainchainBlockReferenceInfoByHash(mcBlock.hash)

      val allInfos = Seq(receivedInfoByHeight, receivedInfoByHash.get)

      allInfos.foreach{receivedInfo =>
        assertEquals(
          s"Storage shall return correct mainchain block reference id by mainchain block reference height for block ${mcBlock.hash.deep} for ${mainchainReferenceIndex}nth mainchain block",
          mcBlock.hash.deep, receivedInfo.getMainchainBlockReferenceHash.deep)

        assertEquals(
          s"Storage shall return correct mainchain block reference height by mainchain block reference height for block ${mcBlock.hash.deep} for ${mainchainReferenceIndex}nth mainchain block",
          mainchainHeight, receivedInfo.getMainchainHeight)

        assertEquals(
          s"Storage shall return correct sidechain block reference by mainchain block reference height for block ${mcBlock.hash.deep} for ${mainchainReferenceIndex}nth mainchain block",
          idToBytes(blocks(blockIndex).id).deep, receivedInfo.getSidechainBlockId.deep)

        assertEquals(
          s"Storage shall return correct mainchain block parent by mainchain block reference height for block ${mcBlock.hash.deep} for ${mainchainReferenceIndex}nth mainchain block",
          mcBlock.header.hashPrevBlock.deep, receivedInfo.getParentMainchainBlockReferenceHash.deep)
      }
    }
  }

  @Before
  def setUp() : Unit = {
    // add genesis block
    activeChainBlockList += generateGenesisBlock(sidechainTransactionsCompanion)
    activeChainBlockInfoList += generateGenesisBlockInfo(Some(activeChainBlockList.head.mainchainBlocks.head.hash))

    // declare real genesis block id
    class HistoryTestParams extends MainNetParams {
      override val sidechainGenesisBlockId: ModifierId = activeChainBlockList.head.id
      override val mainchainCreationBlockHeight: Int = 24
    }
    params = new HistoryTestParams()

    // generate (height-1) more blocks
    generateSidechainBlockSeq(height - 1, sidechainTransactionsCompanion, params).map(block => {
      activeChainBlockList += block
      activeChainBlockInfoList += generateBlockInfo(block, activeChainBlockInfoList.last, params)
    })
    // generate (height/2) fork blocks
    generateSidechainBlockSeq(height / 2, sidechainTransactionsCompanion, params, basicSeed = 13213111L).map(block => {
      forkChainBlockList += block
      forkChainBlockInfoList += generateBlockInfo(block, forkChainBlockInfoList.lastOption.getOrElse(activeChainBlockInfoList.head), params) // First Fork block parent is genesis block
    })


    storedDataList ++= generateStoredData(activeChainBlockList zip activeChainBlockInfoList)
    storedDataList ++= generateStoredData(forkChainBlockList zip forkChainBlockInfoList) // without genesis block

    // store best block data from active chain
    storedDataList += new Pair(
      new ByteArrayWrapper(Array.fill(32)(-1: Byte)),
      new ByteArrayWrapper(idToBytes(activeChainBlockList.last.id))
    )

    Mockito.when(mockedStorage.get(ArgumentMatchers.any[ByteArrayWrapper]()))
      .thenAnswer(answer => {
        storedDataList.find(_.getKey.equals(answer.getArgument(0))) match {
          case Some(pair) => JOptional.of(pair.getValue)
          case None => JOptional.empty()
        }
      })
  }

  @Test
  def testGet(): Unit = {
    val historyStorage = new SidechainBlocks(mockedStorage, sidechainTransactionsCompanion, params)

    // Test 0: check mainchain references
    (0 until height).foreach{ index => checkMainchainReferences(historyStorage, activeChainBlockList, index) }

    val index = activeChainBlockList.lastIndexWhere(b => b.mainchainBlocks.nonEmpty)
    val expectedBestMainchainReferenceOption = activeChainBlockList.lift(index).flatMap(_.mainchainBlocks.lastOption)
    expectedBestMainchainReferenceOption.foreach{expectedBestId =>
      assertEquals("Best mainchain reference shall be as expected", expectedBestId.hash.deep, historyStorage.getBestMainchainBlockReferenceInfo.get.getMainchainBlockReferenceHash.deep)
    }

    // Test 1: get height
    assertEquals("Storage contains wrong height", height, historyStorage.height)

    // Test 2: get heightOf
    // genesis block
    assertEquals("Storage returned wrong height", activeChainBlockInfoList.head.height, historyStorage.heightOf(activeChainBlockList.head.id).get)
    // active chain 5th block
    assertEquals("Storage returned wrong height", activeChainBlockInfoList(4).height, historyStorage.heightOf(activeChainBlockList(4).id).get)
    // active chain last block
    assertEquals("Storage returned wrong height", activeChainBlockInfoList.last.height, historyStorage.heightOf(activeChainBlockList.last.id).get)
    // fork chain last block
    assertEquals("Storage returned wrong height", forkChainBlockInfoList.last.height, historyStorage.heightOf(forkChainBlockList.last.id).get)
    // unknown block
    assertTrue("Storage expected not to find height for unknown block id", historyStorage.heightOf(getRandomModifier()).isEmpty)


    // Test 3: get bestBlockId
    assertEquals("Storage returned wrong best block id", activeChainBlockList.last.id, historyStorage.bestBlockId)


    // Test 4: get bestBlock and bestBlockInfo
    assertEquals("Storage returned wrong best block", activeChainBlockList.last.id, historyStorage.bestBlock.id)
    assertEquals("Storage returned wrong best block info", activeChainBlockInfoList.last, historyStorage.bestBlockInfo)


    // Test 5: get blockById
    // active chain genesis block id
    assertEquals("Storage returned wrong block", activeChainBlockList.head.id, historyStorage.blockById(activeChainBlockList.head.id).get.id)
    // active chain 5th block id
    assertEquals("Storage returned wrong block", activeChainBlockList(4).id, historyStorage.blockById(activeChainBlockList(4).id).get.id)
    // active chain last block id
    assertEquals("Storage returned wrong block", activeChainBlockList.last.id, historyStorage.blockById(activeChainBlockList.last.id).get.id)
    // fork chain last block id
    assertEquals("Storage returned wrong block", forkChainBlockList.last.id, historyStorage.blockById(forkChainBlockList.last.id).get.id)
    // unknown block id
    assertTrue("Storage expected not to find block for unknown id", historyStorage.blockById(getRandomModifier()).isEmpty)



    // Test 6: get blockInfoById
    // active chain genesis block id
    assertEquals("Storage returned wrong block", activeChainBlockInfoList.head, historyStorage.blockInfoById(activeChainBlockList.head.id).get)
    // active chain 5th block id
    assertEquals("Storage returned wrong block", activeChainBlockInfoList(4), historyStorage.blockInfoById(activeChainBlockList(4).id).get)
    // active chain last block id
    assertEquals("Storage returned wrong block", activeChainBlockInfoList.last, historyStorage.blockInfoById(activeChainBlockList.last.id).get)
    // fork chain last block id
    assertEquals("Storage returned wrong block", forkChainBlockInfoList.last, historyStorage.blockInfoById(forkChainBlockList.last.id).get)
    // unknown block id
    assertTrue("Storage expected not to find block for unknown id", historyStorage.blockInfoById(getRandomModifier()).isEmpty)


    // Test 7: get parentBlockId
    // active chain genesis block id
    assertEquals("Storage returned wrong parent id", activeChainBlockList.head.parentId, historyStorage.parentBlockId(activeChainBlockList.head.id).get)
    // active chain 5th block id
    assertEquals("Storage returned wrong parent id", activeChainBlockList(4).parentId, historyStorage.parentBlockId(activeChainBlockList(4).id).get)
    // active chain last block id
    assertEquals("Storage returned wrong parent id", activeChainBlockList.last.parentId, historyStorage.parentBlockId(activeChainBlockList.last.id).get)
    // fork chain last block id
    assertEquals("Storage returned wrong parent id", forkChainBlockList.last.parentId, historyStorage.parentBlockId(forkChainBlockList.last.id).get)
    // unknown block id
    assertTrue("Storage expected not to find parent id for unknown id", historyStorage.parentBlockId(getRandomModifier()).isEmpty)


    // Test 8: get chainScoreFor
    // active chain genesis block id
    assertEquals("Storage returned wrong chain score", activeChainBlockInfoList.head.score, historyStorage.chainScoreFor(activeChainBlockList.head.id).get)
    // active chain 5th block id
    assertEquals("Storage returned wrong chain score", activeChainBlockInfoList(4).score, historyStorage.chainScoreFor(activeChainBlockList(4).id).get)
    // active chain last block id
    assertEquals("Storage returned wrong chain score", activeChainBlockInfoList.last.score, historyStorage.chainScoreFor(activeChainBlockList.last.id).get)
    // fork chain last block id
    assertEquals("Storage returned wrong chain score", forkChainBlockInfoList.last.score, historyStorage.chainScoreFor(forkChainBlockList.last.id).get)
    // unknown block id
    assertTrue("Storage expected not to find chain score for unknown id", historyStorage.chainScoreFor(getRandomModifier()).isEmpty)


    // Test 9: check isInActiveChain
    // active chain genesis block id
    assertTrue("Block id expected to be in active chain", historyStorage.isInActiveChain(activeChainBlockList.head.id))
    // active chain 5th block id
    assertTrue("Block id expected to be in active chain", historyStorage.isInActiveChain(activeChainBlockList(4).id))
    // active chain last block id
    assertTrue("Block id expected to be in active chain", historyStorage.isInActiveChain(activeChainBlockList.last.id))
    // fork chain last block id
    assertFalse("Block id expected to be NOT in active chain", historyStorage.isInActiveChain(forkChainBlockList.last.id))
    // unknown block id
    assertFalse("Block id expected to be NOT in active chain", historyStorage.isInActiveChain(getRandomModifier()))


    // Test 10: get activeChainBlockId by height
    // active chain genesis block id height
    assertEquals("Storage returned wrong block id", activeChainBlockList.head.id, historyStorage.activeChainBlockId(1).get)
    // active chain 5th block id height
    assertEquals("Storage returned wrong block id", activeChainBlockList(4).id, historyStorage.activeChainBlockId(5).get)
    // active chain last block id height
    assertEquals("Storage returned wrong block id", activeChainBlockList.last.id, historyStorage.activeChainBlockId(height).get)
    // unknown height
    assertTrue("Storage expected not to find id", historyStorage.activeChainBlockId(height + 1).isEmpty)
    // 0 height
    assertTrue("Storage expected not to find id", historyStorage.activeChainBlockId(0).isEmpty)
    // negative height
    assertTrue("Storage expected not to find id", historyStorage.activeChainBlockId(-1).isEmpty)


    // Test 11: get activeChainFrom
    // active chain from genesis block
    assertEquals("Storage returned wrong active chain size", height, historyStorage.activeChainAfter(activeChainBlockList.head.id).size)
    // active chain from 5th block
    assertEquals("Storage returned wrong active chain size", height - 4, historyStorage.activeChainAfter(activeChainBlockList(4).id).size)
    // active chain last block id height
    assertEquals("Storage returned wrong active chain size", 1, historyStorage.activeChainAfter(activeChainBlockList.last.id).size)
    // active chain from fork last block id
    assertTrue("Storage active chain from forked block should be empty", historyStorage.activeChainAfter(forkChainBlockList.last.id).isEmpty)


    // Test 8: get semanticValidity
    // active chain genesis block id
    assertEquals("Storage returned wrong semanticValidity", ModifierSemanticValidity.Unknown, historyStorage.semanticValidity(activeChainBlockList.head.id))
    // active chain last block id
    assertEquals("Storage returned wrong semanticValidity", ModifierSemanticValidity.Unknown, historyStorage.semanticValidity(activeChainBlockList.last.id))
    // fork chain last block id
    assertEquals("Storage returned wrong semanticValidity", ModifierSemanticValidity.Unknown, historyStorage.semanticValidity(forkChainBlockList.last.id))
    // unknown block id
    assertEquals("Storage expected not to find chain score for unknown id", ModifierSemanticValidity.Absent, historyStorage.semanticValidity(getRandomModifier()))
  }

  @Test
  def testUpdates(): Unit = {
    val historyStorage = new SidechainBlocks(mockedStorage, sidechainTransactionsCompanion, params)
    var tryRes: Try[SidechainBlocks] = null
    val expectedException = new IllegalArgumentException("on update exception")

    val nextTipBlock = generateNextSidechainBlock(activeChainBlockList.last, sidechainTransactionsCompanion, params, basicSeed = 11119992L)
    val nextTipBlockInfo = generateBlockInfo(nextTipBlock, activeChainBlockInfoList.last, params)
    val nextTipToUpdate: JList[Pair[ByteArrayWrapper, ByteArrayWrapper]] = new JArrayList[Pair[ByteArrayWrapper, ByteArrayWrapper]]()
    generateStoredData(ListBuffer(nextTipBlock -> nextTipBlockInfo)).map(p => nextTipToUpdate.add(p))

    val nextForkTipBlock = generateNextSidechainBlock(forkChainBlockList.last, sidechainTransactionsCompanion, params, basicSeed = 114422L)
    val nextForkTipBlockInfo = generateBlockInfo(nextForkTipBlock, forkChainBlockInfoList.last, params, Some(forkChainBlockInfoList.last.score + 2))
    val forkTipToUpdate: JList[Pair[ByteArrayWrapper, ByteArrayWrapper]] = new JArrayList[Pair[ByteArrayWrapper, ByteArrayWrapper]]()
    generateStoredData(ListBuffer(nextForkTipBlock -> nextForkTipBlockInfo)).map(p => forkTipToUpdate.add(p))

    Mockito.when(mockedStorage.update(
      ArgumentMatchers.any[ByteArrayWrapper](),
      ArgumentMatchers.anyList[Pair[ByteArrayWrapper, ByteArrayWrapper]](),
      ArgumentMatchers.anyList[ByteArrayWrapper]()))
      // For Test 1: update active chain with new block
      .thenAnswer(answer => {
      val actualToUpdate = answer.getArgument(1).asInstanceOf[java.util.List[Pair[ByteArrayWrapper, ByteArrayWrapper]]]
      val actualToRemove = answer.getArgument(2).asInstanceOf[java.util.List[ByteArrayWrapper]]
      assertEquals("HistoryStorage.update(...) actual toUpdate list is wrong.", nextTipToUpdate, actualToUpdate)
      assertTrue("HistoryStorage.update(...) actual toRemove list should be empty.", actualToRemove.isEmpty)
    })
      // For Test 2: update with some fork block
      .thenAnswer(answer => {
      val actualToUpdate = answer.getArgument(1).asInstanceOf[java.util.List[Pair[ByteArrayWrapper, ByteArrayWrapper]]]
      val actualToRemove = answer.getArgument(2).asInstanceOf[java.util.List[ByteArrayWrapper]]
      assertEquals("HistoryStorage.update(...) actual toUpdate list is wrong.", forkTipToUpdate, actualToUpdate)
      assertTrue("HistoryStorage.update(...) actual toRemove list should be empty.", actualToRemove.isEmpty)
    })
      // For Test 3: exception processing
      .thenAnswer(answer => throw expectedException)


    // Test 1: try to add valid block next to current tip
    tryRes = historyStorage.update(nextTipBlock, nextTipBlockInfo)
    assertTrue("HistoryStorage successful update expected, instead exception occurred:\n %s".format(if(tryRes.isFailure) tryRes.failed.get.getMessage else ""),
      tryRes.isSuccess)


    // Test 2: try to add valid block from forked chain
    tryRes = historyStorage.update(nextForkTipBlock, nextForkTipBlockInfo)
    assertTrue("HistoryStorage successful update expected, instead exception occurred:\n %s".format(if(tryRes.isFailure) tryRes.failed.get.getMessage else ""),
      tryRes.isSuccess)

    // Test 3: test failed update, when Storage throws an exception
    val nextFailureBlock = generateNextSidechainBlock(activeChainBlockList.last, sidechainTransactionsCompanion, params, basicSeed = 1315692L)
    val nextFailureBlockInfo = generateBlockInfo(nextFailureBlock, activeChainBlockInfoList.last, params)
    tryRes = historyStorage.update(nextFailureBlock, nextFailureBlockInfo)
    assertTrue("HistoryStorage failure expected during update.", tryRes.isFailure)
    assertEquals("HistoryStorage different exception expected during update.", expectedException, tryRes.failed.get)
  }

  @Test
  def testUpdateSemanticValidity(): Unit = {
    val historyStorage = new SidechainBlocks(mockedStorage, sidechainTransactionsCompanion, params)
    var tryRes: Try[SidechainBlocks] = null
    val expectedException = new IllegalArgumentException("on update semantic validity exception")


    val tipNewValidity = ModifierSemanticValidity.Valid
    val tipValidityToUpdate: JList[Pair[ByteArrayWrapper, ByteArrayWrapper]] = new JArrayList[Pair[ByteArrayWrapper, ByteArrayWrapper]]()
    tipValidityToUpdate.add(new Pair(
      new ByteArrayWrapper(Blake2b256(s"blockInfo${activeChainBlockList.last.id}")),
      new ByteArrayWrapper(
        SidechainBlockInfo(
          activeChainBlockInfoList.last.height,
          activeChainBlockInfoList.last.score,
          activeChainBlockList.last.parentId,
          tipNewValidity,
          activeChainBlockInfoList.last.mainchainBlockReferenceHashes,
          activeChainBlockInfoList.last.withdrawalEpochInfo
        ).bytes)
    ))

    val forkTipNewValidity = ModifierSemanticValidity.Invalid
    val forkTipValidityToUpdate: JList[Pair[ByteArrayWrapper, ByteArrayWrapper]] = new JArrayList[Pair[ByteArrayWrapper, ByteArrayWrapper]]()
    forkTipValidityToUpdate.add(new Pair(
      new ByteArrayWrapper(Blake2b256(s"blockInfo${forkChainBlockList.last.id}")),
      new ByteArrayWrapper(
        SidechainBlockInfo(
          forkChainBlockInfoList.last.height,
          forkChainBlockInfoList.last.score,
          forkChainBlockList.last.parentId,
          forkTipNewValidity,
          forkChainBlockInfoList.last.mainchainBlockReferenceHashes,
          forkChainBlockInfoList.last.withdrawalEpochInfo
        ).bytes)
    ))

    Mockito.when(mockedStorage.update(
      ArgumentMatchers.any[ByteArrayWrapper](),
      ArgumentMatchers.anyList[Pair[ByteArrayWrapper, ByteArrayWrapper]](),
      ArgumentMatchers.anyList[ByteArrayWrapper]()))
      // For Test 1: update active chain with new block
      .thenAnswer(answer => {
      val actualToUpdate = answer.getArgument(1).asInstanceOf[java.util.List[Pair[ByteArrayWrapper, ByteArrayWrapper]]]
      val actualToRemove = answer.getArgument(2).asInstanceOf[java.util.List[ByteArrayWrapper]]
      assertEquals("HistoryStorage.update(...) actual toUpdate list is wrong.", tipValidityToUpdate, actualToUpdate)
      assertTrue("HistoryStorage.update(...) actual toRemove list should be empty.", actualToRemove.isEmpty)
    })
      // For Test 2: update with some fork block
      .thenAnswer(answer => {
      val actualToUpdate = answer.getArgument(1).asInstanceOf[java.util.List[Pair[ByteArrayWrapper, ByteArrayWrapper]]]
      val actualToRemove = answer.getArgument(2).asInstanceOf[java.util.List[ByteArrayWrapper]]
      assertEquals("HistoryStorage.update(...) actual toUpdate list is wrong.", forkTipValidityToUpdate, actualToUpdate)
      assertTrue("HistoryStorage.update(...) actual toRemove list should be empty.", actualToRemove.isEmpty)
    })
      // For Test 3: exception processing
      .thenAnswer(answer => throw expectedException)


    // Test 1: try to update semantic validity of current tip
    tryRes = historyStorage.updateSemanticValidity(activeChainBlockList.last, tipNewValidity)
    assertTrue("HistoryStorage successful semantic validity update expected, instead exception occurred:\n %s".format(if(tryRes.isFailure) tryRes.failed.get.getMessage else ""),
      tryRes.isSuccess)


    // Test 2:  try to update semantic validity of fork block
    tryRes = historyStorage.updateSemanticValidity(forkChainBlockList.last, forkTipNewValidity)
    assertTrue("HistoryStorage successful semantic validity update expected, instead exception occurred:\n %s".format(if(tryRes.isFailure) tryRes.failed.get.getMessage else ""),
      tryRes.isSuccess)


    // Test 3: test failed update, when Storage throws an exception
    tryRes = historyStorage.updateSemanticValidity(forkChainBlockList.last, forkTipNewValidity)
    assertTrue("HistoryStorage failure expected during update.", tryRes.isFailure)
    assertEquals("HistoryStorage different exception expected during update.", expectedException, tryRes.failed.get)
  }

  @Test
  def testUpdateBestBlock(): Unit = {
    val historyStorage = new SidechainBlocks(mockedStorage, sidechainTransactionsCompanion, params)
    var tryRes: Try[SidechainBlocks] = null
    val expectedException = new IllegalArgumentException("on update best block exception")

    val newBestBlock = forkChainBlockList.head
    val newBestBlockInfo = SidechainBlockInfo(2, 2, newBestBlock.parentId, ModifierSemanticValidity.Valid, SidechainBlockInfo.mainchainReferencesFromBlock(newBestBlock), WithdrawalEpochInfo(1, 2))
    val newBestBlockToUpdate: JList[Pair[ByteArrayWrapper, ByteArrayWrapper]] = new JArrayList[Pair[ByteArrayWrapper, ByteArrayWrapper]]()
    newBestBlockToUpdate.add(new Pair(
      new ByteArrayWrapper(Array.fill(32)(-1: Byte)),
      new ByteArrayWrapper(idToBytes(newBestBlock.id))
    ))

    Mockito.when(mockedStorage.update(
      ArgumentMatchers.any[ByteArrayWrapper](),
      ArgumentMatchers.anyList[Pair[ByteArrayWrapper, ByteArrayWrapper]](),
      ArgumentMatchers.anyList[ByteArrayWrapper]()))
      // For Test 1: update active chain with new block
      .thenAnswer(answer => {
      val actualToUpdate = answer.getArgument(1).asInstanceOf[java.util.List[Pair[ByteArrayWrapper, ByteArrayWrapper]]]
      val actualToRemove = answer.getArgument(2).asInstanceOf[java.util.List[ByteArrayWrapper]]
      assertEquals("HistoryStorage.update(...) actual toUpdate list is wrong.", newBestBlockToUpdate, actualToUpdate)
      assertTrue("HistoryStorage.update(...) actual toRemove list should be empty.", actualToRemove.isEmpty)
    })
      // For Test 2: exception processing
      .thenAnswer(answer => throw expectedException)


    // Test 1: try to update best block with a forked block
    tryRes = historyStorage.setAsBestBlock(newBestBlock, newBestBlockInfo)
    assertTrue("HistoryStorage successful best block update expected, instead exception occurred:\n %s".format(if(tryRes.isFailure) tryRes.failed.get.getMessage else ""),
      tryRes.isSuccess)


    // Test 2: test failed update, when Storage throws an exception
    tryRes = historyStorage.setAsBestBlock(newBestBlock, newBestBlockInfo) // Why it shall be failed? @TODO check it
    assertTrue("HistoryStorage failure expected during update.", tryRes.isFailure)
    assertEquals("HistoryStorage different exception expected during update.", expectedException, tryRes.failed.get)
  }

  @Test
  def testExceptions() : Unit = {
    var exceptionThrown = false

    try {
      val stateStorage = new SidechainBlocks(null, sidechainTransactionsCompanion, params)
    } catch {
      case e : IllegalArgumentException => exceptionThrown = true
      case e : Throwable => System.out.print(e.getMessage)
    }

    assertTrue("HistoryStorage constructor. Exception must be thrown if storage is not specified.",
      exceptionThrown)

    exceptionThrown = false
    try {
      val stateStorage = new SidechainBlocks(mockedStorage, null, params)
    } catch {
      case e : IllegalArgumentException => exceptionThrown = true
    }

    assertTrue("HistoryStorage constructor. Exception must be thrown if transactions companion is not specified.",
      exceptionThrown)

    exceptionThrown = false
    try {
      val stateStorage = new SidechainBlocks(mockedStorage, sidechainTransactionsCompanion, null)
    } catch {
      case e : IllegalArgumentException => exceptionThrown = true
    }

    assertTrue("HistoryStorage constructor. Exception must be thrown if params object is not specified.",
      exceptionThrown)
  }
}
