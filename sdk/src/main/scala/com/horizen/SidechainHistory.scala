package com.horizen

import java.util.{ArrayList => JArrayList, List => JList, Optional => JOptional}

import com.horizen.block.{MainchainBlockReference, MainchainHeader, SidechainBlock}
import com.horizen.chain.{MainchainBlockReferenceDataInfo, MainchainHeaderHash, MainchainHeaderInfo, SidechainBlockInfo}
import com.horizen.consensus._
import com.horizen.node.NodeHistory
import com.horizen.node.util.MainchainBlockReferenceInfo
import com.horizen.params.{NetworkParams, NetworkParamsUtils}
import com.horizen.storage.SidechainHistoryStorage
import com.horizen.utils.{BytesUtils, WithdrawalEpochInfo, WithdrawalEpochUtils}
import com.horizen.validation.{HistoryBlockValidator, SemanticBlockValidator}
import scorex.core.NodeViewModifier
import scorex.core.consensus.History._
import scorex.core.consensus.{History, ModifierSemanticValidity}
import scorex.core.validation.RecoverableModifierError
import scorex.util.{ModifierId, ScorexLogging, idToBytes}

import scala.collection.mutable.ListBuffer
import scala.compat.java8.OptionConverters._
import scala.util.{Failure, Success, Try}


class SidechainHistory private (val storage: SidechainHistoryStorage,
                                val consensusDataStorage: ConsensusDataStorage,
                                val params: NetworkParams,
                                semanticBlockValidators: Seq[SemanticBlockValidator],
                                historyBlockValidators: Seq[HistoryBlockValidator])
  extends scorex.core.consensus.History[
      SidechainBlock,
      SidechainSyncInfo,
      SidechainHistory]
  with NetworkParamsUtils
  with TimeToEpochSlotConverter
  with ConsensusDataProvider
  with scorex.core.utils.ScorexEncoding
  with NodeHistory
  with ScorexLogging
{

  override type NVCT = SidechainHistory

  require(NodeViewModifier.ModifierIdSize == 32, "32 bytes ids assumed")

  def height: Int = storage.height
  def bestBlockId: ModifierId = storage.bestBlockId
  def bestBlock: SidechainBlock = storage.bestBlock
  def bestBlockInfo: SidechainBlockInfo = storage.bestBlockInfo

  // Note: if block already exists in History it will be declined inside NodeViewHolder before appending.
  override def append(block: SidechainBlock): Try[(SidechainHistory, ProgressInfo[SidechainBlock])] = Try {
    for(validator <- semanticBlockValidators)
      validator.validate(block).get

    // Non-genesis blocks mast have a parent already present in History
    val parentBlockInfoOption: Option[SidechainBlockInfo] = storage.blockInfoOptionById(block.parentId)
    if(!isGenesisBlock(block.id) && parentBlockInfoOption.isEmpty)
      throw new IllegalArgumentException("Sidechain block %s appending failed: parent block is missed.".format(BytesUtils.toHexString(idToBytes(block.id))))

    for(validator <- historyBlockValidators)
      validator.validate(block, this).get

    val (newStorage: Try[SidechainHistoryStorage], progressInfo: ProgressInfo[SidechainBlock]) = {
      if(isGenesisBlock(block.id)) {
        (
          storage.update(block, SidechainHistory.calculateGenesisBlockInfo(block, params)),
          ProgressInfo(None, Seq(), Seq(block), Seq())
        )
      }
      else {
        val parentBlockInfo = parentBlockInfoOption.get
        val blockInfo: SidechainBlockInfo = calculateBlockInfo(block, parentBlockInfo)
        // Check if we retrieved the next block of best chain
        if (block.parentId.equals(bestBlockId)) {
          (
            storage.update(block, blockInfo),
            ProgressInfo(None, Seq(), Seq(block), Seq())
          )
        } else {
          // Check if retrieved block is the best one, but from another chain
          if (isBestBlock(block, parentBlockInfo)) {
            bestForkChanges(block) match { // get info to switch to another chain
              case Success(progInfo) =>
                (
                  storage.update(block, blockInfo),
                  progInfo
                )
              case Failure(e) =>
                //log.error("New best block found, but it can not be applied: %s".format(e.getMessage))
                (
                  storage.update(block, blockInfo),
                  // TO DO: we should somehow prevent growing of such chain (penalize the peer?)
                  ProgressInfo[SidechainBlock](None, Seq(), Seq(), Seq())
                )

            }
          } else {
            // We retrieved block from another chain that is not the best one
            (
              storage.update(block, blockInfo),
              ProgressInfo[SidechainBlock](None, Seq(), Seq(), Seq())
            )
          }
        }
      }
    }
    new SidechainHistory(newStorage.get, consensusDataStorage, params, semanticBlockValidators, historyBlockValidators) -> progressInfo
  }

  def isBestBlock(block: SidechainBlock, parentBlockInfo: SidechainBlockInfo): Boolean = {
    val currentScore = storage.chainScoreFor(bestBlockId).get
    val newScore = calculateChainScore(block, parentBlockInfo.score)
    if (newScore == currentScore) {
      (parentBlockInfo.height + 1) > height
    } else {
      newScore > currentScore
    }
  }

  // score is a long value
  // parentScore is a sum of the length of the chain till parent block and amount of ommers inside the chain
  // So we should increase chain length by 1 and ommers amount with current block ommers number.
  private def calculateChainScore(block: SidechainBlock, parentScore: Long): Long = {
    parentScore + block.score
  }

  def blockInfoById(blockId: ModifierId): SidechainBlockInfo = storage.blockInfoById(blockId)

  def blockToBlockInfo(block: SidechainBlock): Option[SidechainBlockInfo] = storage.blockInfoOptionById(block.parentId).map(calculateBlockInfo(block, _))


  // Calculate SidechainBlock info based on passed block and parent info.
  private def calculateBlockInfo(block: SidechainBlock, parentBlockInfo: SidechainBlockInfo): SidechainBlockInfo = {
    val lastBlockInPreviousConsensusEpoch = getLastBlockInPreviousConsensusEpoch(block.timestamp, block.parentId)
    val nonceConsensusEpochInfo = getOrCalculateNonceConsensusEpochInfo(block.header.timestamp, block.header.parentId)
    val vrfOutputOpt = getVrfOutput(block.header, nonceConsensusEpochInfo)

    SidechainBlockInfo(
      parentBlockInfo.height + 1,
      calculateChainScore(block, parentBlockInfo.score),
      block.parentId,
      block.timestamp,
      ModifierSemanticValidity.Unknown,
      SidechainBlockInfo.mainchainHeaderHashesFromBlock(block),
      SidechainBlockInfo.mainchainReferenceDataHeaderHashesFromBlock(block),
      WithdrawalEpochUtils.getWithdrawalEpochInfo(block, parentBlockInfo.withdrawalEpochInfo, params),
      vrfOutputOpt, //technically block is not correct from consensus point of view if vrfOutput is None
      lastBlockInPreviousConsensusEpoch
    )
  }

  def bestForkChanges(block: SidechainBlock): Try[ProgressInfo[SidechainBlock]] = Try {
    val (newChainSuffix, currentChainSuffix) = commonBlockSuffixes(modifierById(block.parentId).get)
    if(newChainSuffix.isEmpty && currentChainSuffix.isEmpty)
      throw new IllegalArgumentException("Cannot retrieve fork changes. Fork length is more than params.maxHistoryRewritingLength")

    val newChainSuffixValidity: Boolean = !newChainSuffix.tail.map(isSemanticallyValid)
      .contains(ModifierSemanticValidity.Invalid)

    if(newChainSuffixValidity) {
      val rollbackPoint = newChainSuffix.headOption
      val toRemove = currentChainSuffix.tail.map(id => storage.blockById(id).get)
      val toApply = newChainSuffix.tail.map(id => storage.blockById(id).get) ++ Seq(block)

      require(toRemove.nonEmpty)
      require(toApply.nonEmpty)

      ProgressInfo[SidechainBlock](rollbackPoint, toRemove, toApply, Seq())
    } else {
      //log.info(s"Orphaned block $block from invalid suffix")
      ProgressInfo[SidechainBlock](None, Seq(), Seq(), Seq())
    }
  }

  // Find common suffixes for two chains - starting from forkBlock and from bestBlock.
  // Returns last common block and then variant blocks for two chains.
  private def commonBlockSuffixes(forkBlock: SidechainBlock): (Seq[ModifierId], Seq[ModifierId]) = {
    chainBack(forkBlock.id, storage.isInActiveChain, Int.MaxValue) match {
      case Some(newBestChain) =>
        val commonBlockHeight = storage.blockInfoById(newBestChain.head).height
        if(height - commonBlockHeight > params.maxHistoryRewritingLength)
          // fork length is more than params.maxHistoryRewritingLength
          (Seq[ModifierId](), Seq[ModifierId]())
        else
          (newBestChain, storage.activeChainAfter(newBestChain.head))

      case None => (Seq[ModifierId](), Seq[ModifierId]())
    }
  } ensuring { res =>
    // verify, that both sequences starts from common block
    (res._1.isEmpty && res._2.isEmpty) || res._1.head == res._2.head
  }

  // Go back though chain and get block ids until condition 'until' or reaching the limit
  // None if parent block is not in chain
  // Note: work faster for active chain back (looks inside memory) and slower for fork chain (looks inside disk)
  private def chainBack(blockId: ModifierId,
                        until: ModifierId => Boolean,
                        limit: Int): Option[Seq[ModifierId]] = { // to do
    var acc: Seq[ModifierId] = Seq(blockId)
    var id = blockId

    while(acc.size < limit && !until(id)) {
      storage.parentBlockId(id) match {
        case Some(parentId) =>
          acc = parentId +: acc
          id = parentId
        case _ =>
          //log.warn(s"Parent block for ${encoder.encode(block.id)} not found ")
          return None
      }
    }
    Some(acc)
  }

  override def reportModifierIsValid(block: SidechainBlock): SidechainHistory = {
      var newStorage = storage.updateSemanticValidity(block, ModifierSemanticValidity.Valid).get
      newStorage = newStorage.setAsBestBlock(block, storage.blockInfoById(block.id)).get
      new SidechainHistory(newStorage, consensusDataStorage, params, semanticBlockValidators, historyBlockValidators)
  }

  override def reportModifierIsInvalid(modifier: SidechainBlock, progressInfo: History.ProgressInfo[SidechainBlock]): (SidechainHistory, History.ProgressInfo[SidechainBlock]) = { // to do
    val newHistory: SidechainHistory = Try {
      val newStorage = storage.updateSemanticValidity(modifier, ModifierSemanticValidity.Invalid).get
      new SidechainHistory(newStorage, consensusDataStorage, params, semanticBlockValidators, historyBlockValidators)
    } match {
      case Success(history) => history
      case Failure(e) =>
        //log.error(s"Failed to update validity for block ${encoder.encode(block.id)} with error ${e.getMessage}.")
        new SidechainHistory(storage, consensusDataStorage, params, semanticBlockValidators, historyBlockValidators)
    }

    // In case when we try to apply some fork, which contains at least one invalid block, we should return to the State and History to the "state" before fork.
    // Calculate new ProgressInfo:
    // Set branch point as previous one
    // Remove blocks, that were applied before current invalid one
    // Apply blocks, that were part of ActiveChain
    // skip blocks to Download, that are part of wrong chain we tried to apply.
    val newProgressInfo = ProgressInfo(progressInfo.branchPoint, progressInfo.toApply.takeWhile(block => !block.id.equals(modifier.id)), progressInfo.toRemove, Seq())
    newHistory -> newProgressInfo
  }

  override def isEmpty: Boolean = height <= 0

  override def contains(id: ModifierId): Boolean = storage.blockInfoOptionById(id).isDefined

  override def applicableTry(block: SidechainBlock): Try[Unit] = {
    if (!contains(block.parentId))
      Failure(new RecoverableModifierError("Parent block is not in history yet"))
    else
      Success()
  }

  override def modifierById(blockId: ModifierId): Option[SidechainBlock] = storage.blockById(blockId)

  def blockIdByHeight(height: Int): Option[String] = {
    storage.activeChainBlockId(height)
  }

  override def isSemanticallyValid(blockId: ModifierId): ModifierSemanticValidity = {
    storage.semanticValidity(blockId)
  }

  override def openSurfaceIds(): Seq[ModifierId] = {
    if(isEmpty)
      Seq()
    else
      Seq(bestBlockId)
  }


  override def continuationIds(info: SidechainSyncInfo, size: Int): ModifierIds = {
    info.knownBlockIds.find(id => storage.isInActiveChain(id)) match {
      case Some(commonBlockId) =>
        storage.activeChainAfter(commonBlockId).tail.take(size).map(id => (SidechainBlock.ModifierTypeId, id))
      case None =>
        //log.warn("Found chain without common block ids from remote")
        Seq()
    }
  }

  // see https://en.bitcoin.it/wiki/Protocol_documentation#getblocks
  private def knownBlocksHeightToSync(): Seq[Int] = {
    if (isEmpty)
      return Seq()

    var indexes: Seq[Int] = Seq()

    var step: Int = 1
    // Start at the top of the chain and work backwards.
    var index: Int = height
    while (index > 1) {
      indexes = indexes :+ index
      // Push top 10 indexes first, then back off exponentially.
      if(indexes.size >= 10)
        step *= 2
      index -= step
    }
    // Push the genesis block index.
    indexes :+ 1
  }

  override def syncInfo: SidechainSyncInfo = {
    // collect control points of block ids like in bitcoin (last 10, then increase step exponentially until genesis block)
    SidechainSyncInfo(
      // return a sequence of block ids for given blocks height backward starting from blockId
      knownBlocksHeightToSync().map(height => storage.activeChainBlockId(height).get)
    )

  }

  // get divergent suffix until we reach the end of otherBlockIds or known block in otherBlockIds.
  // return last common block + divergent suffix
  // Note: otherBlockIds ordered from most recent to oldest block
  private def divergentSuffix(otherBlockIds: Seq[ModifierId]): Seq[ModifierId] = {
    var suffix = Seq[ModifierId]()
    var blockId: ModifierId = null
    var restOfOtherBlockIds = otherBlockIds

    while(restOfOtherBlockIds.nonEmpty) {
      blockId = restOfOtherBlockIds.head
      restOfOtherBlockIds = restOfOtherBlockIds.tail
      suffix = blockId +: suffix

      if(storage.isInActiveChain(blockId))
        return suffix
    }
    Seq() // we didn't find common block (even genesis one is different) -> we have totally different chains.
  }

  /** From Scorex History:
    * Whether another's node syncinfo shows that another node is ahead or behind ours
    *
    * @param other other's node sync info
    * @return Equal if nodes have the same history, Younger if another node is behind, Older if a new node is ahead
    */
  override def compare(other: SidechainSyncInfo): History.HistoryComparisonResult = {
    val dSuffix = divergentSuffix(other.knownBlockIds)

    dSuffix.size match {
      case 0 =>
        // log.warn(Nonsence situation..._)
        Nonsense
      case 1 =>
        if(dSuffix.head.equals(bestBlockId))
          Equal
        else
          Younger
      case _ =>
        val otherBestKnownBlockIndex = dSuffix.size - 1 - dSuffix.reverse.indexWhere(id => storage.heightOf(id).isDefined)
        val otherBestKnownBlockHeight = storage.heightOf(dSuffix(otherBestKnownBlockIndex)).get
        // other node height can be approximatly calculated as height of other KNOWN best block height + size of rest unknown blocks after it.
        // why approximately? see knownBlocksHeightToSync algorithm: blocks to sync step increasing.
        // to do: need to discuss
        val otherBestBlockApproxHeight = otherBestKnownBlockHeight + (dSuffix.size - 1 - otherBestKnownBlockIndex)
        if (storage.height < otherBestBlockApproxHeight)
          Older
        else if (storage.height == otherBestBlockApproxHeight) {
          if(otherBestBlockApproxHeight == otherBestKnownBlockHeight)
            Equal // UPDATE: FORK in both cases
          else
            Fork
        }
        else
          Younger
    }
  }

  override def getBlockById(blockId: String): JOptional[SidechainBlock] = {
    storage.blockById(ModifierId(blockId)).asJava
  }

  override def getLastBlockIds(count: Int): JList[String] = {
    val blockList = new JArrayList[String]()
    if(isEmpty)
      blockList
    else {
      var id = bestBlockId
      blockList.add(id)
      while (blockList.size < count && !isGenesisBlock(id)) {
        val parentBlockId = storage.parentBlockId(id).get
        blockList.add(parentBlockId)
        id = parentBlockId
      }
      blockList
    }
  }

  override def getBestBlock: SidechainBlock = {
    bestBlock
  }

  override def getBlockIdByHeight(height: Int): JOptional[String] = {
    storage.activeChainBlockId(height) match {
      case Some(blockId) => JOptional.of[String](blockId)
      case None => JOptional.empty()
    }
  }

  override def getBlockHeightById(id: String): JOptional[Integer] = {
    storage.heightOf(ModifierId(id)) match {
      case Some(height) => JOptional.of[Integer](height)
      case None => JOptional.empty()
    }
  }


  override def getCurrentHeight: Int = {
    height
  }

  override def searchTransactionInsideSidechainBlock(transactionId: String, blockId: String): JOptional[SidechainTypes#SCBT] = {
    storage.blockById(ModifierId(blockId)) match {
      case Some(scBlock) => findTransactionInsideBlock(transactionId, scBlock)
      case None => JOptional.empty()
    }
  }

  private def findTransactionInsideBlock(transactionId : String, block : SidechainBlock) : JOptional[SidechainTypes#SCBT] = {
    block.transactions.find(box => box.id.equals(ModifierId(transactionId))) match {
      case Some(tx) => JOptional.ofNullable(tx)
      case None => JOptional.empty()
    }
  }

  override def searchTransactionInsideBlockchain(transactionId: String): JOptional[SidechainTypes#SCBT] = {
    var startingBlock = JOptional.ofNullable(getBestBlock)
    var transaction : JOptional[SidechainTypes#SCBT] = JOptional.empty()
    var found = false
    while(!found && startingBlock.isPresent){
      val tx = findTransactionInsideBlock(transactionId, startingBlock.get())
      if(tx.isPresent){
        found = true
        transaction = JOptional.ofNullable(tx.get())
      }else{
        startingBlock = storage.parentBlockId(startingBlock.get().id) match {
          case Some(id) => storage.blockById(id) match {
            case Some(block) => JOptional.ofNullable(block)
            case None => JOptional.empty()
          }
          case None => JOptional.empty()
        }
      }
    }

    transaction
  }

  /*
    All the methods in SidechainHistory and NodeHistory, that work with MainchainBlockReferences,
    MainchainHeaders, MainchainBlockReferenceData itself or any information about them,
    are designed to work with ActiveChain data only.
    In a Sidechain we are not interested in Mainchain data form Sidechain forks.
   */
  override def getMainchainCreationBlockHeight: Int = params.mainchainCreationBlockHeight

  override def getBestMainchainBlockReferenceInfo: JOptional[MainchainBlockReferenceInfo] = {
    storage.getBestMainchainBlockReferenceInfo.asJava
  }

  override def getMainchainBlockReferenceInfoByMainchainBlockHeight(height: Int): JOptional[MainchainBlockReferenceInfo] = {
    storage.getMainchainBlockReferenceInfoByMainchainBlockHeight(height).asJava
  }

  override def getMainchainBlockReferenceInfoByHash(mainchainBlockReferenceHash: Array[Byte]): JOptional[MainchainBlockReferenceInfo] = {
    storage.getMainchainBlockReferenceInfoByHash(mainchainBlockReferenceHash).asJava
  }

  override def getMainchainBlockReferenceByHash(mainchainHeaderHash: Array[Byte]): JOptional[MainchainBlockReference] = {
    storage.getMainchainBlockReferenceByHash(mainchainHeaderHash).asJava
  }

  override def getMainchainHeaderByHash(mainchainHeaderHash: Array[Byte]): JOptional[MainchainHeader] = {
    storage.getMainchainHeaderByHash(mainchainHeaderHash).asJava
  }

  def getBestMainchainHeaderInfo: Option[MainchainHeaderInfo] = storage.getBestMainchainHeaderInfo

  def getMainchainHeaderInfoByHeight(height: Int): Option[MainchainHeaderInfo] = storage.getMainchainHeaderInfoByHeight(height)

  def getMainchainHeaderInfoByHash(mainchainHeaderHash: Array[Byte]): Option[MainchainHeaderInfo] = storage.getMainchainHeaderInfoByHash(mainchainHeaderHash)

  def getBestMainchainBlockReferenceDataInfo: Option[MainchainBlockReferenceDataInfo] = storage.getBestMainchainBlockReferenceDataInfo

  def getMainchainBlockReferenceDataInfoByHeight(height: Int): Option[MainchainBlockReferenceDataInfo] = storage.getMainchainBlockReferenceDataInfoByHeight(height)

  def getMainchainBlockReferenceDataInfoByHash(mainchainHeaderHash: Array[Byte]): Option[MainchainBlockReferenceDataInfo] = storage.getMainchainBlockReferenceDataInfoByHash(mainchainHeaderHash)

  // Create MC Locator sequence from most recent Mainchain Header to MC Creation Block
  // Locator in Bitcoin style
  def getMainchainHashesLocator: Seq[MainchainHeaderHash] = {
    val firstMainchainHeaderHeight: Int = getMainchainCreationBlockHeight

    val indexes: ListBuffer[Int] = ListBuffer()
    var step: Int = 1
    var index: Int = storage.getBestMainchainHeaderInfo.get.height

    while (index > firstMainchainHeaderHeight) {
      indexes.append(index)
      // Push top 10 indexes first, then back off exponentially.
      if (indexes.size >= 10)
        step *= 2
      index -= step
    }
    // Push the genesis mc ref index.
    indexes.append(firstMainchainHeaderHeight)

    storage.getMainchainHashesForIndexes(indexes)
  }

  // Retrieve the sequence of MainchainHeader hashes from last hash to best hash.
  // best - the most recent hash, last - the oldest hash
  def getMainchainHashes(last: Array[Byte], best: Array[Byte]): Seq[MainchainHeaderHash] = {
    val bestHeight = storage.getMainchainHeaderInfoByHash(best)
      .getOrElse(throw new IllegalArgumentException(s"MainchainHeader hash $best not found."))
      .height
    val lastHeight = storage.getMainchainHeaderInfoByHash(last)
      .getOrElse(throw new IllegalArgumentException(s"MainchainHeader hash $last not found."))
      .height
    require(bestHeight >= lastHeight, "Best hash must lead to the higher MainchainHeader than the last one.")

    val indexes: Seq[Int] = lastHeight to bestHeight
    storage.getMainchainHashesForIndexes(indexes)
  }

  // Return Hashes of MainchainHeader that are present in Active chain, but wait for MainchainBlockReferenceData to be synchronized.
  // Hashes ordered from oldest to MC tip
  def missedMainchainReferenceDataHeaderHashes: Seq[MainchainHeaderHash] = {
    val bestMainchainReferenceDataHeight: Int = getBestMainchainBlockReferenceDataInfo.get.height
    val bestMainchainHeaderHeight: Int = getBestMainchainHeaderInfo.get.height

    if(bestMainchainHeaderHeight == bestMainchainReferenceDataHeight)
      Seq()
    else {
      val missedDataIndexes = bestMainchainReferenceDataHeight + 1 to bestMainchainHeaderHeight
      storage.getMainchainHashesForIndexes(missedDataIndexes)
    }
  }

  def applyFullConsensusInfo(lastBlockInEpoch: ModifierId, fullConsensusEpochInfo: FullConsensusEpochInfo): SidechainHistory = {
    consensusDataStorage.addStakeConsensusEpochInfo(blockIdToEpochId(lastBlockInEpoch), fullConsensusEpochInfo.stakeConsensusEpochInfo)
    consensusDataStorage.addNonceConsensusEpochInfo(blockIdToEpochId(lastBlockInEpoch), fullConsensusEpochInfo.nonceConsensusEpochInfo)

    new SidechainHistory(storage, consensusDataStorage, params, semanticBlockValidators, historyBlockValidators)
  }
}

object SidechainHistory
{
  private[horizen] def restoreHistory(historyStorage: SidechainHistoryStorage,
                                      consensusDataStorage: ConsensusDataStorage,
                                      params: NetworkParams,
                                      semanticBlockValidators: Seq[SemanticBlockValidator],
                                      historyBlockValidators: Seq[HistoryBlockValidator]): Option[SidechainHistory] = {

    if (!historyStorage.isEmpty)
      Some(new SidechainHistory(historyStorage, consensusDataStorage, params, semanticBlockValidators, historyBlockValidators))
    else
      None
  }

  def calculateGenesisBlockInfo(block: SidechainBlock, params: NetworkParams): SidechainBlockInfo = {
    require(block.id == params.sidechainGenesisBlockId, "Passed block is not a genesis block.")

    SidechainBlockInfo(
      1,
      block.score,
      block.parentId,
      block.timestamp,
      ModifierSemanticValidity.Unknown,
      SidechainBlockInfo.mainchainHeaderHashesFromBlock(block),
      SidechainBlockInfo.mainchainReferenceDataHeaderHashesFromBlock(block),
      WithdrawalEpochInfo(1, block.mainchainBlockReferencesData.size), // First Withdrawal epoch value. Note: maybe put to params?
      None,
      block.id,
    )
  }

  private[horizen] def createGenesisHistory(historyStorage: SidechainHistoryStorage,
                                      consensusDataStorage: ConsensusDataStorage,
                                      params: NetworkParams,
                                      genesisBlock: SidechainBlock,
                                      semanticBlockValidators: Seq[SemanticBlockValidator],
                                      historyBlockValidators: Seq[HistoryBlockValidator],
                                      stakeEpochInfo: StakeConsensusEpochInfo) : Try[SidechainHistory] = Try {

    if (historyStorage.isEmpty) {
      val nonceEpochInfo = ConsensusDataProvider.calculateNonceForGenesisBlock(params)
      new SidechainHistory(historyStorage, consensusDataStorage, params, semanticBlockValidators, historyBlockValidators)
        .append(genesisBlock).map(_._1).get.reportModifierIsValid(genesisBlock).applyFullConsensusInfo(genesisBlock.id, FullConsensusEpochInfo(stakeEpochInfo, nonceEpochInfo))
    }
    else
      throw new RuntimeException("History storage is not empty!")
  }
}
