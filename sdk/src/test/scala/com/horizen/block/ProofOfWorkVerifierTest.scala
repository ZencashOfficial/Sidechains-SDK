package com.horizen.block

import java.math.BigInteger

import com.google.common.primitives.UnsignedInts
import com.horizen.fixtures.{MainchainHeaderFixture, MainchainHeaderForPoWTest}
import com.horizen.params.{MainNetParams, NetworkParams}
import com.horizen.storage.SidechainBlocks
import com.horizen.utils.{BytesUtils, Utils}
import org.junit.Assert.{assertEquals, assertFalse, assertTrue}
import org.junit.Test
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.junit.JUnitSuite
import org.scalatest.mockito._
import scorex.util.ModifierId

import scala.collection.mutable.ListBuffer

class ProofOfWorkVerifierTest extends JUnitSuite with MainchainHeaderFixture with MockitoSugar {

  val params = MainNetParams()

  @Test
  def ProofOfWorkVerifierTest_CheckPoW(): Unit = {
    var hash: Array[Byte] = null
    var bits: Int = 0

    // Test 1: Test valid Horizen block #498971
    hash = BytesUtils.fromHexString("00000000117c360186cfea085c6d15c176118a7778ed56733084084133790fe7")
    bits = 0x1c21c09e
    assertTrue("Proof of Work expected to be Valid.", ProofOfWorkVerifier.checkProofOfWork(getHeaderWithPoW(bits, hash), params))


    // Test 2: Test valid Horizen block #238971
    hash = BytesUtils.fromHexString("00000000c1dfa2e554343822f6a6fb70b622ecbd2ae766eb4d61e792f37d46bd")
    bits = 0x1d012dc4
    assertTrue("Proof of Work expected to be Valid.", ProofOfWorkVerifier.checkProofOfWork(getHeaderWithPoW(bits, hash), params))


    // Test 3: Test invalid PoW: bits (target) is lower than hash target
    hash = BytesUtils.fromHexString("00000000c1dfa2e554343822f6a6fb70b622ecbd2ae766eb4d61e792f37d46bd")
    bits = Utils.encodeCompactBits(new BigInteger(1, hash)).toInt // it will cut some part of data, so value will be less than hash target
    assertFalse("Proof of Work expected to be Invalid.", ProofOfWorkVerifier.checkProofOfWork(getHeaderWithPoW(bits, hash), params))


    // Test 4: Test invalid PoW: bits (target) is greater than powLimit
    hash = BytesUtils.fromHexString("00000000c1dfa2e554343822f6a6fb70b622ecbd2ae766eb4d61e792f37d46bd")
    bits = Utils.encodeCompactBits(params.powLimit.add(BigInteger.ONE)).toInt
    assertFalse("Proof of Work expected to be Invalid.", ProofOfWorkVerifier.checkProofOfWork(getHeaderWithPoW(bits, hash), params))
  }

  @Test
  def ProofOfWorkVerifierTest_CalculateNextWorkRequired(): Unit = {
    var nLastRetargetTime: Int = 0
    var nThisTime: Int = 0
    var bitsAvg: BigInteger = null

    var expectedWork: Int = 0
    var calculatedWork: Int = 0

    // Test 1: Test calculation of next difficulty target with no constraints applying
    nLastRetargetTime = 1262149169 // NOTE: Not an actual block time
    nThisTime = 1262152739 // Block #32255 of Bitcoin
    bitsAvg = Utils.decodeCompactBits(UnsignedInts.toLong(0x1d00ffff))
    expectedWork = 0x1d011998

    calculatedWork = ProofOfWorkVerifier.calculateNextWorkRequired(bitsAvg, nLastRetargetTime, nThisTime, params)
    assertEquals("Calculated PoW bits should be equal to expected one.", expectedWork, calculatedWork)


    // Test 2: Test the constraint on the upper bound for next work
    nLastRetargetTime = 1231006505 // Block #0 of Bitcoin
    nThisTime = 1233061996 // Block #2015 of Bitcoin
    bitsAvg = Utils.decodeCompactBits(UnsignedInts.toLong(0x1f07ffff))
    expectedWork = 0x1f07ffff

    calculatedWork = ProofOfWorkVerifier.calculateNextWorkRequired(bitsAvg, nLastRetargetTime, nThisTime, params)
    assertEquals("Calculated PoW bits should be equal to expected one.", expectedWork, calculatedWork)


    // Test 3: Test the constraint on the lower bound for actual time taken
    nLastRetargetTime = 1279296753 // NOTE: Not an actual block time
    nThisTime = 1279297671 // Block #68543 of Bitcoin
    bitsAvg = Utils.decodeCompactBits(UnsignedInts.toLong(0x1c05a3f4))
    expectedWork = 0x1c04bceb

    calculatedWork = ProofOfWorkVerifier.calculateNextWorkRequired(bitsAvg, nLastRetargetTime, nThisTime, params)
    assertEquals("Calculated PoW bits should be equal to expected one.", expectedWork, calculatedWork)


    // Test 4: Test the constraint on the upper bound for actual time taken
    nLastRetargetTime = 1269205629 // NOTE: Not an actual block time
    nThisTime = 1269211443 // Block #46367  of Bitcoin
    bitsAvg = Utils.decodeCompactBits(UnsignedInts.toLong(0x1c387f6f))
    expectedWork = 0x1c4a93bb

    calculatedWork = ProofOfWorkVerifier.calculateNextWorkRequired(bitsAvg, nLastRetargetTime, nThisTime, params)
    assertEquals("Calculated PoW bits should be equal to expected one.", expectedWork, calculatedWork)
  }

  case class PowRelatedData (mcblockhash: String, time: Int, bits: Int)

  @Test
  def ProofOfWorkVerifierTest_CheckNextWorkRequired(): Unit = {

    // Set PoW related data for testing
    var powRelatedDataList: List[PowRelatedData] = List(
      // 11 additional Blocks for medianTimeSpan calculation
      PowRelatedData("000000000b5eb20ecc1a71ce4b344d03d2faf74be598adf6003031e893dd6aba", 1559023042, 0x1c1362d9), // 0 MC block 523480
      PowRelatedData("0000000007c36f37621e9f57d020cbbacf37f2ae3d094561defdabd2c6d97798", 1559023097, 0x1c12912d), // 1
      PowRelatedData("0000000001cd1e7afd0008d299cbff61a23268bbfa740749b1a0e1f5a050a969", 1559023524, 0x1c128553), // 2
      PowRelatedData("000000000773168f0a20b407a419be0c8f4559fc163f3e2a5e5c56b278600072", 1559023542, 0x1c1273c6), // 3
      PowRelatedData("000000000ca9490904d221d10b2523ea283648ca0c48bf6cb85a13419ce36e86", 1559023842, 0x1c123879), // 4
      PowRelatedData("000000000227251feff1b3770fb99eb375c6816c5c6316de8eed828055b1f2d2", 1559023961, 0x1c12cc5e), // 5
      PowRelatedData("000000000cc11e9480c55c3571e1da9b0bb8a3acba452f92821351373dce5945", 1559024014, 0x1c12d434), // 6
      PowRelatedData("000000000e63a6e749be481084438c2a656c607499cdcfd91c07d10cee4f22f3", 1559024075, 0x1c12f147), // 7
      PowRelatedData("000000000be932857a7229840f796e70c03ea58fbf0dee6498c3869e1a706bb2", 1559024259, 0x1c13afb5), // 8
      PowRelatedData("000000000493223c2b87fea19afebd874b914fe80d3b14110d4bdf850b8020a7", 1559024555, 0x1c13682a), // 9
      PowRelatedData("0000000006b63fca6d6cb7605ead7c1dbaee3b3f445469ef279cfa5540c716e1", 1559024724, 0x1c12d779), // 10
      // MC blocks for nPowAveragingWindow = 17 for MainNet
      PowRelatedData("0000000000d6b37d4a673e902cf48abbca8f576fc571e44a7e66be2b0c2dee38", 1559024737, 0x1c12f031), // 11 MC block #523491
      PowRelatedData("00000000024b0156fe765d506b9a0b00ffae4a8bf78822f1692a82c972269a26", 1559024746, 0x1c12bf8c), // 12
      PowRelatedData("000000000184c3b8a948d839c278bf9709ae6adc1abf2e55f356296b6a94f420", 1559024796, 0x1c12c8dc), // 13
      PowRelatedData("000000000f77805d79566269aefaa5c7d40a0853bbca45762da51ea58b896c5f", 1559024928, 0x1c12c9b4), // 14
      PowRelatedData("00000000028538c544fed195eabf71da22fe538500689ec319dc68a426bc80ba", 1559024995, 0x1c12f11e), // 15
      PowRelatedData("0000000001bb939ee2695f5aea379b0a60774bb65524965080054c4d048ad1e6", 1559025005, 0x1c12e555), // 16
      PowRelatedData("0000000010f142d9a6b29d1588d802f697cee3731ecdff48286c1a425a21d971", 1559025152, 0x1c12703a), // 17
      PowRelatedData("00000000020a60751d027c9a3235a1a4b8be22296a09d3921fa7b2d0fd202eff", 1559025198, 0x1c126064), // 18
      PowRelatedData("000000000e60b44c2a976af60d26b48eff4cd9bdbc7ba89e35cff4621e64ac7f", 1559025444, 0x1c12615d), // 19
      PowRelatedData("000000000b8bca9a2265fe6280d4ad16b6702c6bd3779eff8c82fca7e9d59872", 1559025577, 0x1c12923c), // 20
      PowRelatedData("000000000fe9ad7587c491f8c3710743937120651d1ae556fe4d20cadf5af9e3", 1559025738, 0x1c12ac8a), // 21
      PowRelatedData("0000000003deb7de318889668cc4452fcb1eed86f0b5e585b429acec8b3873a6", 1559025834, 0x1c11d086), // 22 genesis mc block reference
      PowRelatedData("000000000657d8d0a2bd70385fd69531336d0e29a6f5c14dd1921f1159b4db73", 1559025889, 0x1c11f561), // 23
      PowRelatedData("000000001069a833c270d76769de8421658f047f6e587810f7e0335accfbbaa5", 1559025999, 0x1c11e514), // 24
      PowRelatedData("000000001124cbc1c5fb29dd553ed6c70bb46542af2ed3192dac47deec1bd4be", 1559026009, 0x1c1181b7), // 25
      PowRelatedData("0000000007be3cde4ac7246fc6d12f77ea0aca13fec7a032de180b3938bc70d0", 1559026181, 0x1c1198da), // 26
      PowRelatedData("000000001077fee4738c153526d66dc61a835cc5c8eb12b058c9e2050ba3bbdc", 1559026200, 0x1c113e3e), // 27 MC block #523507
      // MC blocks to check
      PowRelatedData("000000000efb0d5a2231c4312e84d0134d1534dd4da5b9e0555779fd70e37e03", 1559026523, 0x1c111cab), // 28 MC block #523508
      PowRelatedData("0000000001c3eb8acfd95d591016300c9fc6422115cc14bd8e7402d6836df19e", 1559026662, 0x1c1104d2)  // 29 MC block #523509
    )

    // mock SC Blocks for History
    val scblocks = ListBuffer[SidechainBlock]()

    scblocks.append(createSCBlockForPowTest(BytesUtils.toHexString(new Array[Byte](32)), powRelatedDataList(20).mcblockhash, Seq(powRelatedDataList(21)))) // SCBlock with genesis MCBlockRef
    scblocks.append(createSCBlockForPowTest(scblocks.last.id, "", Seq())) // SCBlock with no MCBlockRef
    scblocks.append(createSCBlockForPowTest(scblocks.last.id, powRelatedDataList(21).mcblockhash, Seq(powRelatedDataList(22), powRelatedDataList(23), powRelatedDataList(24)))) // SCBlock with 3 MCBlockRefs
    scblocks.append(createSCBlockForPowTest(scblocks.last.id, powRelatedDataList(24).mcblockhash, Seq(powRelatedDataList(25), powRelatedDataList(26)))) // SCBlock with 2 MCBlockRefs
    scblocks.append(createSCBlockForPowTest(scblocks.last.id, powRelatedDataList(26).mcblockhash, Seq(powRelatedDataList(27)))) // SCBlock with 1 MCBlockRef

    // mock History methods used in test
    val storage = mock[SidechainBlocks]
    Mockito.when(storage.blockById(ArgumentMatchers.any[ModifierId]()))
      .thenAnswer(answer => {
        Some(scblocks.filter(block => block.id.equals(answer.getArgument(0))).head)
      })

    // mock Params with Test genesis data
    // TO DO: review this
    class PowtestParams extends NetworkParams {
      override val EquihashN: Int = 0
      override val EquihashK: Int = 0
      override val EquihashVarIntLength: Int = 0
      override val EquihashSolutionLength: Int = 0
      override val powLimit: BigInteger = new BigInteger("0007ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16)
      override val nPowAveragingWindow: Int = 17 // = MainNetParams.nPowAveragingWindow
      override val nPowMaxAdjustDown: Int = 32
      override val nPowMaxAdjustUp: Int = 16
      override val nPowTargetSpacing: Int = 150
      override val sidechainId: Array[Byte] = null
      override val sidechainGenesisBlockId: ModifierId = null
      override val mainchainCreationBlockHeight: Int = 1
      override val genesisMainchainBlockHash: Array[Byte] = BytesUtils.fromHexString(powRelatedDataList(21).mcblockhash)
      override val genesisPoWData: List[(Int, Int)] = powRelatedDataList.slice(0, 21).map(powData => Tuple2(powData.time, powData.bits))
      override val withdrawalEpochLength: Int = 100
    }


    // Test 1: Check SCBlock without MainchainBlockReference
    var block = createSCBlockForPowTest(scblocks.last.id, "", Seq())
    assertTrue("SC block without MainchainBlockReference expected to have valid PoW Targed.",
      ProofOfWorkVerifier.checkNextWorkRequired(block, storage, new PowtestParams()))


    // Test 2: Check SCBlock with 1 valid MainchainBlockReference
    block = createSCBlockForPowTest(scblocks.last.id, powRelatedDataList(27).mcblockhash, Seq(powRelatedDataList(28)))
    assertTrue("SC block with 1 valid MainchainBlockReference expected to have valid PoW Targed.",
      ProofOfWorkVerifier.checkNextWorkRequired(block, storage, new PowtestParams()))


    // Test 3: Check SCBlock with multiple MainchainBlockReference
    block = createSCBlockForPowTest(scblocks.last.id, powRelatedDataList(27).mcblockhash, Seq(powRelatedDataList(28), powRelatedDataList(29)))
    assertTrue("SC block with 2 valid MainchainBlockReferences expected to have valid PoW Targed.",
      ProofOfWorkVerifier.checkNextWorkRequired(block, storage, new PowtestParams()))


    // Test 4: Check SCBlock, that contains 1 MainchainBlockReference with invalid target(bits)
    block = createSCBlockForPowTest(scblocks.last.id, powRelatedDataList(27).mcblockhash, Seq(
      PowRelatedData("0000000001c3eb8acfd95d591016300c9fc6422115cc14bd8e7402d6836df19e", 1559026662, 0x1c1104d4) // 0x1c1104d2 is valid one
    ))
    assertFalse("SC block, that contains 1 MainchainBlockReference with invalid target(bits), expected to have invalid PoW Targed.",
      ProofOfWorkVerifier.checkNextWorkRequired(block, storage, new PowtestParams()))


    // Test 5: Check SCBlock, that contains 1 MainchainBlockReference with invalid prev block reference
    block = createSCBlockForPowTest(scblocks.last.id, powRelatedDataList(20).mcblockhash, Seq(powRelatedDataList(28))) // 20 -> 27
    assertFalse("SC block, that contains 1 MainchainBlockReference with invalid prev block reference, expected to have invalid PoW Targed.",
      ProofOfWorkVerifier.checkNextWorkRequired(block, storage, new PowtestParams()))
  }

  def createSCBlockForPowTest(prevSCBlockId: String, prevMCBlockHash: String, powRelatedDataSeq: Seq[PowRelatedData]): SidechainBlock = {
    var block: SidechainBlock = mock[SidechainBlock]
    var blockHash = new Array[Byte](32)
    var tmpPrevMCBlockHash = prevMCBlockHash
    scala.util.Random.nextBytes(blockHash)
    Mockito.when(block.id).thenReturn(ModifierId @@ BytesUtils.toHexString(blockHash))
    Mockito.when(block.parentId).thenReturn(ModifierId @@ prevSCBlockId)
    Mockito.when(block.mainchainBlocks).thenReturn({
      var res = Seq[MainchainBlockReference]()
      for(powRelatedData <- powRelatedDataSeq) {
        res = res :+ new MainchainBlockReference(
          MainchainHeaderForPoWTest(powRelatedData.bits, BytesUtils.fromHexString(powRelatedData.mcblockhash), BytesUtils.fromHexString(tmpPrevMCBlockHash), powRelatedData.time),
          null,
          null)
        tmpPrevMCBlockHash = powRelatedData.mcblockhash
      }
      res
    })
    block
  }
}