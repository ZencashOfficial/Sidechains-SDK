package com.horizen.forge

import com.horizen.block._
import com.horizen.box.{ForgerBox, NoncedBox}
import com.horizen.chain.{MainchainHeaderHash, SidechainBlockInfo}
import com.horizen.companion.SidechainTransactionsCompanion
import com.horizen.consensus._
import com.horizen.params.{NetworkParams, RegTestParams}
import com.horizen.proof.VrfProof
import com.horizen.proposition.Proposition
import com.horizen.secret.{PrivateKey25519, VrfSecretKey}
import com.horizen.transaction.SidechainTransaction
import com.horizen.utils.{ForgingStakeMerklePathInfo, MerklePath, TimeToEpochUtils}
import com.horizen.{SidechainHistory, SidechainMemoryPool, SidechainState, SidechainWallet}
import scorex.core.NodeViewHolder.ReceivableMessages.GetDataFromCurrentView
import scorex.util.{ModifierId, ScorexLogging}
import com.horizen.chain._
import com.horizen.vrf.VrfOutput

import scala.util.{Failure, Success, Try}

class ForgeMessageBuilder(mainchainSynchronizer: MainchainSynchronizer,
                          companion: SidechainTransactionsCompanion,
                          val params: NetworkParams,
                          allowNoWebsocketConnectionInRegtest: Boolean) extends ScorexLogging {
  type ForgeMessageType = GetDataFromCurrentView[SidechainHistory, SidechainState, SidechainWallet, SidechainMemoryPool, ForgeResult]

  case class BranchPointInfo(branchPointId: ModifierId, referenceDataToInclude: Seq[MainchainHeaderHash], headersToInclude: Seq[MainchainHeaderHash])

  def buildForgeMessageForEpochAndSlot(consensusEpochNumber: ConsensusEpochNumber, consensusSlotNumber: ConsensusSlotNumber): ForgeMessageType = {
      val forgingFunctionForEpochAndSlot: View => ForgeResult = tryToForgeNextBlock(consensusEpochNumber, consensusSlotNumber)

      val forgeMessage: ForgeMessageType =
        GetDataFromCurrentView[SidechainHistory, SidechainState, SidechainWallet, SidechainMemoryPool, ForgeResult](forgingFunctionForEpochAndSlot)

      forgeMessage
  }

  protected def tryToForgeNextBlock(nextConsensusEpochNumber: ConsensusEpochNumber, nextConsensusSlotNumber: ConsensusSlotNumber)(nodeView: View): ForgeResult = Try {
    log.info(s"Try to forge block for epoch $nextConsensusEpochNumber with slot $nextConsensusSlotNumber")

    val branchPointInfo: BranchPointInfo = getBranchPointInfo(nodeView.history) match {
      case Success(info) => info
      case Failure(ex) => return ForgeFailed(ex)
    }

    val parentBlockId: ModifierId = branchPointInfo.branchPointId
    val parentBlockInfo = nodeView.history.blockInfoById(parentBlockId)

    checkNextEpochAndSlot(parentBlockInfo.timestamp,nodeView.history.bestBlockInfo.timestamp,
        nextConsensusEpochNumber, nextConsensusSlotNumber) match {
      case Some(forgeFailure) => return forgeFailure
      case _ => // checks passed
    }

    val nextBlockTimestamp = TimeToEpochUtils.getTimeStampForEpochAndSlot(params, nextConsensusEpochNumber, nextConsensusSlotNumber)
    val consensusInfo: FullConsensusEpochInfo = nodeView.history.getFullConsensusEpochInfoForBlock(nextBlockTimestamp, parentBlockId)
    val totalStake = consensusInfo.stakeConsensusEpochInfo.totalStake
    val vrfMessage = buildVrfMessage(nextConsensusSlotNumber, consensusInfo.nonceConsensusEpochInfo)

    val sidechainWallet = nodeView.vault

    // Get ForgingStakeMerklePathInfo from wallet and order them by stake decreasing.
    val forgingStakeMerklePathInfoSeq: Seq[ForgingStakeMerklePathInfo] =
      sidechainWallet.getForgingStakeMerklePathInfoOpt(nextConsensusEpochNumber).getOrElse(Seq())
        .sortWith(_.forgingStakeInfo.stakeAmount > _.forgingStakeInfo.stakeAmount)

    if (forgingStakeMerklePathInfoSeq.isEmpty) {
      NoOwnedForgingStake
    } else {
      val ownedForgingDataView: Seq[(ForgingStakeMerklePathInfo, PrivateKey25519, VrfProof, VrfOutput)]
      = forgingStakeMerklePathInfoSeq.view.flatMap(forgingStakeMerklePathInfo => getSecretsAndProof(sidechainWallet, vrfMessage, forgingStakeMerklePathInfo))

      val eligibleForgingDataView: Seq[(ForgingStakeMerklePathInfo, PrivateKey25519, VrfProof, VrfOutput)]
      = ownedForgingDataView.filter { case (forgingStakeMerklePathInfo, _, _, vrfOutput) =>
        vrfProofCheckAgainstStake(vrfOutput, forgingStakeMerklePathInfo.forgingStakeInfo.stakeAmount, totalStake)
      }


      val eligibleForgerOpt = eligibleForgingDataView.headOption //force all forging related calculations

      val forgingResult = eligibleForgerOpt
        .map { case (forgingStakeMerklePathInfo, privateKey25519, vrfProof, _) =>
          forgeBlock(nodeView, nextBlockTimestamp, branchPointInfo, forgingStakeMerklePathInfo, privateKey25519, vrfProof)
        }
        .getOrElse(SkipSlot("No eligible forging stake found."))
      forgingResult
    }
  } match {
      case Success(result) => {
        log.info(s"Forge result is: $result")
        result
      }
      case Failure(ex) => {
        log.error(s"Failed to forge block for ${nextConsensusEpochNumber} epoch ${nextConsensusSlotNumber} slot due:" , ex)
        ForgeFailed(ex)
    }
  }

  private def getSecretsAndProof(wallet: SidechainWallet,
                                 vrfMessage: VrfMessage,
                                 forgingStakeMerklePathInfo: ForgingStakeMerklePathInfo): Option[(ForgingStakeMerklePathInfo, PrivateKey25519, VrfProof, VrfOutput)] = {
    for {
      blockSignPrivateKey <- wallet.secret(forgingStakeMerklePathInfo.forgingStakeInfo.blockSignPublicKey).asInstanceOf[Option[PrivateKey25519]]
      vrfSecret <- wallet.secret(forgingStakeMerklePathInfo.forgingStakeInfo.vrfPublicKey).asInstanceOf[Option[VrfSecretKey]]
      vrfProofAndHash <- Some(vrfSecret.prove(vrfMessage))
    } yield {
      val vrfProof = vrfProofAndHash.getKey
      val vrfOutput = vrfProofAndHash.getValue
      (forgingStakeMerklePathInfo, blockSignPrivateKey, vrfProof, vrfOutput)
    }
  }

  private def checkNextEpochAndSlot(parentBlockTimestamp: Long,
                                    currentTipBlockTimestamp: Long,
                                    nextEpochNumber: ConsensusEpochNumber,
                                    nextSlotNumber: ConsensusSlotNumber): Option[ForgeFailure] = {
    // Parent block and current tip block can be the same in case of extension the Active chain.
    // But can be different in case of sidechain fork caused by mainchain fork.
    // In this case parent block is before the tip, and tip block will be the last Ommer included into the next block.
    val parentBlockEpochAndSlot: ConsensusEpochAndSlot = TimeToEpochUtils.timestampToEpochAndSlot(params, parentBlockTimestamp)
    val currentTipBlockEpochAndSlot: ConsensusEpochAndSlot = TimeToEpochUtils.timestampToEpochAndSlot(params, currentTipBlockTimestamp)
    val nextBlockEpochAndSlot: ConsensusEpochAndSlot = ConsensusEpochAndSlot(nextEpochNumber, nextSlotNumber)

    if(parentBlockEpochAndSlot > nextBlockEpochAndSlot) {
      return Some(ForgeFailed(new IllegalArgumentException (s"Try to forge block with incorrect epochAndSlot $nextBlockEpochAndSlot which are equal or less than parent block epochAndSlot: $parentBlockEpochAndSlot")))
    }

    if(parentBlockEpochAndSlot == nextBlockEpochAndSlot) {
      return Some(SkipSlot(s"Chain tip with $nextBlockEpochAndSlot has been generated already."))
    }

    if ((nextEpochNumber - parentBlockEpochAndSlot.epochNumber) > 1) {
      return Some(ForgeFailed(new IllegalArgumentException (s"Forging is not possible, because of whole consensus epoch is missed: current epoch = $nextEpochNumber, parent epoch = ${parentBlockEpochAndSlot.epochNumber}")))
    }

    if(currentTipBlockEpochAndSlot >= nextBlockEpochAndSlot) {
      return Some(ForgeFailed(new IllegalArgumentException (s"Try to forge block with incorrect epochAndSlot $nextBlockEpochAndSlot which are equal or less than last ommer epochAndSlot: $currentTipBlockEpochAndSlot")))
    }

    None
  }

  private def getBranchPointInfo(history: SidechainHistory): Try[BranchPointInfo] = Try {
    val bestMainchainHeaderInfo = history.getBestMainchainHeaderInfo.get

    val (bestMainchainCommonPointHeight: Int, bestMainchainCommonPointHash: MainchainHeaderHash, newHeaderHashes: Seq[MainchainHeaderHash]) =
      mainchainSynchronizer.getMainchainDivergentSuffix(history, MainchainSynchronizer.MAX_BLOCKS_REQUEST) match {
        case Success((height, hashes)) => (height, hashes.head, hashes.tail) // hashes contains also the hash of best known block
        case Failure(ex) =>
          // For regtest Forger is allowed to produce next block in case if there is no MC Node connection
          if (params.isInstanceOf[RegTestParams] && allowNoWebsocketConnectionInRegtest)
            (bestMainchainHeaderInfo.height, bestMainchainHeaderInfo.hash, Seq())
          else
            throw ex
      }

    // Check that there is no orphaned mainchain headers: SC most recent mainchain header is a part of MC active chain
    if(bestMainchainCommonPointHash == bestMainchainHeaderInfo.hash) {
      val branchPointId: ModifierId = history.bestBlockId
      var withdrawalEpochMcBlocksLeft = params.withdrawalEpochLength - history.bestBlockInfo.withdrawalEpochInfo.lastEpochIndex
      if (withdrawalEpochMcBlocksLeft == 0) // current best block is the last block of the epoch
        withdrawalEpochMcBlocksLeft = params.withdrawalEpochLength

      // to not to include mcblock references data from different withdrawal epochs
      val maxReferenceDataNumber: Int = Math.min(SidechainBlock.MAX_MC_BLOCKS_NUMBER, withdrawalEpochMcBlocksLeft)

      val missedMainchainReferenceDataHeaderHashes: Seq[MainchainHeaderHash] = history.missedMainchainReferenceDataHeaderHashes
      val nextMainchainReferenceDataHeaderHashes: Seq[MainchainHeaderHash] = missedMainchainReferenceDataHeaderHashes ++ newHeaderHashes

      val mainchainReferenceDataHeaderHashesToInclude = nextMainchainReferenceDataHeaderHashes.take(maxReferenceDataNumber)
      val mainchainHeadersHashesToInclude = newHeaderHashes

      BranchPointInfo(branchPointId, mainchainReferenceDataHeaderHashesToInclude, mainchainHeadersHashesToInclude)
    }
    else { // Some blocks in SC Active chain contains orphaned MainchainHeaders
      val orphanedMainchainHeadersNumber: Int = bestMainchainHeaderInfo.height - bestMainchainCommonPointHeight
      val newMainchainHeadersNumber = newHeaderHashes.size

      if (orphanedMainchainHeadersNumber >= newMainchainHeadersNumber) {
        ForgeFailed(new Exception("No sense to forge: active branch contains orphaned MainchainHeaders, that number is greater or equal to actual new MainchainHeaders."))
      }

      val firstOrphanedHashHeight: Int = bestMainchainCommonPointHeight + 1
      val firstOrphanedMainchainHeaderInfo = history.getMainchainHeaderInfoByHeight(firstOrphanedHashHeight).get
      val orphanedSidechainBlockId: ModifierId = firstOrphanedMainchainHeaderInfo.sidechainBlockId
      val orphanedSidechainBlockInfo: SidechainBlockInfo = history.blockInfoById(orphanedSidechainBlockId)

      if (firstOrphanedMainchainHeaderInfo.hash.equals(orphanedSidechainBlockInfo.mainchainHeaderHashes.head)) {
        // First orphaned MainchainHeader is the first header inside the containing SidechainBlock, so no common MainchainHeaders present in SidechainBlock before it
        BranchPointInfo(orphanedSidechainBlockInfo.parentId, Seq(), newHeaderHashes)
      }
      else {
        // SidechainBlock also contains some common MainchainHeaders before first orphaned MainchainHeader
        // So we should add that common MainchainHeaders to the SidechainBlock as well
        BranchPointInfo(orphanedSidechainBlockInfo.parentId, Seq(),
          orphanedSidechainBlockInfo.mainchainHeaderHashes.takeWhile(hash => !hash.equals(firstOrphanedMainchainHeaderInfo.hash)) ++ newHeaderHashes)
      }
    }
  }

  private def forgeBlock(nodeView: View,
                         timestamp: Long,
                         branchPointInfo: BranchPointInfo,
                         forgingStakeMerklePathInfo: ForgingStakeMerklePathInfo,
                         blockSignPrivateKey: PrivateKey25519,
                         vrfProof: VrfProof): ForgeResult = {
    val parentBlockId: ModifierId = branchPointInfo.branchPointId
    val parentBlockInfo: SidechainBlockInfo = nodeView.history.blockInfoById(branchPointInfo.branchPointId)
    var withdrawalEpochMcBlocksLeft: Int = params.withdrawalEpochLength - parentBlockInfo.withdrawalEpochInfo.lastEpochIndex
    if (withdrawalEpochMcBlocksLeft == 0) // parent block is the last block of the epoch
      withdrawalEpochMcBlocksLeft = params.withdrawalEpochLength

    // Get all needed MainchainBlockReferences from MC Node
    val mainchainBlockHashesToRetrieve: Seq[MainchainHeaderHash] = branchPointInfo.referenceDataToInclude
    val mainchainHeaderHashesToRetrieve: Seq[MainchainHeaderHash] = branchPointInfo.headersToInclude

    val mainchainBlockReferences: Seq[MainchainBlockReference] =
      mainchainSynchronizer.getMainchainBlockReferences(nodeView.history, mainchainBlockHashesToRetrieve) match {
        case Success(references) => references
        case Failure(ex) => return ForgeFailed(ex)
      }

    // Extract proper MainchainReferenceData
    val mainchainReferenceData: Seq[MainchainBlockReferenceData] =
      mainchainBlockReferences.map(_.data)

    // Extract proper MainchainHeaders
    val mainchainHeaders: Seq[MainchainHeader] =
      mainchainSynchronizer.getMainchainBlockHeaders(mainchainHeaderHashesToRetrieve) match {
        case Success(references) => references
        case Failure(ex) => return ForgeFailed(ex)
      }

    // Get transactions if possible
    val transactions: Seq[SidechainTransaction[Proposition, NoncedBox[Proposition]]] =
      if (branchPointInfo.referenceDataToInclude.size == withdrawalEpochMcBlocksLeft) { // SC block is going to become the last block of the withdrawal epoch
        Seq() // no SC Txs allowed
      } else { // SC block is in the middle of the epoch
        nodeView.pool.take(SidechainBlock.MAX_SIDECHAIN_TXS_NUMBER) // TO DO: problems with types
          .map(t => t.asInstanceOf[SidechainTransaction[Proposition, NoncedBox[Proposition]]])
          .toSeq
      }

    // Get ommers in case if branch point is not current best block
    var ommers: Seq[Ommer] = Seq()
    var blockId = nodeView.history.bestBlockId
    while (blockId != branchPointInfo.branchPointId) {
      val block = nodeView.history.getBlockById(blockId).get() // TODO: replace with method blockById with no Option
      blockId = block.parentId
      ommers = Ommer.toOmmer(block) +: ommers
    }

    val tryBlock = SidechainBlock.create(
      parentBlockId,
      timestamp,
      mainchainReferenceData,
      transactions,
      mainchainHeaders,
      ommers,
      blockSignPrivateKey,
      forgingStakeMerklePathInfo.forgingStakeInfo,
      vrfProof,
      forgingStakeMerklePathInfo.merklePath,
      companion,
      params)

    tryBlock match {
      case Success(block) => ForgeSuccess(block)
      case Failure(exception) => ForgeFailed(exception)
    }
  }
}




