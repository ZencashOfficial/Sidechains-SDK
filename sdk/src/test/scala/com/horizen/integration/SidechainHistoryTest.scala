package com.horizen.integration

import java.lang.{Byte => JByte}
import java.util.{HashMap => JHashMap}

import com.horizen.block.SidechainBlock
import com.horizen.chain.SidechainBlockInfo
import com.horizen.companion.SidechainTransactionsCompanion
import com.horizen.customtypes.SemanticallyInvalidTransactionSerializer
import com.horizen.fixtures.{IODBStoreFixture, SidechainBlockFixture, SidechainBlockInfoFixture, TransactionFixture}
import com.horizen.params.{MainNetParams, NetworkParams}
import com.horizen.storage.{IODBStoreAdapter, SidechainBlocks, Storage, TransactionIndexes}
import com.horizen.transaction.TransactionSerializer
import com.horizen.utils.WithdrawalEpochInfo
import com.horizen.validation.SidechainBlockSemanticValidator
import com.horizen.{SidechainHistory, SidechainSettings, SidechainSyncInfo, SidechainTypes}
import org.junit.Assert.{assertEquals, assertFalse, assertTrue}
import org.junit.{Before, Test}
import org.mockito.Mockito
import org.scalatest.junit.JUnitSuite
import org.scalatest.mockito.MockitoSugar
import scorex.core.consensus.History.ProgressInfo
import scorex.core.consensus.{History, ModifierSemanticValidity}
import scorex.core.settings.ScorexSettings

import scala.util.{Failure, Success}

class SidechainHistoryTest extends JUnitSuite with MockitoSugar
    with SidechainBlockFixture with SidechainBlockInfoFixture
    with IODBStoreFixture with scorex.core.utils.ScorexEncoding
    with TransactionFixture {

  var customTransactionSerializers: JHashMap[JByte, TransactionSerializer[SidechainTypes#SCBT]] = new JHashMap()
  customTransactionSerializers.put(11.toByte, SemanticallyInvalidTransactionSerializer.getSerializer.asInstanceOf[TransactionSerializer[SidechainTypes#SCBT]])
  val sidechainTransactionsCompanion = SidechainTransactionsCompanion(customTransactionSerializers)

  val genesisBlock: SidechainBlock = generateGenesisBlock(sidechainTransactionsCompanion)
  val genesisBlockInfo: SidechainBlockInfo = generateGenesisBlockInfo(Some(genesisBlock.mainchainBlocks.head.hash), ModifierSemanticValidity.Valid)
  var params: NetworkParams = _

  val sidechainSettings = mock[SidechainSettings]
  val scorexSettings = mock[ScorexSettings]
  var storage: Storage = _


  @Before
  def setUp(): Unit = {
    // declare real genesis block id
    params = MainNetParams(new Array[Byte](32), genesisBlock.id)

    Mockito.when(sidechainSettings.scorexSettings)
      .thenAnswer(answer => {
        scorexSettings
      })
    Mockito.when(scorexSettings.dataDir) // NOTE: each call returns different dir path
      .thenAnswer(answer => {
        tempDir()
      })
  }


  @Test
  def genesisTest(): Unit = {
    val sidechainHistoryStorage = new SidechainBlocks(new IODBStoreAdapter(getStore()),
      sidechainTransactionsCompanion, params)
    val transactionIndexesStorage = new TransactionIndexes(new IODBStoreAdapter(getStore()))

    val historyTry = SidechainHistory.genesisHistory(
      sidechainHistoryStorage,
      transactionIndexesStorage,
      params,
      genesisBlock,
      Seq(new SidechainBlockSemanticValidator(params)),
      Seq())
    assertTrue("Genesis history creation expected to be successful. ", historyTry.isSuccess)

    val history = historyTry.get
    assertFalse("Expected to be not empty.", history.isEmpty)
    assertEquals("Expected to have a genesis block height.", 1 , history.height)
    assertEquals("Expected to have a genesis block.", genesisBlock.id , history.bestBlockId)
    assertEquals("Expected to have a genesis block info.", genesisBlockInfo , history.bestBlockInfo)
    assertTrue("Expected to contain the genesis block.", history.contains(genesisBlock.id))
    assertTrue("Check for genesis block was failed.", history.isGenesisBlock(genesisBlock.id))
  }

  @Test
  def appendTest(): Unit = {
    val sidechainHistoryStorage = new SidechainBlocks(new IODBStoreAdapter(getStore()),
      sidechainTransactionsCompanion, params)
    val transactionIndexesStorage = new TransactionIndexes(new IODBStoreAdapter(getStore()))

    val historyTry = SidechainHistory.genesisHistory(
      sidechainHistoryStorage,
      transactionIndexesStorage,
      params,
      genesisBlock,
      Seq(new SidechainBlockSemanticValidator(params)),
      Seq())
    assertTrue("Genesis history creation expected to be successful. ", historyTry.isSuccess)

    var history: SidechainHistory = historyTry.get
    var progressInfo: ProgressInfo[SidechainBlock] = null

    // Test 1: append block after genesis
    val transactionsForB2 = generateRegularTransactionsList(2, 111)
    val blockB2: SidechainBlock = generateNextSidechainBlock(
      genesisBlock,
      sidechainTransactionsCompanion,
      params,
      basicSeed = 111L,
      transactionsForB2)
    history.append(blockB2) match {
      case Success((hist, prog)) =>
        history = hist
        progressInfo = prog
      case Failure(e) => assertFalse("Unexpected Exception occurred during block appending: %s".format(e.getMessage), true)
    }

    // check
    assertEquals("Expected to have NOT updated height, best block was NOT changed.", 1 , history.height)
    assertEquals("Expected to have a genesis block, best block was NOT changed.", genesisBlock.id , history.bestBlockId)
    assertTrue("Block expected to be present.", history.contains(blockB2.id))
    assertEquals("Different progress info expected.", ProgressInfo[SidechainBlock](None, Seq(), Seq(blockB2), Seq()), progressInfo)

    // notify history that appended block is valid
    history = history.reportModifierIsValid(blockB2)

    // check
    assertEquals("Expected to have updated height, best block was changed.", 2 , history.height)
    assertEquals("Expected to have different best block, best block was changed.", blockB2.id , history.bestBlockId)
    assertEquals("Expected to have different best block info, best block was changed.",
      SidechainBlockInfo(2, (1L << 32) + 2, blockB2.parentId, ModifierSemanticValidity.Valid, Seq(), WithdrawalEpochInfo(1, 1)), history.bestBlockInfo)
    assertTrue(history.getBlockById(blockB2.id).isPresent)

    transactionsForB2.foreach{ transaction =>
      val transactionInBlockChain = history.searchTransactionInsideBlockchain(transaction.id).get
      assertEquals("transactions shall be found in blockchain", transaction.id, transactionInBlockChain.id)
    }

    assertEquals("transactions shall be found in block",
      transactionsForB2.head.id, history.searchTransactionInsideSidechainBlock(transactionsForB2.head.id, blockB2.id).get().id)


    // Test 2: append block after current tip (not after genesis)
    val transactionsForB3 = generateRegularTransactionsList(3, 112)
    val blockB3: SidechainBlock = generateNextSidechainBlock(
      blockB2,
      sidechainTransactionsCompanion,
      params,
      basicSeed = 112L,
      transactionsForB3)
    history.append(blockB3) match {
      case Success((hist, prog)) =>
        history = hist
        progressInfo = prog
      case Failure(e) => assertFalse("Unexpected Exception occurred during block appending: %s".format(e.getMessage), true)
    }

    // check
    assertEquals("Expected to have NOT updated height, best block was NOT changed.", 2 , history.height)
    assertTrue("Block expected to be present.", history.contains(blockB3.id))
    assertEquals("Different progress info expected.", ProgressInfo[SidechainBlock](None, Seq(), Seq(blockB3), Seq()), progressInfo)

    // notify history that appended block is valid
    history = history.reportModifierIsValid(blockB3)

    // check
    assertEquals("Expected to have updated height, best block was changed.", 3 , history.height)
    assertEquals("Expected to have different best block, best block was changed.", blockB3.id , history.bestBlockId)
    assertEquals("Expected to have different best block info, best block was changed.",
      SidechainBlockInfo(3, (1L << 32) + 3, blockB3.parentId, ModifierSemanticValidity.Valid, Seq(), WithdrawalEpochInfo(1, 1)), history.bestBlockInfo)

    transactionsForB3.foreach{ transaction =>
      val transactionInBlockChain = history.searchTransactionInsideBlockchain(transaction.id).get
      assertEquals("transactions shall be found in blockchain", transaction.id, transactionInBlockChain.id)
    }

    // At the moment we have an active chain G1 -> B2 -> B3,
    // where G1 - genesis with height 1,
    // B<N> - active chain block with height N,
    // b<N> - non active chain block with height N


    // Test 3: try to append block that parent is absent
    val unknownBlock = generateGenesisBlock(sidechainTransactionsCompanion, basicSeed = 444L)
    history.append(unknownBlock) match {
      case Success((_, _)) =>
        assertFalse("Exception expected during appending a block without parent present in History.", true)
      case Failure(e) =>
        e match {
          case _: IllegalArgumentException =>
          case _ => assertFalse("IllegalArgumentException expected during appending a block without parent present in History: %s".format(e.getMessage), true)
        }
    }

    assertFalse("Block expected to be NOT present.", history.contains(unknownBlock.id))


    // Test 4: try to append semantically invalid block
    val invalidBlock = createSemanticallyInvalidClone(generateNextSidechainBlock(blockB3, sidechainTransactionsCompanion, params, basicSeed = 1231L), sidechainTransactionsCompanion)
    history.append(invalidBlock) match {
      case Success(_) =>
        assertTrue("Exception expected during block appending", false)
      case Failure(_) => // expected behaviour
    }

    assertFalse("Block expected to be NOT present.", history.contains(invalidBlock.id))


    // Test 5: try to add fork block b2
    val transactionsForB2Fork = generateRegularTransactionsList(4, 4441)
    val forkBlockB2: SidechainBlock = generateNextSidechainBlock(
      genesisBlock,
      sidechainTransactionsCompanion,
      params,
      basicSeed = 4441L,
      transactionsForB2Fork)

    history.append(forkBlockB2) match {
      case Success((hist, prog)) =>
        history = hist
        progressInfo = prog
      case Failure(e) => assertFalse("Unexpected Exception occurred during block appending: %s".format(e.getMessage), true)
    }

    // check
    assertEquals("Expected to have NOT updated height, best block was NOT changed.", 3, history.height)
    assertTrue("Block expected to be present.", history.contains(forkBlockB2.id))
    assertEquals("Different progress info expected.", ProgressInfo[SidechainBlock](None, Seq(), Seq(), Seq()), progressInfo)

    transactionsForB2Fork.foreach{ transaction =>
      val transactionInBlockChain = history.searchTransactionInsideBlockchain(transaction.id)
      assertTrue("transactions shall be NOT found in blockchain", transactionInBlockChain.isEmpty)
    }

    // Test 6: try to add block b3
    // score of b3 chain expected to be the same as B3 chain. So no chain switch expected.
    val forkBlockb3: SidechainBlock = generateNextSidechainBlock(forkBlockB2, sidechainTransactionsCompanion, params, basicSeed = 199L)
    history.append(forkBlockb3) match {
      case Success((hist, prog)) =>
        history = hist
        progressInfo = prog
      case Failure(e) => assertFalse("Unexpected Exception occurred during block appending: %s".format(e.getMessage), true)
    }

    // check
    assertEquals("Expected to have NOT updated height, best block was NOT changed.", 3, history.height)
    assertTrue("Block expected to be present.", history.contains(forkBlockb3.id))
    assertEquals("Different progress info expected.", ProgressInfo[SidechainBlock](None, Seq(), Seq(), Seq()), progressInfo)


    /* At the moment we have a blockchain:
           G1
          / \
         B2   b2
        /     \
       B3       b3
    */


    // Test 7: try to add block b4. Chain switch to b4 fork expected.
    val forkBlockb4: SidechainBlock = generateNextSidechainBlock(forkBlockb3, sidechainTransactionsCompanion, params, basicSeed = 11192L)
    history.append(forkBlockb4) match {
      case Success((hist, prog)) =>
        history = hist
        progressInfo = prog
      case Failure(e) => assertFalse("Unexpected Exception occurred during block appending: %s".format(e.getMessage), true)
    }

    // check
    assertEquals("Expected to have NOT updated height, best block was NOT changed.", 3, history.height)
    assertTrue("Block expected to be present.", history.contains(forkBlockb4.id))
    assertTrue("Progress info chain switch expected.", progressInfo.branchPoint.isDefined)
    assertEquals("Different progress info branch point expected.", genesisBlock.id, progressInfo.branchPoint.get)
    assertEquals("Different progress info remove data expected", Seq(blockB2, blockB3), progressInfo.toRemove)
    assertEquals("Different progress info apply data expected", Seq(forkBlockB2, forkBlockb3, forkBlockb4), progressInfo.toApply)
    assertTrue("Different progress info download data expected to be empty", progressInfo.toDownload.isEmpty)


    // notify history that appended block is valid
    history = history.reportModifierIsValid(forkBlockB2)
    history = history.reportModifierIsValid(forkBlockb3)
    history = history.reportModifierIsValid(forkBlockb4)

    // check
    assertEquals("Expected to have updated height, best block was changed.", 4, history.height)
    assertEquals("Expected to have different best block, best block was changed.", forkBlockb4.id , history.bestBlockId)

    transactionsForB2Fork.foreach{ transaction =>
      val transactionInBlockChain = history.searchTransactionInsideBlockchain(transaction.id).get
      assertEquals("transactions shall be found in blockchain", transaction.id, transactionInBlockChain.id)
    }

    // Test 8: try to add block B4, that contains invalid transactions
    val blockB4: SidechainBlock = generateNextSidechainBlock(blockB3, sidechainTransactionsCompanion, params, basicSeed = 888L)
    history.append(blockB4) match {
      case Success((hist, prog)) =>
        history = hist
        progressInfo = prog
      case Failure(e) => assertFalse("Unexpected Exception occurred during block appending: %s".format(e.getMessage), true)
    }

    // check
    assertEquals("Expected to have NOT updated height, best block was NOT changed.", 4, history.height)
    assertTrue("Block expected to be present.", history.contains(blockB4.id))
    assertEquals("Block expected to have undefined semantic validity.", ModifierSemanticValidity.Unknown, history.isSemanticallyValid(blockB4.id))
    assertEquals("Different progress info expected.", ProgressInfo[SidechainBlock](None, Seq(), Seq(), Seq()), progressInfo)


    // notify history that appended block is invalid
    history.reportModifierIsInvalid(blockB4, progressInfo) match {
      case (hist, prog) =>
        history = hist
        progressInfo = prog
    }

    // check
    assertEquals("Expected to have updated height, best block was changed.", 4, history.height)
    assertEquals("Expected to have different best block, best block was changed.", forkBlockb4.id , history.bestBlockId)
    assertEquals("Block expected to have undefined semantic validity.", ModifierSemanticValidity.Invalid, history.isSemanticallyValid(blockB4.id))


    /* At the moment we have a blockchain:
           G1
          / \
         B2   b2
        /     \
       B3       b3
      /          \
     B4(invalid)  b4 (active chain tip)
    */


    // Test 9: try to add block B5, to switch back to from "b"-chains to "B"-chain
    // Because of B4 is invalid, no switch expected.
    val blockB5: SidechainBlock = generateNextSidechainBlockWithInvalidTransaction(blockB4, sidechainTransactionsCompanion, params)
    history.append(blockB5) match {
      case Success((hist, prog)) =>
        history = hist
        progressInfo = prog
      case Failure(e) => assertFalse("Unexpected Exception occurred during block appending: %s".format(e.getMessage), true)
    }

    // check
    assertEquals("Expected to have NOT updated height, best block was NOT changed.", 4, history.height)
    assertTrue("Block expected to be present.", history.contains(blockB5.id))
    assertEquals("Block expected to have undefined semantic validity.", ModifierSemanticValidity.Unknown, history.isSemanticallyValid(blockB5.id))
    assertEquals("Different progress info expected.", ProgressInfo[SidechainBlock](None, Seq(), Seq(), Seq()), progressInfo)
  }

  @Test
  def bestForkChangesTest(): Unit = {
    val sidechainHistoryStorage = new SidechainBlocks(new IODBStoreAdapter(getStore()),
      sidechainTransactionsCompanion, params)
    val transactionIndexesStorage = new TransactionIndexes(new IODBStoreAdapter(getStore()))

    val historyTry = SidechainHistory.genesisHistory(
      sidechainHistoryStorage,
      transactionIndexesStorage,
      params,
      genesisBlock,
      Seq(new SidechainBlockSemanticValidator(params)),
      Seq())
    assertTrue("Genesis history creation expected to be successful. ", historyTry.isSuccess)

    var history = historyTry.get
    var blockSeq = Seq[SidechainBlock](genesisBlock)
    var blocksToAppend = 9

    while(blocksToAppend > 0) {
      val block = generateNextSidechainBlock(blockSeq.last, sidechainTransactionsCompanion, params, basicSeed = 86766L)

      history.append(block) match {
        case Success((hist, _)) =>
          history = hist
        case Failure(e) => assertFalse("Unexpected Exception occurred during block appending: %s".format(e.getMessage), true)
      }
      // notify history that appended block is valid
      history = history.reportModifierIsValid(block)

      blockSeq = blockSeq :+ block
      blocksToAppend -= 1
    }

    assertEquals("Expected to have different height", 10, history.height)


    // Test 1: fork changes for block after genesis
    val blockH2 = generateNextSidechainBlock(blockSeq.head, sidechainTransactionsCompanion, params, basicSeed = 9000L)
    history.bestForkChanges(blockH2) match {
      case Success(progressInfo) =>
        assertEquals("Different progress info branch point expected.", blockSeq.head.id, progressInfo.branchPoint.get)
        assertEquals("Different progress info remove data expected", blockSeq.tail, progressInfo.toRemove)
        assertEquals("Different progress info apply data expected", Seq(blockH2), progressInfo.toApply)
        assertTrue("Different progress info download data expected to be empty", progressInfo.toDownload.isEmpty)
      case Failure(e) => assertFalse("Unexpected Exception occurred during bestForkChanges calculation: %s".format(e.getMessage), true)
    }


    // Test 2: fork changes for block in the middle
    val blockH5 = generateNextSidechainBlock(blockSeq(4), sidechainTransactionsCompanion, params, basicSeed = 65522L)
    history.bestForkChanges(blockH5) match {
      case Success(progressInfo) =>
        assertEquals("Different progress info branch point expected.", blockSeq(4).id, progressInfo.branchPoint.get)
        assertEquals("Different progress info remove data expected", blockSeq.takeRight(5), progressInfo.toRemove)
        assertEquals("Different progress info apply data expected", Seq(blockH5), progressInfo.toApply)
        assertTrue("Different progress info download data expected to be empty", progressInfo.toDownload.isEmpty)
      case Failure(e) => assertFalse("Unexpected Exception occurred during bestForkChanges calculation: %s".format(e.getMessage), true)
    }


    // Test 3: fork changes for block before last
    val blockH9 = generateNextSidechainBlock(blockSeq(8), sidechainTransactionsCompanion, params, basicSeed = 1231234L)
    history.bestForkChanges(blockH9) match {
      case Success(progressInfo) =>
        assertEquals("Different progress info branch point expected.", blockSeq(8).id, progressInfo.branchPoint.get)
        assertEquals("Different progress info remove data expected", Seq(blockSeq.last), progressInfo.toRemove)
        assertEquals("Different progress info apply data expected", Seq(blockH9), progressInfo.toApply)
        assertTrue("Different progress info download data expected to be empty", progressInfo.toDownload.isEmpty)
      case Failure(e) => assertFalse("Unexpected Exception occurred during bestForkChanges calculation: %s".format(e.getMessage), true)
    }


    // Test 4: fork changes for block after last one
    // wrong situation
    val blockH11 = generateNextSidechainBlock(blockSeq.last, sidechainTransactionsCompanion, params, basicSeed = 5545454L)
    history.bestForkChanges(blockH11) match {
      case Success(progressInfo) =>
        assertTrue("Exception expected during bestForkChanges calculation", false)
      case Failure(_) =>
    }


    // Test 5: fork changes for block, which parent doesn't exist
    // wrong situation
    val unknownBlock = generateGenesisBlock(sidechainTransactionsCompanion, basicSeed = 444L)
    history.bestForkChanges(unknownBlock) match {
      case Success(progressInfo) =>
        assertTrue("Exception expected during bestForkChanges calculation", false)
      case Failure(_) =>
    }
  }

  @Test
  def applicableTryTest(): Unit = {
    val sidechainHistoryStorage = new SidechainBlocks(new IODBStoreAdapter(getStore()),
      sidechainTransactionsCompanion, params)
    val transactionIndexesStorage = new TransactionIndexes(new IODBStoreAdapter(getStore()))

    val historyTry = SidechainHistory.genesisHistory(
      sidechainHistoryStorage,
      transactionIndexesStorage,
      params,
      genesisBlock,
      Seq(new SidechainBlockSemanticValidator(params)),
      Seq())
    assertTrue("Genesis history creation expected to be successful. ", historyTry.isSuccess)

    var history: SidechainHistory = historyTry.get

    val applicableBlock: SidechainBlock = generateNextSidechainBlock(genesisBlock, sidechainTransactionsCompanion, params, basicSeed = 5656521L)
    history.applicableTry(applicableBlock) match {
      case Success(_) =>
      case Failure(_) =>
        assertFalse("Block expected to be applicable", true)
    }

    val irrelevantBlock: SidechainBlock = generateNextSidechainBlock(generateGenesisBlock(sidechainTransactionsCompanion, basicSeed = 99L), sidechainTransactionsCompanion, params, basicSeed = 7872832L)
    history.applicableTry(irrelevantBlock) match {
      case Success(_) =>
        assertTrue("Exception expected on applicableTry for block without parent already stored in history.", false)
      case Failure(_) =>
    }

  }

  @Test
  def synchronizationTest(): Unit = {
    val sidechainHistoryStorage1 = new SidechainBlocks(new IODBStoreAdapter(getStore()),
      sidechainTransactionsCompanion, params)
    val transactionIndexesStorage1 = new TransactionIndexes(new IODBStoreAdapter(getStore()))

    // Create first history object
    val history1Try = SidechainHistory.genesisHistory(
      sidechainHistoryStorage1,
      transactionIndexesStorage1,
      params,
      genesisBlock,
      Seq(new SidechainBlockSemanticValidator(params)),
      Seq())
    assertTrue("Genesis history1 creation expected to be successful. ", history1Try.isSuccess)
    var history1: SidechainHistory = history1Try.get
    // Init history1 with 19 more blocks
    var history1blockSeq = Seq[SidechainBlock](genesisBlock)
    var blocksToAppend = 19
    while(blocksToAppend > 0) {
      val block = generateNextSidechainBlock(history1blockSeq.last, sidechainTransactionsCompanion, params, basicSeed = 443356L)
      history1.append(block) match {
        case Success((hist, _)) =>
          history1 = hist
        case Failure(e) => assertFalse("Unexpected Exception occurred during block appending: %s".format(e.getMessage), true)
      }
      // notify history that appended block is valid
      history1 = history1.reportModifierIsValid(block)

      history1blockSeq = history1blockSeq :+ block
      blocksToAppend -= 1
    }
    assertEquals("Expected to have different height", 20, history1.height)

    val sidechainHistoryStorage2 = new SidechainBlocks(new IODBStoreAdapter(getStore()),
      sidechainTransactionsCompanion, params)
    val transactionIndexesStorage2 = new TransactionIndexes(new IODBStoreAdapter(getStore()))

    // Create second history object
    val history2Try = SidechainHistory.genesisHistory(
      sidechainHistoryStorage2,
      transactionIndexesStorage2,
      params,
      genesisBlock,
      Seq(new SidechainBlockSemanticValidator(params)),
      Seq())
    assertTrue("Genesis history2 creation expected to be successful. ", history2Try.isSuccess)
    var history2: SidechainHistory = history2Try.get
    // Init history2 with 18 more blocks
    var history2blockSeq = history1blockSeq.take(19)
    for(block <- history2blockSeq.tail) { // without genesis
      history2.append(block) match {
        case Success((hist, _)) =>
          history2 = hist
        case Failure(e) => assertFalse("Unexpected Exception occurred during block appending: %s".format(e.getMessage), true)
      }
      // notify history that appended block is valid
      history2 = history2.reportModifierIsValid(block)
    }
    assertEquals("Expected to have different height", 19, history2.height)


    // Test 1: retrieve history1 sync info and check against history2
    // get history1 syncInfo
    var history1SyncInfo: SidechainSyncInfo = history1.syncInfo
    assertTrue("History 1 sync info expected to be not empty", history1SyncInfo.knownBlockIds.nonEmpty)
    assertEquals("History 1 sync info starting point expected to be best block of its chain", history1blockSeq.last.id, history1SyncInfo.startingPoints.head._2)

    // Compare history1 syncInfo with history2
    var comparisonResult: History.HistoryComparisonResult = history2.compare(history1SyncInfo)
    assertEquals("History 1 chain expected to be older then history 2 chain", History.Older, comparisonResult)
    // Verify history2 continuationIds for history1 info
    var continuationIds = history2.continuationIds(history1SyncInfo, Int.MaxValue)
    assertTrue("History 2 continuation Ids for history 1 info expected to be empty.", continuationIds.isEmpty)


    // Test 2: check history1 sync info against itself
    comparisonResult = history1.compare(history1SyncInfo)
    assertEquals("History 1 chain expected to equal to itself", History.Equal, comparisonResult)
    // Verify history1 continuationIds for its info
    continuationIds = history1.continuationIds(history1SyncInfo, Int.MaxValue)
    assertTrue("History 1 continuation Ids for itself info expected to be empty.", continuationIds.isEmpty)


    // Test 3: retrieve history2 sync info and check against history1
    // get history2 syncInfo
    var history2SyncInfo: SidechainSyncInfo = history2.syncInfo
    assertTrue("History 2 sync info expected to be not empty", history2SyncInfo.knownBlockIds.nonEmpty)
    assertEquals("History 2 sync info starting point expected to be best block of its chain", history2blockSeq.last.id, history2SyncInfo.startingPoints.head._2)

    // Compare history2 syncInfo with history1
    comparisonResult = history1.compare(history2SyncInfo)
    assertEquals("History 2 chain expected to be younger then history 1 chain", History.Younger, comparisonResult)
    // Verify history1 continuationIds for history2 info
    continuationIds = history1.continuationIds(history2SyncInfo, Int.MaxValue)
    assertTrue("History 1 continuation Ids for history 2 info expected to be defined.", continuationIds.nonEmpty)
    assertEquals("History 1 continuation Ids for history 2 info expected to be with given size empty.", 1, continuationIds.size)
    assertEquals("History 1 continuation Ids for history 2 should contain different data.", history1blockSeq.last.id, continuationIds.head._2)



    // Append to history2 one more block, different to the one in history1
    val forkBlock = generateNextSidechainBlock(history2blockSeq.last, sidechainTransactionsCompanion, params, basicSeed = 77655L)
    history2blockSeq = history2blockSeq :+ forkBlock
    history2.append(forkBlock) match {
      case Success((hist, _)) =>
        history2 = hist
      case Failure(e) => assertFalse("Unexpected Exception occurred during block appending: %s".format(e.getMessage), true)
    }
    // notify history that appended block is valid
    history2 = history2.reportModifierIsValid(forkBlock)


    // Test 4: compare history1 syncInfo with history2, they have fork on lasts block, height is the same.
    comparisonResult = history2.compare(history1SyncInfo)
    assertEquals("History 1 chain expected to be younger then history 2 chain", History.Fork, comparisonResult)
    // Verify history2 continuationIds for history1 info
    continuationIds = history2.continuationIds(history1SyncInfo, Int.MaxValue)
    assertEquals("History 1 continuation Ids for history 2 info expected to be with given size empty.", 1, continuationIds.size)
    assertEquals("History 1 continuation Ids for history 2 should contain different data.", history2blockSeq.last.id, continuationIds.head._2)


    // Test 5: Append history1.bestblock to history2 , but don't make it best.
    // compare history1 syncInfo with history2, they have fork on lasts block, height is the same.
    // Expected to be equal, but history2 will try to provide hist best block inside continuation ids
    history2.append(history1blockSeq.last) match {
      case Success((hist, _)) =>
        history2 = hist
      case Failure(e) => assertFalse("Unexpected Exception occurred during block appending: %s".format(e.getMessage), true)
    }
    comparisonResult = history2.compare(history1SyncInfo)
    assertEquals("History 1 chain expected to be equal then history 2 chain", History.Equal, comparisonResult)
    // Verify history2 continuationIds for history1 info
    continuationIds = history2.continuationIds(history1SyncInfo, Int.MaxValue)
    assertEquals("History 1 continuation Ids for history 2 info expected to be with given size empty.", 1, continuationIds.size)
    assertEquals("History 1 continuation Ids for history 2 should contain different data.", history2blockSeq.last.id, continuationIds.head._2)
  }
}
