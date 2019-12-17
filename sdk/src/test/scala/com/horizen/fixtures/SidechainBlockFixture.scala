package com.horizen.fixtures

import java.time.Instant

import com.horizen.block.SidechainBlock
import com.horizen.box.NoncedBox
import com.horizen.chain.SidechainBlockInfo
import com.horizen.companion.SidechainTransactionsCompanion
import com.horizen.customtypes.SemanticallyInvalidTransaction
import com.horizen.params.NetworkParams
import com.horizen.proposition.Proposition
import com.horizen.secret.PrivateKey25519Creator
import com.horizen.transaction.SidechainTransaction
import com.horizen.utils.{WithdrawalEpochInfo, WithdrawalEpochUtils}
import scorex.core.consensus.ModifierSemanticValidity
import scorex.util.{ModifierId, bytesToId}

class SemanticallyInvalidSidechainBlock(block: SidechainBlock, companion: SidechainTransactionsCompanion)
  extends SidechainBlock(block.parentId, block.timestamp, block.mainchainBlocks, block.sidechainTransactions, block.forgerPublicKey, block.signature, companion) {
  override def semanticValidity(params: NetworkParams): Boolean = false
}

trait SidechainBlockFixture extends MainchainBlockReferenceFixture {

  def generateGenesisBlock(companion: SidechainTransactionsCompanion, basicSeed: Long = 6543211L, genesisMainchainBlockHash: Option[Array[Byte]] = None): SidechainBlock = {
    SidechainBlock.create(
      bytesToId(new Array[Byte](32)),
      Instant.now.getEpochSecond - 10000,
      Seq(generateMainchainBlockReference(blockHash = genesisMainchainBlockHash)),
      Seq(),
      PrivateKey25519Creator.getInstance().generateSecret("genesis_seed%d".format(basicSeed).getBytes),
      companion,
      null
    ).get
  }

  def generateGenesisBlockInfo(genesisMainchainBlockHash: Option[Array[Byte]] = None, validity: ModifierSemanticValidity = ModifierSemanticValidity.Unknown): SidechainBlockInfo = {
    SidechainBlockInfo(
      1,
      (1L << 32) + 1,
      bytesToId(new Array[Byte](32)),
      validity,
      Seq(com.horizen.chain.byteArrayToMainchainBlockReferenceId(genesisMainchainBlockHash.getOrElse(new Array[Byte](32)))),
      WithdrawalEpochInfo(1, 1)
    )
  }

  def changeBlockInfoValidity(blockInfo: SidechainBlockInfo, validity: ModifierSemanticValidity): SidechainBlockInfo = {
    SidechainBlockInfo(
      blockInfo.height,
      blockInfo.score,
      blockInfo.parentId,
      validity,
      blockInfo.mainchainBlockReferenceHashes,
      WithdrawalEpochInfo(blockInfo.withdrawalEpochInfo.epoch, blockInfo.withdrawalEpochInfo.lastEpochIndex)
    )
  }

  def generateBlockInfo(block: SidechainBlock,
                        parentBlockInfo: SidechainBlockInfo,
                        params: NetworkParams,
                        customScore: Option[Long] = None,
                        validity: ModifierSemanticValidity = ModifierSemanticValidity.Unknown): SidechainBlockInfo = {
    SidechainBlockInfo(
      parentBlockInfo.height + 1,
      customScore.getOrElse(parentBlockInfo.score + (parentBlockInfo.mainchainBlockReferenceHashes.size.toLong << 32) + 1),
      block.parentId,
      validity,
      SidechainBlockInfo.mainchainReferencesFromBlock(block),
      WithdrawalEpochUtils.getWithdrawalEpochInfo(block, parentBlockInfo.withdrawalEpochInfo, params)
    )
  }

  def generateGenesisBlockWithNoMainchainReferences(companion: SidechainTransactionsCompanion, basicSeed: Long = 6543211L, genesisMainchainBlockHash: Option[Array[Byte]] = None): SidechainBlock = {
    SidechainBlock.create(
      bytesToId(new Array[Byte](32)),
      Instant.now.getEpochSecond - 10000,
      Seq(),
      Seq(),
      PrivateKey25519Creator.getInstance().generateSecret("genesis_seed%d".format(basicSeed).getBytes),
      companion,
      null
    ).get
  }

  def generateSidechainBlockSeq(count: Int, companion: SidechainTransactionsCompanion, params: NetworkParams, basicSeed: Long = 65432L): Seq[SidechainBlock] = {
    var res: Seq[SidechainBlock] = Seq()

    for(i <- 0 until count) {
      val parentId: ModifierId = {
        if (i == 0)
          params.sidechainGenesisBlockId
        else
          res(i - 1).id
        }

      res = res :+ SidechainBlock.create(
        parentId,
        Instant.now.getEpochSecond - 1000 + i * 10,
        Seq(), //generateMainchainReferences(/*parent = res.flatMap(sb => sb.mainchainBlocks.map(mcBlock => new ByteArrayWrapper(mcBlock.hash))).lastOption*/),
        Seq(),
        PrivateKey25519Creator.getInstance().generateSecret("seed%d".format(basicSeed.toInt + i).getBytes),
        companion,
        params
      ).get
    }
    res
  }

  def generateNextSidechainBlock(sidechainBlock: SidechainBlock,
                                 companion: SidechainTransactionsCompanion,
                                 params: NetworkParams,
                                 basicSeed: Long = 123177L,
                                 transactions: Seq[SidechainTransaction[Proposition, NoncedBox[Proposition]]] = Seq()): SidechainBlock = {
    SidechainBlock.create(
      sidechainBlock.id,
      sidechainBlock.timestamp + 10,
      Seq(),
      transactions,
      PrivateKey25519Creator.getInstance().generateSecret("seed%d".format(basicSeed).getBytes),
      companion,
      params
    ).get
  }

  def createSemanticallyInvalidClone(sidechainBlock: SidechainBlock, companion: SidechainTransactionsCompanion): SidechainBlock = {
    new SemanticallyInvalidSidechainBlock(sidechainBlock, companion)
  }

  // not companion should contain serializer for SemanticallyInvalidTransaction
  def generateNextSidechainBlockWithInvalidTransaction(sidechainBlock: SidechainBlock, companion: SidechainTransactionsCompanion, params: NetworkParams, basicSeed: Long = 12325L): SidechainBlock = {
    SidechainBlock.create(
      sidechainBlock.id,
      sidechainBlock.timestamp + 10,
      Seq(),
      Seq[SidechainTransaction[Proposition, NoncedBox[Proposition]]](
        new SemanticallyInvalidTransaction(sidechainBlock.timestamp - 100).asInstanceOf[SidechainTransaction[Proposition, NoncedBox[Proposition]]]
      ),
      PrivateKey25519Creator.getInstance().generateSecret("seed%d".format(basicSeed).getBytes),
      companion,
      params
    ).get
  }
}