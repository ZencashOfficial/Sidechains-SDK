package com.horizen.fixtures.sidechainblock.generation

import java.math.BigInteger
import java.security.MessageDigest
import java.time.Instant
import java.util.Random

import com.google.common.primitives.{Ints, Longs}
import com.horizen.block._
import com.horizen.box.data.ForgerBoxData
import com.horizen.box.{ForgerBox, NoncedBox}
import com.horizen.companion.SidechainTransactionsCompanion
import com.horizen.consensus._
import com.horizen.fixtures._
import com.horizen.params.{NetworkParams, RegTestParams}
import com.horizen.proof.{Signature25519, VrfProof}
import com.horizen.proposition.{Proposition, PublicKey25519Proposition, SchnorrProposition, VrfPublicKey}
import com.horizen.secret.{PrivateKey25519, PrivateKey25519Creator, VrfKeyGenerator}
import com.horizen.storage.InMemoryStorageAdapter
import com.horizen.transaction.SidechainTransaction
import com.horizen.transaction.mainchain.SidechainCreation
import com.horizen.utils
import com.horizen.utils._
import com.horizen.vrf._
import com.horizen.cryptolibprovider.{VrfFunctions, CryptoLibProvider}
import scorex.core.block.Block
import scorex.util.{ModifierId, bytesToId}

import scala.collection.JavaConverters._


case class GeneratedBlockInfo(block: SidechainBlock, forger: SidechainForgingData)
case class FinishedEpochInfo(epochId: ConsensusEpochId, stakeConsensusEpochInfo: StakeConsensusEpochInfo, nonceConsensusEpochInfo: NonceConsensusEpochInfo)

//will be thrown if block generation no longer is possible, for example: nonce no longer can be calculated due no mainchain references in whole epoch
class GenerationIsNoLongerPossible extends IllegalStateException

// @TODO consensusDataStorage is shared between generator instances, so data could be already added. Shall be fixed.
class SidechainBlocksGenerator private (val params: NetworkParams,
                                        forgersSet: PossibleForgersSet,
                                        consensusDataStorage: ConsensusDataStorage,
                                        val lastBlockId: Block.BlockId,
                                        lastMainchainBlockId: ByteArrayWrapper,
                                        val nextFreeSlotNumber: ConsensusSlotNumber,
                                        nextEpochNumber: ConsensusEpochNumber,
                                        nextBlockNonceEpochId: ConsensusEpochId,
                                        nextBlockStakeEpochId: ConsensusEpochId,
                                        allEligibleVrfOutputs: List[Array[Byte]],
                                        previousEpochId: ConsensusEpochId,
                                        rnd: Random) extends TimeToEpochSlotConverter {
  import SidechainBlocksGenerator._

  def getNotSpentBoxes: Set[SidechainForgingData] = forgersSet.getNotSpentSidechainForgingData.map(_.copy())

  def tryToGenerateBlockForCurrentSlot(generationRules: GenerationRules): Option[GeneratedBlockInfo] = {
    checkGenerationRules(generationRules)
    val nextSlot = intToConsensusSlotNumber(Math.min(nextFreeSlotNumber, params.consensusSlotsInEpoch))
    getNextEligibleForgerForCurrentEpoch(generationRules, nextSlot).map{
      case (blockForger, vrfProof, vrfOutput, usedSlotNumber) => {
        println(s"Got forger for block: ${blockForger}")
        val newBlock = generateBlock(blockForger, vrfProof, vrfOutput, usedSlotNumber, generationRules)
        GeneratedBlockInfo(newBlock, blockForger.forgingData)
      }
    }
  }

  def tryToGenerateCorrectBlock(generationRules: GenerationRules): (SidechainBlocksGenerator, Either[FinishedEpochInfo, GeneratedBlockInfo]) = {
    checkGenerationRules(generationRules)
    getNextEligibleForgerForCurrentEpoch(generationRules, intToConsensusSlotNumber(params.consensusSlotsInEpoch)) match {
      case Some((blockForger, vrfProof, vrfOutput, usedSlotNumber)) => {
        println(s"Got forger: ${blockForger}")
        val newBlock: SidechainBlock = generateBlock(blockForger, vrfProof, vrfOutput, usedSlotNumber, generationRules)
        val newForgers: PossibleForgersSet = forgersSet.createModified(generationRules)
        val generator = createGeneratorAfterBlock(newBlock, usedSlotNumber, newForgers, vrfOutput)
        (generator, Right(GeneratedBlockInfo(newBlock, blockForger.forgingData)))
      }
      case None => {
        val (generator, finishedEpochInfo) = createGeneratorAndFinishedEpochInfo
        (generator, Left(finishedEpochInfo))
      }
    }
  }

  private def checkGenerationRules(generationRules: GenerationRules): Unit = {
    checkInputSidechainForgingData(generationRules.forgingBoxesToAdd, generationRules.forgingBoxesToSpent)
  }

  private def checkInputSidechainForgingData(forgingBoxesToAdd: Set[SidechainForgingData], forgingBoxesToSpent: Set[SidechainForgingData]): Unit = {
    val incorrectBoxesToAdd: Set[SidechainForgingData] = forgingBoxesToAdd & forgersSet.getAvailableSidechainForgingData
    if (incorrectBoxesToAdd.nonEmpty) {
      throw new IllegalArgumentException(s"Try to add already existed SidechainForgingData(s): ${incorrectBoxesToAdd.mkString(",")}")
    }

    val incorrectBoxesToSpent: Set[SidechainForgingData] = forgingBoxesToSpent -- forgersSet.getAvailableSidechainForgingData
    if (incorrectBoxesToSpent.nonEmpty) {
      throw new IllegalArgumentException(s"Try to spent non existed SidechainForgingData(s): ${incorrectBoxesToSpent.mkString(",")}")
    }
  }

  private def getNextEligibleForgerForCurrentEpoch(generationRules: GenerationRules, endSlot: ConsensusSlotNumber): Option[(PossibleForger, VrfProof, VrfOutput, ConsensusSlotNumber)] = {
    require(nextFreeSlotNumber <= endSlot + 1)

    val initialNonce: Array[Byte] = consensusDataStorage.getNonceConsensusEpochInfo(nextBlockNonceEpochId).get.consensusNonce
    val changedNonceBytes: Long = Longs.fromByteArray(initialNonce.take(Longs.BYTES)) + generationRules.corruption.consensusNonceShift
    val nonce =  Longs.toByteArray(changedNonceBytes) ++ initialNonce.slice(Longs.BYTES, initialNonce.length)

    val consensusNonce: NonceConsensusEpochInfo = NonceConsensusEpochInfo(ConsensusNonce @@ nonce)

    val totalStake = consensusDataStorage.getStakeConsensusEpochInfo(nextBlockStakeEpochId).get.totalStake

    val possibleForger = (nextFreeSlotNumber to endSlot)
      .toStream
      .flatMap{currentSlot =>
        val slotWithShift = intToConsensusSlotNumber(Math.min(currentSlot + generationRules.corruption.consensusSlotShift, params.consensusSlotsInEpoch))
        println(s"Process slot: ${slotWithShift}")
        val res = forgersSet.getEligibleForger(slotWithShift, consensusNonce, totalStake, generationRules.corruption.getStakeCheckCorruptionFunction)
        if (res.isEmpty) {println(s"No forger had been found for slot ${currentSlot}")}
        res.map{case(forger, proof, vrfOutput) => (forger, proof, vrfOutput, intToConsensusSlotNumber(currentSlot))}
      }
      .headOption

    possibleForger
  }


  private def createGeneratorAfterBlock(newBlock: SidechainBlock,
                                        usedSlot: ConsensusSlotNumber,
                                        newForgers: PossibleForgersSet,
                                        vrfOutput: VrfOutput): SidechainBlocksGenerator = {
    val quietSlotsNumber = params.consensusSlotsInEpoch / 3
    val eligibleSlotsRange = ((quietSlotsNumber + 1) until (params.consensusSlotsInEpoch - quietSlotsNumber))

    new SidechainBlocksGenerator(
      params = params,
      forgersSet = newForgers,
      consensusDataStorage = consensusDataStorage,
      lastBlockId = newBlock.id,
      lastMainchainBlockId = newBlock.mainchainBlockReferencesData.lastOption.map(data => byteArrayToWrapper(data.headerHash)).getOrElse(lastMainchainBlockId),
      nextFreeSlotNumber = intToConsensusSlotNumber(usedSlot + 1), //in case if current slot is the last in epoch then next try to apply new block will finis epoch
      nextEpochNumber = nextEpochNumber,
      nextBlockNonceEpochId = nextBlockNonceEpochId,
      nextBlockStakeEpochId = nextBlockStakeEpochId,
      allEligibleVrfOutputs =
        if (eligibleSlotsRange.contains(usedSlot)) allEligibleVrfOutputs :+ vrfOutput.bytes() else allEligibleVrfOutputs,
      previousEpochId = previousEpochId,
      rnd = rnd
    )
  }

  private def generateBlock(possibleForger: PossibleForger, vrfProof: VrfProof, vrfOutput: VrfOutput, usedSlotNumber: ConsensusSlotNumber, generationRules: GenerationRules): SidechainBlock = {
    val parentId = generationRules.forcedParentId.getOrElse(lastBlockId)
    val timestamp = generationRules.forcedTimestamp.getOrElse{
      getTimeStampForEpochAndSlot(nextEpochNumber, usedSlotNumber) + generationRules.corruption.timestampShiftInSlots * params.consensusSecondsInSlot}

    val mainchainBlockReferences: Seq[MainchainBlockReference] = generationRules.mcReferenceIsPresent match {
      //generate at least one MC block reference
      case Some(true)  => {
        val necessaryMcBlockReference = mcRefGen.generateMainchainBlockReference(parentOpt = Some(lastMainchainBlockId), rnd = rnd, timestamp = timestamp.toInt)
        mcRefGen.generateMainchainReferences(generated = Seq(necessaryMcBlockReference), rnd = rnd, timestamp = timestamp.toInt)
      }
      //no MC shall not be generated at all
      case Some(false) => Seq()

      //truly random generated MC
      case None        => SidechainBlocksGenerator.mcRefGen.generateMainchainReferences(parentOpt = Some(lastMainchainBlockId), rnd = rnd, timestamp = timestamp.toInt)
    }

    // TODO: we need more complex cases
    val mainchainBlockReferencesData = mainchainBlockReferences.map(_.data)
    val mainchainHeaders = mainchainBlockReferences.map(_.header)

    val forgingData: SidechainForgingData =
      if (generationRules.corruption.getOtherSidechainForgingData) {
        getIncorrectPossibleForger(possibleForger).forgingData
      }
      else {
        possibleForger.forgingData
      }

    val owner: PrivateKey25519 = forgingData.key

    val forgingStake: ForgingStakeInfo = generationRules.corruption.forgingStakeCorruptionRules.map(getIncorrectForgingStake(forgingData.forgingStakeInfo, _)).getOrElse(forgingData.forgingStakeInfo)

    val forgingStakeMerklePath: MerklePath =
      if (generationRules.corruption.merklePathFromPreviousEpoch) {
        getCorruptedMerklePath(possibleForger)
      }
      else {
        possibleForger.merklePathInPrePreviousEpochOpt.get
      }

    val vrfProofInBlock: VrfProof = generationRules.corruption.forcedVrfProof.getOrElse(vrfProof)

    val sidechainTransactions: Seq[SidechainTransaction[Proposition, NoncedBox[Proposition]]] = Seq(
      SidechainBlocksGenerator.txGen.generateRegularTransaction(rnd = rnd, transactionBaseTimeStamp = timestamp, inputTransactionsSize = 1, outputTransactionsSize = 1)
    )

    val ommers: Seq[Ommer] = Seq()

    val sidechainTransactionsMerkleRootHash: Array[Byte] = SidechainBlock.calculateTransactionsMerkleRootHash(sidechainTransactions)
    val mainchainMerkleRootHash: Array[Byte] = SidechainBlock.calculateMainchainMerkleRootHash(mainchainBlockReferencesData, mainchainHeaders)
    val ommersMerkleRootHash: Array[Byte] = SidechainBlock.calculateOmmersMerkleRootHash(ommers)

    val unsignedBlockHeader = SidechainBlockHeader(
      SidechainBlock.BLOCK_VERSION,
      parentId,
      timestamp,
      forgingStake,
      forgingStakeMerklePath,
      vrfProofInBlock,
      sidechainTransactionsMerkleRootHash,
      mainchainMerkleRootHash,
      ommersMerkleRootHash,
      ommers.map(_.score).sum,
      new Signature25519(new Array[Byte](Signature25519.SIGNATURE_LENGTH)) // empty signature
    )

    val signature = owner.sign(unsignedBlockHeader.messageToSign)

    val signedBlockHeader = SidechainBlockHeader(
      SidechainBlock.BLOCK_VERSION,
      parentId,
      timestamp,
      forgingStake,
      forgingStakeMerklePath,
      vrfProofInBlock,
      sidechainTransactionsMerkleRootHash,
      mainchainMerkleRootHash,
      ommersMerkleRootHash,
      ommers.map(_.score).sum,
      signature
    )
    val generatedBlock = new SidechainBlock(
      signedBlockHeader,
      sidechainTransactions,
      mainchainBlockReferencesData,
      mainchainHeaders,
      ommers,
      companion
    )

    println(s"Generate new unique block with id ${generatedBlock.id}")

    generatedBlock
  }

  private def getIncorrectForgingStake(initialForgingStake: ForgingStakeInfo, forgingStakeCorruptionRules: ForgingStakeCorruptionRules): ForgingStakeInfo = {
    val stakeAmount: Long = initialForgingStake.stakeAmount + forgingStakeCorruptionRules.stakeAmountShift

    val blockSignProposition: PublicKey25519Proposition = if (forgingStakeCorruptionRules.blockSignPropositionChanged) {
      val propositionKeyPair: utils.Pair[Array[Byte], Array[Byte]] = Ed25519.createKeyPair(rnd.nextLong().toString.getBytes)
      val newBlockSignProposition: PublicKey25519Proposition = new PublicKey25519Proposition(propositionKeyPair.getValue)
      newBlockSignProposition
    }
    else {
      initialForgingStake.blockSignPublicKey
    }

    val vrfPubKey: VrfPublicKey = if (forgingStakeCorruptionRules.vrfPubKeyChanged) {
      var corrupted: VrfPublicKey = null

      do {
        val corruptedVrfPublicKeyBytes =
          CryptoLibProvider.vrfFunctions.generatePublicAndSecretKeys(rnd.nextLong().toString.getBytes).get(VrfFunctions.KeyType.PUBLIC)
        corrupted = new VrfPublicKey(corruptedVrfPublicKeyBytes)
        println(s"corrupt VRF public key ${BytesUtils.toHexString(initialForgingStake.vrfPublicKey.bytes)} by ${BytesUtils.toHexString(corrupted.bytes)}")
      } while (corrupted.bytes.deep == initialForgingStake.vrfPublicKey.bytes.deep)
      corrupted
    }
    else {
      initialForgingStake.vrfPublicKey
    }

    ForgingStakeInfo(blockSignProposition, vrfPubKey, stakeAmount)
  }

  private def getIncorrectPossibleForger(initialPossibleForger: PossibleForger): PossibleForger = {
    val possibleIncorrectPossibleForger = forgersSet.getRandomPossibleForger(rnd)
    if (possibleIncorrectPossibleForger.forgingData == initialPossibleForger.forgingData) {
      PossibleForger(
        SidechainForgingData.generate(rnd, rnd.nextLong()),
        Some(MerkleTreeFixture.generateRandomMerklePath(rnd.nextLong())),
        Some(MerkleTreeFixture.generateRandomMerklePath(rnd.nextLong())),
        initialPossibleForger.spentInEpochsAgoOpt
      )
    }
    else {
      possibleIncorrectPossibleForger
    }
  }

  private def getCorruptedMerklePath(possibleForger: PossibleForger): MerklePath = {
    if (possibleForger.merklePathInPreviousEpochOpt.get.bytes().sameElements(possibleForger.merklePathInPrePreviousEpochOpt.get.bytes())) {
      val incorrectPossibleForger = getIncorrectPossibleForger(possibleForger)
      incorrectPossibleForger.merklePathInPrePreviousEpochOpt.getOrElse(MerkleTreeFixture.generateRandomMerklePath(rnd.nextLong()))
    }
    else {
      possibleForger.merklePathInPreviousEpochOpt.get
    }
  }

  private def createGeneratorAndFinishedEpochInfo: (SidechainBlocksGenerator, FinishedEpochInfo) = {
    val finishedEpochId = blockIdToEpochId(lastBlockId)
    if (finishedEpochId == previousEpochId) {throw new GenerationIsNoLongerPossible()} //no generated block during whole epoch

    val (newForgers, realStakeConsensusEpochInfo) = forgersSet.finishCurrentEpoch()
    //Just to increase chance for forgers, @TODO do it as parameter
    val stakeConsensusEpochInfo = realStakeConsensusEpochInfo.copy(totalStake = realStakeConsensusEpochInfo.totalStake / 20)

    val nonceConsensusEpochInfo = calculateNewNonce()
    consensusDataStorage.addNonceConsensusEpochInfo(finishedEpochId, nonceConsensusEpochInfo)

    consensusDataStorage.addStakeConsensusEpochInfo(finishedEpochId, stakeConsensusEpochInfo)

    val newGenerator: SidechainBlocksGenerator = new SidechainBlocksGenerator(
                                                  params = params,
                                                  forgersSet = newForgers,
                                                  consensusDataStorage = consensusDataStorage,
                                                  lastBlockId = lastBlockId,
                                                  lastMainchainBlockId = lastMainchainBlockId,
                                                  nextFreeSlotNumber = intToConsensusSlotNumber(1),
                                                  nextEpochNumber = intToConsensusEpochNumber(nextEpochNumber + 1),
                                                  nextBlockNonceEpochId = blockIdToEpochId(lastBlockId),
                                                  nextBlockStakeEpochId = nextBlockNonceEpochId,
                                                  allEligibleVrfOutputs = List(),
                                                  previousEpochId = finishedEpochId,
                                                  rnd = rnd)

    (newGenerator, FinishedEpochInfo(finishedEpochId, stakeConsensusEpochInfo, nonceConsensusEpochInfo))
  }

  private def calculateNewNonce(): NonceConsensusEpochInfo = {

    val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
    allEligibleVrfOutputs.foldRight(digest){case (bytes, messageDigest) =>
      messageDigest.update(bytes)
      messageDigest
    }

    val previousNonce: Array[Byte] = consensusDataStorage.getNonceConsensusEpochInfo(previousEpochId)
      .getOrElse(throw new IllegalStateException (s"Failed to get nonce info for epoch id ${previousEpochId}"))
      .consensusNonce
    digest.update(previousNonce)

    val currentEpochNumberBytes: Array[Byte] = Ints.toByteArray(nextEpochNumber)
    digest.update(currentEpochNumberBytes)

    NonceConsensusEpochInfo(byteArrayToConsensusNonce(digest.digest()))
  }

}


object SidechainBlocksGenerator extends CompanionsFixture {
  private val companion: SidechainTransactionsCompanion = getDefaultTransactionsCompanion
  val merkleTreeSize: Int = 128
  val txGen: TransactionFixture = new TransactionFixture {}
  val mcRefGen: MainchainBlockReferenceFixture = new MainchainBlockReferenceFixture {}
  println("end SidechainBlocksGenerator object")

  def startSidechain(initialValue: Long, seed: Long, params: NetworkParams): (NetworkParams, SidechainBlock, SidechainBlocksGenerator, SidechainForgingData, FinishedEpochInfo) = {
    println("startSidechain")

    val random: Random = new Random(seed)


    val genesisSidechainForgingData: SidechainForgingData = buildGenesisSidechainForgingData(initialValue, seed)

    val vrfProof = VrfGenerator.generateProof(seed) //no VRF proof checking for genesis block!

    val genesisMerkleTree: MerkleTree = buildGenesisMerkleTree(genesisSidechainForgingData.forgingStakeInfo)
    val merklePathForGenesisSidechainForgingData: MerklePath = genesisMerkleTree.getMerklePathForLeaf(0)
    val possibleForger: PossibleForger = PossibleForger(
      forgingData = genesisSidechainForgingData,
      merklePathInPreviousEpochOpt = Some(merklePathForGenesisSidechainForgingData),
      merklePathInPrePreviousEpochOpt = Some(merklePathForGenesisSidechainForgingData),
      spentInEpochsAgoOpt = None)

    val genesisSidechainBlock: SidechainBlock = generateGenesisSidechainBlock(params, possibleForger.forgingData, vrfProof, merklePathForGenesisSidechainForgingData)
    val networkParams = generateNetworkParams(genesisSidechainBlock, params, random)

    val nonceInfo = ConsensusDataProvider.calculateNonceForGenesisBlock(networkParams)
    val stakeInfo =
      StakeConsensusEpochInfo(genesisMerkleTree.rootHash(), possibleForger.forgingData.forgingStakeInfo.stakeAmount)
    val consensusDataStorage = createConsensusDataStorage(genesisSidechainBlock.id, nonceInfo, stakeInfo)


    val genesisGenerator = new SidechainBlocksGenerator(
      params = networkParams,
      forgersSet = new PossibleForgersSet(Set(possibleForger)),
      consensusDataStorage = consensusDataStorage,
      lastBlockId = genesisSidechainBlock.id,
      lastMainchainBlockId = genesisSidechainBlock.mainchainBlockReferencesData.last.headerHash,
      nextFreeSlotNumber = intToConsensusSlotNumber(consensusSlotNumber = 1),
      nextEpochNumber = intToConsensusEpochNumber(2),
      nextBlockNonceEpochId = blockIdToEpochId(genesisSidechainBlock.id),
      nextBlockStakeEpochId = blockIdToEpochId(genesisSidechainBlock.id),
      allEligibleVrfOutputs = List(),
      previousEpochId = blockIdToEpochId(genesisSidechainBlock.id),
      rnd = random)

    (networkParams, genesisSidechainBlock, genesisGenerator, possibleForger.forgingData, FinishedEpochInfo(blockIdToEpochId(genesisSidechainBlock.id), stakeInfo, nonceInfo))
  }

  private def buildGenesisSidechainForgingData(initialValue: Long, seed: Long): SidechainForgingData = {
    val key = PrivateKey25519Creator.getInstance().generateSecret(seed.toString.getBytes)
    val value = initialValue
    val vrfSecretKey = VrfKeyGenerator.getInstance().generateSecret(seed.toString.getBytes)
    val vrfPublicKey = vrfSecretKey.publicImage()

    val forgerBoxData = new ForgerBoxData(key.publicImage(), value, key.publicImage(), vrfPublicKey)

    val nonce = 42L

    val forgerBox = forgerBoxData.getBox(nonce)
    val forgingStake = ForgingStakeInfo(forgerBox.blockSignProposition(), forgerBox.vrfPubKey(), forgerBox.value())

    SidechainForgingData(key, forgingStake, vrfSecretKey)
  }

  private def buildGenesisMerkleTree(genesisForgingStakeInfo: ForgingStakeInfo): MerkleTree = {
    val leave: Array[Byte] = genesisForgingStakeInfo.hash

    val initialLeaves = Seq.fill(merkleTreeSize)(leave)
    MerkleTree.createMerkleTree(initialLeaves.asJava)
  }

  private def generateGenesisSidechainBlock(params: NetworkParams, forgingData: SidechainForgingData, vrfProof: VrfProof, merklePath: MerklePath): SidechainBlock = {
    val parentId = bytesToId(new Array[Byte](32))
    val timestamp = if (params.sidechainGenesisBlockTimestamp == 0) {
      Instant.now.getEpochSecond - (params.consensusSecondsInSlot * params.consensusSlotsInEpoch * 100)
    }
    else {
      params.sidechainGenesisBlockTimestamp
    }

    val mainchainBlockReferences = getHardcodedMainchainBlockReferencesWithSidechainCreation
    val mainchainBlockReferencesData = mainchainBlockReferences.map(_.data)
    val mainchainHeaders = mainchainBlockReferences.map(_.header)

    val sidechainTransactionsMerkleRootHash: Array[Byte] = SidechainBlock.calculateTransactionsMerkleRootHash(Seq())
    val mainchainMerkleRootHash: Array[Byte] = SidechainBlock.calculateMainchainMerkleRootHash(mainchainBlockReferencesData, mainchainHeaders)
    val ommersMerkleRootHash: Array[Byte] = SidechainBlock.calculateOmmersMerkleRootHash(Seq())

    val owner: PrivateKey25519 = forgingData.key
    val forgingStake = forgingData.forgingStakeInfo


    val unsignedBlockHeader = SidechainBlockHeader(
      SidechainBlock.BLOCK_VERSION,
      parentId,
      timestamp,
      forgingStake,
      merklePath,
      vrfProof,
      sidechainTransactionsMerkleRootHash,
      mainchainMerkleRootHash,
      ommersMerkleRootHash,
      0,
      new Signature25519(new Array[Byte](Signature25519.SIGNATURE_LENGTH)) // empty signature
    )

    val signature = owner.sign(unsignedBlockHeader.messageToSign)

    val signedBlockHeader = SidechainBlockHeader(
      SidechainBlock.BLOCK_VERSION,
      parentId,
      timestamp,
      forgingStake,
      merklePath,
      vrfProof,
      sidechainTransactionsMerkleRootHash,
      mainchainMerkleRootHash,
      ommersMerkleRootHash,
      0,
      signature
    )

    new SidechainBlock(
      signedBlockHeader,
      Seq(),
      mainchainBlockReferencesData,
      mainchainHeaders,
      Seq(),
      companion
    )
  }

  private def createConsensusDataStorage(genesisBlockId: Block.BlockId, nonceInfo: NonceConsensusEpochInfo, stakeInfo: StakeConsensusEpochInfo): ConsensusDataStorage = {
    val consensusDataStorage = new ConsensusDataStorage(new InMemoryStorageAdapter())

    consensusDataStorage.addNonceConsensusEpochInfo(blockIdToEpochId(genesisBlockId), nonceInfo)
    consensusDataStorage.addStakeConsensusEpochInfo(blockIdToEpochId(genesisBlockId), stakeInfo)
    consensusDataStorage
  }

  private def generateNetworkParams(genesisSidechainBlock: SidechainBlock, params: NetworkParams, random: Random): NetworkParams = {
    val mainchainHeight = Math.abs(random.nextInt() % 1024)

    new NetworkParams {
      override val EquihashN: Int = params.EquihashN
      override val EquihashK: Int = params.EquihashK
      override val EquihashVarIntLength: Int = params.EquihashVarIntLength
      override val EquihashSolutionLength: Int = params.EquihashSolutionLength

      override val powLimit: BigInteger = params.powLimit
      override val nPowAveragingWindow: Int = params.nPowAveragingWindow
      override val nPowMaxAdjustDown: Int = params.nPowMaxAdjustDown
      override val nPowMaxAdjustUp: Int = params.nPowMaxAdjustUp
      override val nPowTargetSpacing: Int = params.nPowTargetSpacing

      override val sidechainId: Array[Byte] = params.sidechainId
      override val sidechainGenesisBlockId: ModifierId = genesisSidechainBlock.id
      override val genesisMainchainBlockHash: Array[Byte] = params.genesisMainchainBlockHash
      override val parentHashOfGenesisMainchainBlock: Array[Byte] = params.parentHashOfGenesisMainchainBlock
      override val genesisPoWData: Seq[(Int, Int)] = params.genesisPoWData
      override val mainchainCreationBlockHeight: Int = genesisSidechainBlock.mainchainBlockReferencesData.size + mainchainHeight
      override val sidechainGenesisBlockTimestamp: Block.Timestamp = genesisSidechainBlock.timestamp
      override val withdrawalEpochLength: Int = params.withdrawalEpochLength
      override val consensusSecondsInSlot: Int = params.consensusSecondsInSlot
      override val consensusSlotsInEpoch: Int = params.consensusSlotsInEpoch
      override val signersPublicKeys: Seq[SchnorrProposition] = params.signersPublicKeys
      override val signersThreshold: Int = params.signersThreshold
      override val provingKeyFilePath: String = params.provingKeyFilePath
      override val verificationKeyFilePath: String = params.verificationKeyFilePath
      override val calculatedSysDataConstant: Array[Byte] = Array() //calculate if we need for some reason that data
    }
  }

  private val getHardcodedMainchainBlockReferencesWithSidechainCreation: Seq[MainchainBlockReference] = {
    /* shall we somehow check time leap between mainchain block time creation and genesis sidechain block creation times? */
    /* @TODO sidechain creation mainchain block shall also be generated, not hardcoded */
    // Genesis MC block hex was created in zendoo MC branch ScCommitmentTree_to_cryptolib on 16.10.2020
    val mcBlockHex: String = "03000000bd91ce50d4770963445ea8f3080c6f43db253d541c775daf02b64d91e22c8f0b6c237f26ce658bf5deb539bb21e93d3702a45ecd00ab0c9091669b45704f3efd74981fc7e1d01e967fa55f55b396322400a366675d301494081a7ad7db3aff5fc099895f030f0f202400ff72127a7d5680168b243c14e61fb6e50ff8f00191a1e233eb9c4e2500002401354eea10b8c8343b0a58cb2628dca399d904669290221230caed2baed8b0dcf74363c80201000000010000000000000000000000000000000000000000000000000000000000000000ffffffff0502dc000101ffffffff04493eb42c000000001976a914b8879a3ace16e0a004c2d13e5113f23021d67ce388ac80b2e60e0000000017a914b6863b182a52745bf6d5fb190139a2aa876c08f587405973070000000017a914b6863b182a52745bf6d5fb190139a2aa876c08f587405973070000000017a914b6863b182a52745bf6d5fb190139a2aa876c08f58700000000fcffffff3647e98ae67283f3f6e7a6281bdc7d83ab746f98ecc4f55fc96476f1bdb8c5015a000000006a47304402203695eaa1135079dfde99f2b3dce39cb7dc17d877de19d43bf47cb0a1e61602b20220223e0ec1ace79bce583f83174aaae8b602acbd242e8cd3b1589b0ce969533e1c0121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff6ff2db229bf68df1c133f1b50f493e54b2483476ee59a93515af380da8b4855d000000006a473044022078a237a2d81c1799b5cf4c1da64b925882f172c82c0c957557bbac9c509fcc690220037ad8dfa657a011dc2c4a35c513c6fad03bcfa640f2a85f54aa667766edc8570121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff5a6a25fbc0340bb149fada1a5589b369b97797485dca57888777878ffcf749ba000000006a47304402202399ad7784ee4eb613f7e1504a07da9cb6b292e950ffee8a96b88368d349ab3002200bef5984cf1f2b27c41ef4703188a2925b6f6feaf2cdaee9e8d44321d3f568360121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffffcf0872a89cd3bdcdc397c7e12180f5a6145610898ad6554e406a7af3771ca77f000000006a4730440220026dc74d1b533dab154528852f8bfa6d2a968699d27af5760aa3b653b769e3e0022008d244d57f69e7118a8a2ace48e5bb4244023b4fd1a2775f054948f4bf5ae0330121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffffe710d3a1668d59c368dac2571da121659d4701489d61c51a0841bf2865487731000000006a473044022038f52dc6d9f073929991584ef2e9e6bf42e186f990b6616de0c73112ce2915ce02206c55a41fe64a748a9148a2535c6d74a3437dcca49c72b24d8a0a6fbea0ad36630121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffffeb0ecf0bc03061e5965a7682ac0c54da1b6226724a8010cf33d1c9f6c4702d9f000000006a4730440220721a3fc0c9c37fdaa9ce6bef272baf74432785fc3afefbcea475cb30b21be8940220084b1cba2c019e264270b86109aee13f59b7204aa75410d9ab6bf71fb3735b8e0121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff8262ea7b71781491c5629fbcb358a2063ecf4dd2ec8b8b2d456d09fef6ee212a000000006a47304402202790c96af3444a40fbc6d85e00921e0de88580e4c6470027970f19456d8f73a002201c93d7a77a201a503efc7545b9c81e8428609fbf25ef5fbb755ab895927a0c7b0121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff1f654182b4b007dfd38724e386ab1a1a4449c334c2e176236950c3a11b3c7672000000006b483045022100b6e8bc431037ccc018bbd95c2806b5ed90c7d1ef7e837fea0dfc9af1675622050220586dadc8e5fb475ef7edaf044ab5b2b88fbe202a815b6ee9ea58bf19c40e394c0121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff3021e50b49a3dce16faea8b1d7be70fce7ae58bce92ef15bcc2882a933480868000000006a47304402201ad773e0bc02c8bb242f2fdb0680836890af620541aea62723424b008876fcd102203bb1e7e89d5651b74c867507e03e0dc53929c7500476f8adca61b389494e6f6e0121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff7eeca8b33f977aec02667fd8968ced13d68969dad6e11e02a8798b47955c455f000000006a47304402200ba548f49974d208c07970e24943dbcf16135126f424d27d9b8b07b44279b3de022017230e22dc4096c2e558be16aab504155214d579ba62cda7c3ca7cdc0c826fd00121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff8b3257a0fc753f482b379be10c4114caca11d150cb15529ed1ed7f46e7cc937b000000006a47304402201915333a1f644d9e2a733f1104076bcf2966b6733868f15b3fa86c5359fd044502207750cc4bb0f2deae480682821aeb55c45eafc1244117bd4d32027cca7f23f2e90121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff90ec64c04d27fdb446eeb53e589da4dc8c242428b3664fa718125c5571aca77a000000006b483045022100f21352ad9eb95bd4b7bb6045aa70f1f5367880de6278af5299ff286ed364671202201659cbca152fa3c04102129df50db1cf6cbc1873e2c30f4f2e133cdcdbf0837c0121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff46701cb1c56c63a24dbdd886f8a3379e9bac17df86d0c653475056ddbe7bde2e000000006b483045022100c2c28f4122063efb5b15cc049d549b32f21d3e2dddd965220c338be4beefea5c02200d0f28fc0ba15d053bf7622854415b5ff61a69f0824ef91a965a4e29eb9462820121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffffa12c83d32617c97bc80a44cad8e3e5971f84173e614c19cb68c14902051b3622000000006b48304502210086ab64a7bbc1c79d395b582556cf3396a9e64b8e95f2e8abf3098dc5869051ff0220212a66ec5bed11185c18d5b024b64b2040398594a93b12a64e2ad63ebb6fbd040121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff1956e0f30019831f05c94901289650998bec4d903ec08265e9a01339a6f96fd3000000006b483045022100d51c096525b6e376c2d89a4a3cc94197ea3012310aa1aeede82d8eec54e2fd8902206c5c76ae79d70498a65e4677de3cc7379618ff9f81523498635c2e5aaf7578e50121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff50c04c535360ea00949aebe9c7650857579e70330860e2c00d87154b18c7fef3000000006a473044022040089dd5684efe8247060a5340fd40ee25ceae55d1146fff75293e0ca38b6b9502207613786600005cff939354fe93a6901e06991b07237b8356f65d747a3ac7fead0121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffefffffffa4eaef5a3707dbbba5f681c81869e441648c41e285f43985715fb0cfd094829000000006b483045022100a32e120b6ffd55fcc26c69288b5f2b58a1b2fae11d4a4df593bac97cf7f2cfac02202c6a4edaf2f664fa286a53bc59af5c648bce0d3825338c57dde41d857d7338e40121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffffb087142c43212372714dcb13fe60b2922eb333fdd625f99962254a373eae05f7000000006b483045022100a8264ef7f52c21c990eb5a970ea3f33543363327bfe899ed123f1772883f85ba0220415a6c2c86dc72a7b751170cbef7d23898c30db1b48a6cfb52847c3f7f57d41d0121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffffd324c0dcc16481a9b93808f09feff982f42505dca4160f16d5c8478ea0de4cee000000006b483045022100970d3c807a54f1f1d4eb388f7b8bb3930c301de99e3c97728debb2012b640c44022027c3ebdf6411a3e5bc774c5aecc7fa5b45ea1b2af66ed9439c38d8ba349715730121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffffa4a6ccf67acb62ad5e45c36533a06fce67e7135894cbe76521408d748e5a4431000000006a473044022039787a2d7c6786ad269592bd510f288451353b1a46f9c726e5699b3cce47231202203fdb84caadf9fb75919b168d92c037cfa0cc799fa3fb9e16cb88f44c055416b70121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff7dae7316916c9ab1ad31bb81dd8a3ece4c4ea8a8113658f6852e13d371b68fc5000000006b483045022100902665cf320d608b6c7cd94f225180cca821eb0385f18f2f0d78e3b8e4e2079102200832208c75790f8f1a81f36d74a3893d564923ba7fae91b1c3e9f20938fc52970121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffffa0136a1ef2ee398e74390a6eabc54880002eb49b8af5d99b56e8814ec4327ab2000000006a47304402201b1d11fa809ca080f640f632193964b1190e30839be196e6e59781bca0145e11022046c2efcead061bd2bb0754ffa8a5a6131076e35c98a1a7b6a5e41cdf6c0162540121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffffe9dd8a2e584ff7e0f61ba752c0b80aba2b5bfaf0db42b1135e074f973ef0bd38000000006b483045022100b783b3e42226f8c27fc71681f44aeac207dea9f45f8a34e8298f1201e9612c50022055e4aa9b346f778b129f901aa7273f143416f0e5e23f621a0d3fc63e3a2d24e40121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffffd87a38faf181a9602fd140378e48ef6f6a821e47ee561963865c7fa00669e301000000006a47304402201bee95d7483239379b3d1d9d31aa8e6b3957d000a58c5a6678cd70125133068a0220348428d607e503b6a7abd2afc06d2103cb7df81d3dfce78ff8e69cfc4400a59f0121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffffc4019902714d2eebd4bc83fc38edfdeff6dde38aa4a5f80748bb0bdb3060471c000000006a473044022044dd5ef903ac72a9cdc0e2fdc43d3113f906c8c33a5e7ec981e197490f295963022073354d7fdbd10413fb3bf61954fc33c0910b51fb3f7f9f44205f03e3c0c2b8ef0121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff5037b0b27b0789138eb63a1b1ba59267dfc92020c8831e3ef8bcb8c36d2ddf88000000006b483045022100c76c563d314196868514b6047391f24d03f3a9de8a5421f467feff55090097530220557711159cb9eb0766d4b1a5b566eb7728793346c128a9f496f239e4000e35e50121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff1e7bf5644d3e25251ee1fdf88020392b83b77fe0b3cc27942602e1bc7a69c683000000006b483045022100b37288abb94c5dc2a7e1307604f2e75be3a4dd26adea3bda7139e4340715b9c002203d445aa25575afb728a145024e5cc8b7711b1497c02a1bf94eedccf20fb3749a0121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff52f902641f6732fee3290549ad19eaaa74bd148c5706f64738db8b19c006a0e7000000006b483045022100ad32b8dd782ea0eb63b15bae6d6fa199b3fb49c7cf14688daefec51f7d93ab7002204b8782751c5bdcd96c25d0f435a27a85f80bd79e75ae9b06020c9dc088d5ee670121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff764ed8147dae48e2151d8470dfdef47d99a9e5b80fffdd49b063b45b04c93cc6000000006b483045022100bb5a5f3d2afd42ae77269ba831d7a3e58f5835edf30b25577c1790bb16045c6a02205cb46e15820a36dd65e2adc32ccf2fc6137e4e4ccfede6170fa880cb87cc33500121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffffa081df1621d26d5dad16648972f5598c73efb6bdd20c7842baa3eef20e25763f000000006b483045022100f4719fbc0e8289f81392166152945849d630a2ac09e2ec12c5dcecbfdb96a9a402200daef15fefffb973a95d2c3988f1421526ce0fb6d713fbe7a69b5f4fec0372910121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffffc1baa357127f483627b503a647fca5b125dd7cf488bd16ac0ae67f028d93d8a4000000006b4830450221009e5004933605b1bdf450bf5c3cdb29c5fb3019285595a8a7156c0b47bf8050cc022073905640f577860a96092c03d5c79d6e02eaec4637c93afd910496a9301de3ef0121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff07ca1b522cf2ca507c4feb45e65ad7614b452f6dacdc2026cdc1d110637bb712000000006a4730440220224123cc38773ac9f3e5ce20009987ea6b644d52de2c899fa73e0dc1c83066e802205edb31303f68c30aa6eec2bb284dd8247f98305bebe2b640a4dd5bb2f8a82b470121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff20eea48314b7bcad81ad0e54f1cd18c218743f27ea933beba5f848f4029d866e000000006b483045022100ff7f8d75ec6b229e20c8e55df184728ba5172962da9d22f8a3510e6d033cd44a022003d196a1bd5e0bdae6d548269636118ceeb8119b2bd0cb9679fb9548b4b6732e0121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff9b8e1df7edc09ac18160ebae1a608a7935f20c2d61b9ce1ffa38f489cafa3670000000006a47304402204b3f31a6f6336ec3f8fc7796c876b0a5ddda2f55596692f1bd43ffb79186a50202205e6ed9a5d339f6ce9b76365991ad40e5410373aec18b765499ae426cc15d335f0121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffffdc0973772359bbd903b129450ed8523cc902e803b3285d554a7855a24a9dff55000000006b483045022100f6f9a2b1d73636459efcb36c76406fed4d7a218fd1f1f30d49bb01a46581933d0220144de2c3bb11ef3407dc8a97bdc1fc90a8e235c1f582e5458a88ddb8825a86670121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff074f5be7ce379fba7e1be54fe78b785a3443c0de37e1bd150da2e6e3392437a3000000006b483045022100e3595b3a7b1cc7520d969f5f65aeb372b0731f60f62c8d43f0707f9ec2ad0af102201f98b4e549ae1a811b96e23511273e79111354c6cf0cd2c4d931ef59dca659370121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffffaf0c71207d68309af62792565c06020993d805ebbdc047164927461fe309d1ae000000006b483045022100d71341e99700f0aa6791eab16de42878d04603f0bf57e9bde4660582f948fe2b02205202bbe7918159d03d8b273098284ee4160c5ef7feca86040b5e50bf98e4274b0121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff3d0ca535bc26cab301736009d962ab875875d32b9a2fafb0afa20cb5c062bee7000000006a473044022003f060b36663701fd3906513531936fd47f631e3123093a7daddef75680daef80220797413fc7efb0b6783c1ee94d6fd450b0a5e3b51f3e862fc44436ad7db2c15300121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff268e6a84975fc27dc076eacb6cb749657ddc1c0dffcf6bbe9b8616e25b8b712c000000006b483045022100f83f85b767194dbcae632b2c24b5d1c41a757e16afb1f0bc203ae12afb24340502200b60e8010306e767b63d5ebc340922baf24bc898f30d5d44d639f383d563b61a0121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff360905fd7ab9e8f61f38ce6d0f2815422e205465592ea8be5061551321c7c97d000000006b483045022100f3cb5021a71b041d56f75727cc52b0670432dfc088c67e4d96bd47b5bd82f2a902203239739d8aa17c11bb74a46dd936a6e5b7bc6883c850d52cf2bbaf1bdf4d08d10121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffffefb956ed14a2da21a9e8ee9a972c2e281d6cb585fe57373328d199cbe9425586000000006b483045022100df6eb8c15dbb2ae7a3c77697b0405e2f4b277c8c0de41454240f1582cee20c260220723e432fe3705484e24ba8b6d783eac37f5e6d13bdfe394cb69882b3d4270c630121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff50bfefaf4e9322137744ade91daf5e19c4a93f7d74b3e2805251faff14cd187f000000006a47304402202e807597fa9a5556fde28c1326b8a2562e69452365bb6ea4f1049313a868493002204eaaf9cd5b3e682b5d4dfb740a7ba52ab3733e26eef3c49496f0b97e1b8fecef0121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffffb1ebcb57beec7d31fde0fb56a6eefc406a668637dd6f490fec79372657028a54000000006b4830450221009069305cbb7244a76f1683d791bae5570bd419859e50f71ed9b0e746032cf3dd02203ecf6ef5c4beae268599f05dd098f0021b8cca2b3bd80dd0bb47e313b110c23c0121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffffc7d6389e2eed91919db8cd883df213341f41bb80358537f9485522337121eb03000000006a473044022049d6c99e1bdcd4972c2fb5a289d33e9f7c61de015924787ae63c0da481da951d022016eada8d8aa562dd2ecf8582d7a28581d255f98c7272211efb30313ec97440f90121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff3d4e3fada396a1cff8d215330746711317ff908d3df2435a8a52d699dfadfe41000000006b483045022100ab7ef94fd9a928464df291e1d02288f964afb1f151bfa1e6664e9639c560b1a8022069b82596fb23186c3cb590630d5b56beb80510c3b64f0ee24310347af4bf3ef40121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff2e8a64d46a8ab1f9d4a298d2506613bdb8b8e0469833eda18ec6aa8e7ea441b7000000006b483045022100c3469d10f316c9b685559e5c191b8d9cf20b325fa4668a02c73f242726b226aa02205007813c345376899b850c7064ddc7dd87edb6ed01587e4485c0756c0ecec3a50121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff1583ab37fee162e7bd0f37fb04322b07df2c8a8c625749f142790faa45d064db000000006a47304402204b76d19bd7939bc02d8ef008258a78d3ebfef44813db00bbf1801a277f24b55202202c01c89447ff4e20f5c99a583c2454ddc1bc731c86c2178dd8695a3b053bdad80121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff7adfea537c588c4ee10d9483fc1cc22a5ad7dd3106103b61dfe38334e301e401000000006b483045022100ae9912e3f66477d59f9f838318fe83c68c6523299c9ba53119c9d04c0244931102202d6af7636d11bf97187e693c5e8c0dfac37e6a2e6010ec973cd7e1d982fbb7d30121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffffb03e2b70724be6cc78bf1910d1959fb0e132f2fc2ca34c8404f3ac86f1a35ae1000000006b483045022100b76d63fb95e8afba302ad5436db858baa4f9f80f821ee0181d3be886bc0c965d022064a00025a536e8b6c220d6fa117941edeab5ebd0d2d40cb05a166d97c01639620121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffffe566ee702e0e4e81a7b284ab05e24d5cb4bdb48fba854ca65f749a222f4f874e000000006a4730440220647687f504bccb9715f3258e50097d4f68baec802361a4a17097acb303a5e426022044858e767d845c614a80fe217ca27aa4ce2eb6262cc3aea59a37573aab13c8830121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffffe1a8fdccf166c30e09b7d68d28f320b6a6b1248125f79e155312ce0c2021e698000000006b483045022100d9b2de3d67604456ad1bcb405f794ef739ab67cb439409ad7cf3da7641a1d30e02203f485bd95862360069ce769298a508bdfc5710e3c04de75883626cbfd2f50ae60121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff465a896d352ce11d5830e4b55b3fced7c6fdace58a2ac6ce1f51128d757a53c0000000006b483045022100e6dee023bc95381a0d2aa5b22ccb79c872cac7a8d086e4c7a71e26b55267ae6702206149fae2536005b954374cfdb22f8ba918a5dfdc2b106126a7417f790c2918510121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff9bd023443e992c760a5673dee74851d957abea16ba5bec9fd643859e18f64313000000006a4730440220767b4d8eb88f36e34679b813ce1a3f1a92b9abd4f341a2aa9f5c790433513eef02206852e81972631b5a19b2db204ea7b6deffbccbe3ff7c005338edc7ea5c0e0ef00121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffffab0d638881a96842f5135c10ec335328cef7a25c20d99f8c9e5a991667cc4386000000006a4730440220649c6a07cfb9c5e4d226dff2885d5f063c8a658f62ae49811604b2277f65f3cb02203ddc88b630e8be3fe99a3208434e3933a7acab95b61aae558a44387afac522170121022afc31323cd4e2c5a0b95fe5fd8709856b1bc858b58ef31e26e3ee38b88c39fffeffffff0167f31d01000000003c76a914f15557f71954005fc400590b90dd21b693920fec88ac20bb1acf2c1fc1228967a611c7db30632098f0c641855180b5fe23793b72eea50d00b401e8030000005847f80d000000acb24b2f08e8e56fe1784f1600879c697c7cd90846e076724b090fd72206b1a5c167504d6d0f6771859935cbe8d1209b9e6f65148f8555203334c9c67f9dae381a7c6be16a1e99b90d50e61baeabcffefafaaaba1225915ef890400480ce238b62d2c47b6d2c35dc891f8ea05ce91a40a17670c9e7ec884ef9b3ff31cc8d0a0100c8c05d8eef4511ba6f12a3c2214a79b1e054e95b66c3b99b8e444d1f72bf271f1b2d9b6ca43d12e208325be7bf241253eb1ec066183d9f3779dc4d45a1a40499a991759fe4763ef7ad7502a3784ee41816cf1e5c59da785a7bce465f2ac4010000602cbc02a0126642bb9a84892ecc45cdf0e8ddde1acf54b3c03da503df850ea95a8bbd7b1023f4c28d25f948943e6fd1ac7a8ff7e1d47a9d78dad3daac48aba83a440261a651611b9e9aaaa95f875ccbad6e8d13b956b175aedd9817b5545900005e7b462cc84ae0faaa5884bd5c4a5a5edf13db210599aeeb4d273c0f5f32967b7071ce2b4d490b9f08f6ce66a8405735c79197cd6773d1c5aeb2a38da1c102df07b05879c77198e5aafa7feed25d4137e86b3d98d9edd9547a460f1615b10000ee9570fbffedd44170477b37500a0a1cb3f94b6361f10f8a68c4075fbc17542d7174b3d95e12ddb8aea5d6b6c53c1df6c8f60010cd2e69902ba5e89e86747569463a23254730fc8d2aabf39648a505df9dcce461443b181ef3eda46074070000550836db2c97820971db6b1421e348d946ed4d3f255295abea46556615e3123de33ec56f784f70302901a4bc10c79c6a8b1e32477aeff9fba75876592981b678fc5a2703ac0b3055e567a6cb1ebab578fc4f9121fd968680250696cb85790000078fcfb60bdfc79aa1e377cb120480538e0236156f23129a88824ca5a1d77e371e5e98a16e6f32087c91aa02a4f5e00e412e515c3b678f6535141203c6886c637b626a2ada4062d037503359a680979091c68941a307db6e4ed8bc49d21b00002f0e6f88fb69309873fdefb015569e5511fb5399295204876543d065d177bf36ab79183a7c5e504b50691bc5b4ed0293324cfe2555d3fc8e39485822a90a91afcd4ef79ec3aefbd4cbe25cbccd802d8334ce1dce238c3f7505330a14615500001f89fbe1922ab3aa31a28fd29e19673714a7e48050dee59859d68345bb7bee7d5e888d8b798a58d7c650f9138304c05a92b668294c6114185ccb2c67ce0bbbb7e1dcbb6d76f5cacd7c9732a33b21d69bd7a28c9cca68b5735d50413862bc0100308bb0dd0bd53f3d1134966702dd3c7cc8b58b270a6996a646493250b0d5f3978d0c971f8fa7a0c958f3efe2fa5269244973fafb701c2eb66dd25901f93d677ab6c538c1ed11f115e52d3f2c7087ea40c3e8cd089376baa38842e9429b5f0000d19a8d874d791f952f13d3c8ecd92e44009c09815e5ae6a8e5def7ea52fe3de4accfb5ba2aa401fbcec14b069cd0dc0f66ab025b45ef9831a26acf58673db7487043654e7980fcb2b6c1bd7593a4dfff810436f653e309121c7ccf2df70b010000732254ec6df184be360cd9ed383ed7c8c236d7761cfc0ce4e7f0cac5a06f4edab9cfc75a7dc1449c0e18ed9564c974c2e1b6847c637f74e5d391cbc80fc6e672ffd66b5ce4fb73bda8359ab8a0ea1e855df1e07d82f93c935c7e1a9a55c5000065efdbb7c3e82291a482b2f24cbd46f4dd02c370cf6dcfe8fb3c00b8b004b5ad51369b1f1b134a824d1f16d72ca6a27ba2d6190150329139cf2c6d9e5a14722f8d39b96b882c1f60a7b230e929819e2abe1cd9d7f3e8c726b1a94d20c8010100732c396eca6ffa1bf851cef449f2f087edd93e4f641b4bd93a482d9f129e675aedb688993d4e2cee824d2803301364ba10fbb66895927adb53bad8aefe8a1caab6f4ccb45883e414a1223ac7f90a89087cd752dfa0c7b3e19bbae000edd5000028d1d23c627d1252d2a2a20a246af2280f50e3fde667873aadd9893ba6833118358398e7428e717128f764714a8d52b090c1f554f58e25ea815338d7bc7326c949567e74f2f2ab3c88f5075fea75594608b8937c9059a42d712ffbd1bd980100000000000250c1a474689e375a309446e5cdd3a0c26cecdcff5c7b8cdc0728868983f1a35a49e3a1bae6f969c3d47356c08d3d169d2c0a2be908d82cd35f41a23d8c2924a9f790ab3a00d53061d440a176670d6a32de2ecd19cf8a9774729c09a6ea4d0100d8838bf55d95521291da12294b302c66042eda0dc2acc79360a1fdd8c9a366fa790c52bf926c2d96b5ba88a3a443487c5235f7c476f350c2101cfbe3bd0361dd291ebc5e42c097a158704b71006886a3662ca6db7d816b4ad12444835d89000000795ce2b34aef921ccb3d9b9695f5d3fe0a03743c955cfcf01f8a1815a7c8b03de85fe15201d4b4b6f401cb334a6988ea5bde8986a468c47c3c6a5ae96a3160ff15e06699ea82bd40c0d5547fe1be77af7817861bbfcca3f4232f05a9cec800006c216565cee4d57b32d2d70bb3cb8d4a967c0eb5d7137b2ec58466f3d4d3b5375e4baa823bcc29c6ad877d9708cd5dc1c31fa3883a80710431110c4aa22e97b67fa639f54e86cfab87187011270139df7873bed12f6fb8cd9ab48f38933801000000d100000000"
    val params: NetworkParams = RegTestParams(sidechainId = BytesUtils.fromHexString("0637b53e1810ba0fb89b0fd6408886ea91f7ad4cc1b624ec8f0830f84dc86ac6"))
    val mcRef: MainchainBlockReference = MainchainBlockReference.create(BytesUtils.fromHexString(mcBlockHex), params).get
    Seq(mcRef)
  }
}