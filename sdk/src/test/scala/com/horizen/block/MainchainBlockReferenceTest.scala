package com.horizen.block

import com.google.common.primitives.Ints
import com.horizen.params.{MainNetParams, RegTestParams}
import com.horizen.utils.{ByteArrayWrapper, BytesUtils}
import org.junit.Test
import org.junit.Assert.{assertEquals, assertFalse, assertTrue, fail => jFail}
import org.scalatest.junit.JUnitSuite

import scala.io.Source
import scala.util.{Failure, Success, Try}

class MainchainBlockReferenceTest extends JUnitSuite {

  @Test
  def blocksWithoutScSupportParsing(): Unit = {
    var mcBlockHex: String = null
    var mcBlockBytes: Array[Byte] = null
    var block: Try[MainchainBlockReference] = null


    val params = MainNetParams()

    // Test 1: Block #473173
    // mcblock473173_mainnet data in RPC byte order: https://explorer.zen-solutions.io/api/rawblock/0000000024ebb5c6d558daa34ad9b9a4c5503b057e14815a48e241612b1eb660
    mcBlockHex = Source.fromResource("mcblock473173_mainnet").getLines().next()
    mcBlockBytes = BytesUtils.fromHexString(mcBlockHex)
    block = MainchainBlockReference.create(mcBlockBytes, params)

    assertTrue("Block expected to be parsed", block.isSuccess)
    assertEquals("Block Hash is different.", "0000000024ebb5c6d558daa34ad9b9a4c5503b057e14815a48e241612b1eb660", block.get.header.hashHex)
    assertFalse("Old Block occurred, MProof expected to be undefined.", block.get.data.mProof.isDefined)
    assertFalse("Old Block occurred, proof of no data for left neighbour expected to be undefined.", block.get.data.proofOfNoData._1.isDefined)
    assertFalse("Old Block occurred, proof of no data for right neighbour expected to be undefined.", block.get.data.proofOfNoData._2.isDefined)
    assertFalse("Old Block occurred, MC2SCAggTx expected to be undefined.", block.get.data.sidechainRelatedAggregatedTransaction.isDefined)
    assertFalse("Old Block occurred, Certificate expected to be undefined.", block.get.data.withdrawalEpochCertificate.isDefined)
    assertEquals("Block version = 536870912 expected.", 536870912, block.get.header.version)
    assertEquals("Hash of previous block is different.", "0000000009572f35ecc6e319216b29046fdb6695ad93b3e5d77053285df4af03", BytesUtils.toHexString(block.get.header.hashPrevBlock))
    assertEquals("Merkle root hash is different.", "5bf368ee4fc02f055e8ca5447a21b9758e6435b3214bc10b55f533cc9b3d1a6d", BytesUtils.toHexString(block.get.header.hashMerkleRoot))
    assertEquals("SCMap Merkle root hash expected to be zero bytes.", BytesUtils.toHexString(params.zeroHashBytes), BytesUtils.toHexString(block.get.header.hashScTxsCommitment))
    assertEquals("Block creation time is different", 1551432137, block.get.header.time)
    assertEquals("Block PoW bits is different.", "1c2abb60", BytesUtils.toHexString(Ints.toByteArray(block.get.header.bits)))
    assertEquals("Block nonce is different.", "00000000000000000000000000030000000000009921008000000000c7cf0410", BytesUtils.toHexString(block.get.header.nonce))
    assertEquals("Block equihash solution length is wrong.", params.EquihashSolutionLength, block.get.header.solution.length)
    block.get.semanticValidity(params) match {
      case Success(_) =>
      case Failure(e) => jFail(s"Block expected to be semantically valid, instead: ${e.getMessage}")
    }



    // Test 2: Block #501173
    // mcblock501173 data in RPC byte order: https://explorer.zen-solutions.io/api/rawblock/0000000011aec26c29306d608645a644a592e44add2988a9d156721423e714e0
    mcBlockHex = Source.fromResource("mcblock501173").getLines().next()
    mcBlockBytes = BytesUtils.fromHexString(mcBlockHex)
    block = MainchainBlockReference.create(mcBlockBytes, params)

    assertTrue("Block expected to be parsed", block.isSuccess)
    assertEquals("Block Hash is different.", "0000000011aec26c29306d608645a644a592e44add2988a9d156721423e714e0", block.get.header.hashHex)
    assertFalse("Old Block occurred, MProof expected to be undefined.", block.get.data.mProof.isDefined)
    assertFalse("Old Block occurred, proof of no data for left neighbour expected to be undefined.", block.get.data.proofOfNoData._1.isDefined)
    assertFalse("Old Block occurred, proof of no data for right neighbour expected to be undefined.", block.get.data.proofOfNoData._2.isDefined)
    assertFalse("Old Block occurred, MC2SCAggTx expected to be undefined.", block.get.data.sidechainRelatedAggregatedTransaction.isDefined)
    assertFalse("Old Block occurred, Certificate expected to be undefined.", block.get.data.withdrawalEpochCertificate.isDefined)
    assertEquals("Block version = 536870912 expected.", 536870912, block.get.header.version)
    assertEquals("Hash of previous block is different.", "00000000106843ee0119c6db92e38e8655452fd85f638f6640475e8c6a3a3582", BytesUtils.toHexString(block.get.header.hashPrevBlock))
    assertEquals("Merkle root hash is different.", "493232e7d362852c8e3fe6aa5a48d6f6e01220f617c258db511ee2386b6362ea", BytesUtils.toHexString(block.get.header.hashMerkleRoot))
    assertEquals("SCMap Merkle root hash expected to be zero bytes.", BytesUtils.toHexString(params.zeroHashBytes), BytesUtils.toHexString(block.get.header.hashScTxsCommitment))
    assertEquals("Block creation time is different", 1555658453, block.get.header.time)
    assertEquals("Block PoW bits is different.", "1c1bbecc", BytesUtils.toHexString(Ints.toByteArray(block.get.header.bits)))
    assertEquals("Block nonce is different.", "0000000000000000000000000019000000000000751d00600000000000000000", BytesUtils.toHexString(block.get.header.nonce))
    assertEquals("Block equihash solution length is wrong.", params.EquihashSolutionLength, block.get.header.solution.length)
    block.get.semanticValidity(params) match {
      case Success(_) =>
      case Failure(e) => jFail(s"Block expected to be semantically valid, instead: ${e.getMessage}")
    }


    // Test 3: Block #273173
    // mcblock273173 data in RPC byte order: https://explorer.zen-solutions.io/api/rawblock/0000000009b9f4a9f2abe5cd129421df969d1eb1b02d3fd685ab0781939ead07
    mcBlockHex = Source.fromResource("mcblock273173").getLines().next()
    mcBlockBytes = BytesUtils.fromHexString(mcBlockHex)
    block = MainchainBlockReference.create(mcBlockBytes, params)

    assertTrue("Block expected to be parsed", block.isSuccess)
    assertEquals("Block Hash is different.", "0000000009b9f4a9f2abe5cd129421df969d1eb1b02d3fd685ab0781939ead07", block.get.header.hashHex)
    assertFalse("Old Block occurred, MProof expected to be undefined.", block.get.data.mProof.isDefined)
    assertFalse("Old Block occurred, proof of no data for left neighbour expected to be undefined.", block.get.data.proofOfNoData._1.isDefined)
    assertFalse("Old Block occurred, proof of no data for right neighbour expected to be undefined.", block.get.data.proofOfNoData._2.isDefined)
    assertFalse("Old Block occurred, MC2SCAggTx expected to be undefined.", block.get.data.sidechainRelatedAggregatedTransaction.isDefined)
    assertFalse("Old Block occurred, Certificate expected to be undefined.", block.get.data.withdrawalEpochCertificate.isDefined)
    assertEquals("Block version = 536870912 expected.", 536870912, block.get.header.version)
    assertEquals("Hash of previous block is different.", "0000000071076828a1d738dfde576b21ac4e28998ae7a026f631e57d7561a28b", BytesUtils.toHexString(block.get.header.hashPrevBlock))
    assertEquals("Merkle root hash is different.", "7169f926344ff99dbee02ed2429481bbbc0b84cb4773c1dcaee20458e0d0437a", BytesUtils.toHexString(block.get.header.hashMerkleRoot))
    assertEquals("SCMap Merkle root hash expected to be zero bytes.", BytesUtils.toHexString(params.zeroHashBytes), BytesUtils.toHexString(block.get.header.hashScTxsCommitment))
    assertEquals("Block creation time is different", 1521052551, block.get.header.time)
    assertEquals("Block PoW bits is different.", "1d010d77", BytesUtils.toHexString(Ints.toByteArray(block.get.header.bits)))
    assertEquals("Block nonce is different.", "000000000000000000000000cfcbffd9de586ff80e928e9b83e86c3c8c580000", BytesUtils.toHexString(block.get.header.nonce))
    assertEquals("Block equihash solution length is wrong.", params.EquihashSolutionLength, block.get.header.solution.length)
    block.get.semanticValidity(params) match {
      case Success(_) =>
      case Failure(e) => jFail(s"Block expected to be semantically valid, instead: ${e.getMessage}")
    }
  }

  @Test
  def blockWithoutSidechains(): Unit = {
    val scIdHex = "0000000000000000000000000000000000000000000000000000000000000000"
    val scId = new ByteArrayWrapper(BytesUtils.fromHexString(scIdHex))
    val params = RegTestParams(scId.data)

    // Test: parse MC block with tx version -4 without sidechain related stuff.
    val mcBlockHex = Source.fromResource("new_mc_blocks/mc_block_empty_sidechains").getLines().next()
    val mcBlockBytes = BytesUtils.fromHexString(mcBlockHex)
    val mcblockTry = MainchainBlockReference.create(mcBlockBytes, params)

    assertTrue("Block expected to be parsed", mcblockTry.isSuccess)
    val mcblock = mcblockTry.get

    assertTrue("Block must not contain transaction.", mcblock.data.sidechainRelatedAggregatedTransaction.isEmpty)
    assertTrue("Block must not contain certificate.", mcblock.data.withdrawalEpochCertificate.isEmpty)
    assertTrue("Block must not contain proof.", mcblock.data.mProof.isEmpty)
    assertTrue("Block must not contain proof for left neighbour.", mcblock.data.proofOfNoData._1.isEmpty)
    assertTrue("Block must not contain proof for right neighbour.", mcblock.data.proofOfNoData._2.isEmpty)

    assertEquals("Block Hash is different.", "07a96c734baed8ab18559c21aaa47cd6286f48f83bbb5bba038772a43a3ffe13", mcblock.header.hashHex)
    assertEquals("Block version = 3 expected.", 536870912, mcblock.header.version)
    assertEquals("Hash of previous block is different.", "062bebe609cde184822b80809681508652e1e042d403b4f4f708bf076715c8c6", BytesUtils.toHexString(mcblock.header.hashPrevBlock))
    assertEquals("Merkle root hash is different.", "99ea70ff8f0628ad4b65ebf2884846bc5c7aabe81ba4a24a8199720f1d5be4df", BytesUtils.toHexString(mcblock.header.hashMerkleRoot))
    assertEquals("SCMap Merkle root hash is different.", BytesUtils.toHexString(params.emptyScTransactionCommitment), BytesUtils.toHexString(mcblock.header.hashScTxsCommitment))
    assertEquals("Block creation time is different", 1601550658, mcblock.header.time)
    assertEquals("Block PoW bits is different.", "200f0f04", BytesUtils.toHexString(Ints.toByteArray(mcblock.header.bits)))
    assertEquals("Block nonce is different.", "00004df1d2e56dff809eb6663a0d0953651269b5d7f713234084b82c90660054", BytesUtils.toHexString(mcblock.header.nonce))
    assertEquals("Block equihash solution length is wrong.", params.EquihashSolutionLength, mcblock.header.solution.length)
    assertTrue("Block expected to be semantically valid", mcblock.semanticValidity(params).isSuccess)
  }

  @Test
  def blockWithSingleSidechain(): Unit = {
    // Test: parse MC block with tx version -4 with 1 sidechain (sc creation).
    val mcBlockHex = Source.fromResource("new_mc_blocks/mc_block_with_1_sidechain").getLines().next()
    val mcBlockBytes = BytesUtils.fromHexString(mcBlockHex)

    // Test 1: Check for the sidechain mentioned in the block.
    // We expect to get a MainchainBlockReference with AggTx and proof of presence.
    val scIdHex1 = "52a2a9f2955232ab7d22792b831fba7d3e3bd61587d040130e7fd3a85bf77d5f"
    val scId1 = new ByteArrayWrapper(BytesUtils.fromHexString(scIdHex1))

    val params1 = RegTestParams(scId1.data)

    val mcblockTry1 = MainchainBlockReference.create(mcBlockBytes, params1)

    assertTrue("Block expected to be parsed", mcblockTry1.isSuccess)
    val mcblock1 = mcblockTry1.get

    assertTrue("Block expected to be semantically valid", mcblock1.semanticValidity(params1).isSuccess)

    assertTrue("Block must contain transaction.", mcblock1.data.sidechainRelatedAggregatedTransaction.isDefined)
    assertTrue("Block must not contain certificate.", mcblock1.data.withdrawalEpochCertificate.isEmpty)
    assertTrue("Block must contain proof.", mcblock1.data.mProof.isDefined)
    assertTrue("Block must not contain proof for left neighbour.", mcblock1.data.proofOfNoData._1.isEmpty)
    assertTrue("Block must not contain proof for right neighbour.", mcblock1.data.proofOfNoData._2.isEmpty)
  }

  @Test
  def blockWith3Sidechains(): Unit = {
    // Test: parse MC block with tx version -4 with 3 sidechains.
    val mcBlockHex = Source.fromResource("new_mc_blocks/mc_block_with_3_sidechains").getLines().next()
    val mcBlockBytes = BytesUtils.fromHexString(mcBlockHex)


    // Test 1: Check for the leftmost sidechain mentioned in the block.
    // We expect to get a MainchainBlockReference with AggTx and proof of presence.
    val scIdHex1 = "52a2a9f2955232ab7d22792b831fba7d3e3bd61587d040130e7fd3a85bf77d5f"
    val scId1 = new ByteArrayWrapper(BytesUtils.fromHexString(scIdHex1))

    val params1 = RegTestParams(scId1.data)

    val mcblockTry1 = MainchainBlockReference.create(mcBlockBytes, params1)

    assertTrue("Block expected to be parsed", mcblockTry1.isSuccess)
    val mcblock1 = mcblockTry1.get

    assertTrue("Block expected to be semantically valid", mcblock1.semanticValidity(params1).isSuccess)

    assertTrue("Block must contain transaction.", mcblock1.data.sidechainRelatedAggregatedTransaction.isDefined)
    assertTrue("Block must not contain certificate.", mcblock1.data.withdrawalEpochCertificate.isEmpty)
    assertTrue("Block must contain proof.", mcblock1.data.mProof.isDefined)
    assertTrue("Block must not contain proof for left neighbour.", mcblock1.data.proofOfNoData._1.isEmpty)
    assertTrue("Block must not contain proof for right neighbour.", mcblock1.data.proofOfNoData._2.isEmpty)


    // Test 2: Check for the sidechain in the middle, that is mentioned in the block.
    // We expect to get a MainchainBlockReference with AggTx and proof of presence.
    val scIdHex2 = "2b051b49c4080896f83ebecd86cae7706b5a2a611fb141d87a717459adeb21bc"
    val scId2 = new ByteArrayWrapper(BytesUtils.fromHexString(scIdHex2))

    val params2 = RegTestParams(scId2.data)

    val mcblockTry2 = MainchainBlockReference.create(mcBlockBytes, params2)

    assertTrue("Block expected to be parsed", mcblockTry2.isSuccess)
    val mcblock2 = mcblockTry2.get

    assertTrue("Block expected to be semantically valid", mcblock2.semanticValidity(params2).isSuccess)

    assertTrue("Block must contain transaction.", mcblock2.data.sidechainRelatedAggregatedTransaction.isDefined)
    assertTrue("Block must not contain certificate.", mcblock2.data.withdrawalEpochCertificate.isEmpty)
    assertTrue("Block must contain proof.", mcblock2.data.mProof.isDefined)
    assertTrue("Block must not contain proof for left neighbour.", mcblock2.data.proofOfNoData._1.isEmpty)
    assertTrue("Block must not contain proof for right neighbour.", mcblock2.data.proofOfNoData._2.isEmpty)


    // Test 3:  Check for the rightmost sidechain mentioned in the block.
    // We expect to get a MainchainBlockReference with AggTx and proof of presence.
    val scIdHex3 = "c7b8b013d9a4ed6770b9f7ff306d324a587abe7ad6b52187c2d556b157dbaee7"
    val scId3 = new ByteArrayWrapper(BytesUtils.fromHexString(scIdHex3))

    val params3 = RegTestParams(scId3.data)

    val mcblockTry3 = MainchainBlockReference.create(mcBlockBytes, params3)

    assertTrue("Block expected to be parsed", mcblockTry3.isSuccess)
    val mcblock3 = mcblockTry3.get

    assertTrue("Block expected to be semantically valid", mcblock3.semanticValidity(params3).isSuccess)

    assertTrue("Block must contain transaction.", mcblock3.data.sidechainRelatedAggregatedTransaction.isDefined)
    assertTrue("Block must not contain certificate.", mcblock3.data.withdrawalEpochCertificate.isEmpty)
    assertTrue("Block must contain proof.", mcblock3.data.mProof.isDefined)
    assertTrue("Block must not contain proof for left neighbour.", mcblock3.data.proofOfNoData._1.isEmpty)
    assertTrue("Block must not contain proof for right neighbour.", mcblock3.data.proofOfNoData._2.isEmpty)


    // Test 4: Check for the sidechain that is not mentioned in the block and has a leftmost neighbour.
    // We expect to get a MainchainBlockReference with only right neighbour proof of no data.
    val scIdHex4 = "0000000000000000000000000000000000000000000000000000000000000000"
    val scId4 = new ByteArrayWrapper(BytesUtils.fromHexString(scIdHex4))
    val params4 = RegTestParams(scId4.data)

    val mcblockTry4 = MainchainBlockReference.create(mcBlockBytes, params4)

    assertTrue("Block expected to be parsed", mcblockTry4.isSuccess)
    val mcblock4 = mcblockTry4.get

    assertTrue("Block expected to be semantically valid", mcblock4.semanticValidity(params4).isSuccess)

    assertTrue("Block must not contain transaction.", mcblock4.data.sidechainRelatedAggregatedTransaction.isEmpty)
    assertTrue("Block must not contain certificate.", mcblock4.data.withdrawalEpochCertificate.isEmpty)
    assertTrue("Block must not contain proof.", mcblock4.data.mProof.isEmpty)
    assertTrue("Block must not contain proof for left neighbour.", mcblock4.data.proofOfNoData._1.isEmpty)
    assertTrue("Block must contain proof for right neighbour.", mcblock4.data.proofOfNoData._2.isDefined)


    // Test 5: Check for the sidechain that is not mentioned in the block and has a rightmost neighbour.
    // We expect to get a MainchainBlockReference with only left neighbour proof of no data.
    val scIdHex5 = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
    val scId5 = new ByteArrayWrapper(BytesUtils.fromHexString(scIdHex5))
    val params5 = RegTestParams(scId5.data)

    val mcblockTry5 = MainchainBlockReference.create(mcBlockBytes, params5)

    assertTrue("Block expected to be parsed", mcblockTry5.isSuccess)
    val mcblock5 = mcblockTry5.get

    assertTrue("Block expected to be semantically valid", mcblock5.semanticValidity(params5).isSuccess)

    assertTrue("Block must not contain transaction.", mcblock5.data.sidechainRelatedAggregatedTransaction.isEmpty)
    assertTrue("Block must not contain certificate.", mcblock5.data.withdrawalEpochCertificate.isEmpty)
    assertTrue("Block must not contain proof.", mcblock5.data.mProof.isEmpty)
    assertTrue("Block must contain proof for left neighbour.", mcblock5.data.proofOfNoData._1.isDefined)
    assertTrue("Block must not contain proof for right neighbour.", mcblock5.data.proofOfNoData._2.isEmpty)


    // Test 6: Check for the sidechain that is not mentioned in the block and has a both left and right neighbours.
    // We expect to get a MainchainBlockReference with left and right neighbour proofs of no data.
    // Take the value near to scIdHex2. Quite enough condition to have id, where, scIdHex1 < id < scIdHex2
    val scIdHex6 = "0000" + scIdHex2.substring(4)
    val scId6 = new ByteArrayWrapper(BytesUtils.fromHexString(scIdHex6))
    val params6 = RegTestParams(scId6.data)

    val mcblockTry6 = MainchainBlockReference.create(mcBlockBytes, params6)

    assertTrue("Block expected to be parsed", mcblockTry6.isSuccess)
    val mcblock6 = mcblockTry6.get

    assertTrue("Block expected to be semantically valid", mcblock6.semanticValidity(params6).isSuccess)

    assertTrue("Block must not contain transaction.", mcblock6.data.sidechainRelatedAggregatedTransaction.isEmpty)
    assertTrue("Block must not contain certificate.", mcblock6.data.withdrawalEpochCertificate.isEmpty)
    assertTrue("Block must not contain proof.", mcblock6.data.mProof.isEmpty)
    assertTrue("Block must contain proof for left neighbour.", mcblock6.data.proofOfNoData._1.isDefined)
    assertTrue("Block must contain proof for right neighbour.", mcblock6.data.proofOfNoData._2.isDefined)


    // Test 7: Check for the sidechain that is not mentioned in the block and has a both left and right neighbours.
    // We expect to get a MainchainBlockReference with left and right neighbour proofs of no data.
    // Take the value near to scIdHex1. Quite enough condition to have id, where, scIdHex1 < id < scIdHex2
    val scIdHex7 = "ffff" + scIdHex1.substring(4)
    val scId7 = new ByteArrayWrapper(BytesUtils.fromHexString(scIdHex7))
    val params7 = RegTestParams(scId7.data)

    val mcblockTry7 = MainchainBlockReference.create(mcBlockBytes, params7)

    assertTrue("Block expected to be parsed", mcblockTry7.isSuccess)
    val mcblock7 = mcblockTry7.get

    assertTrue("Block expected to be semantically valid", mcblock7.semanticValidity(params6).isSuccess)

    assertTrue("Block must not contain transaction.", mcblock7.data.sidechainRelatedAggregatedTransaction.isEmpty)
    assertTrue("Block must not contain certificate.", mcblock7.data.withdrawalEpochCertificate.isEmpty)
    assertTrue("Block must not contain proof.", mcblock7.data.mProof.isEmpty)
    assertTrue("Block must contain proof for left neighbour.", mcblock7.data.proofOfNoData._1.isDefined)
    assertTrue("Block must contain proof for right neighbour.", mcblock7.data.proofOfNoData._2.isDefined)
  }

  @Test
  def blockWithBackwardTransfer(): Unit = {
    // Test: parse MC block with tx version -4 with backward transfer.
    val mcBlockHex = Source.fromResource("new_mc_blocks/mc_block_with_backward_transfer").getLines().next()
    val mcBlockBytes = BytesUtils.fromHexString(mcBlockHex)

    //Check for sidechain 1
    val scIdHex1 = "de1ffe5186854b48b27be5e37964da3bb42f8e16fb6b62b76a8e20b24d03f9a9"
    val scId1 = new ByteArrayWrapper(BytesUtils.fromHexString(scIdHex1))

    val params1 = RegTestParams(scId1.data)

    val mcblockTry1 = MainchainBlockReference.create(mcBlockBytes, params1)

    assertTrue("Block expected to be parsed", mcblockTry1.isSuccess)
    val mcblock1 = mcblockTry1.get

    assertTrue("Block expected to be semantically valid", mcblock1.semanticValidity(params1).isSuccess)

    assertTrue("Block must not contain transaction.", mcblock1.data.sidechainRelatedAggregatedTransaction.isEmpty)
    assertTrue("Block must contain certificate.", mcblock1.data.withdrawalEpochCertificate.isDefined)
    assertTrue("Block must contain proof.", mcblock1.data.mProof.isDefined)
    assertTrue("Block must not contain proof for left neighbour.", mcblock1.data.proofOfNoData._1.isEmpty)
    assertTrue("Block must not contain proof for right neighbour.", mcblock1.data.proofOfNoData._2.isEmpty)
  }
}