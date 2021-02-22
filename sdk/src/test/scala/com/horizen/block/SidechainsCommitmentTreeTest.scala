package com.horizen.block

import com.google.common.primitives.{Bytes, Ints}
import com.horizen.box.Box
import com.horizen.librustsidechains.FieldElement
import com.horizen.proposition.Proposition
import com.horizen.transaction.mainchain.SidechainRelatedMainchainOutput
import com.horizen.utils.{ByteArrayWrapper, BytesUtils}
import org.junit.Assert.{assertEquals, assertTrue}
import org.junit.Test
import org.mockito.Mockito
import org.scalatest.junit.JUnitSuite
import org.scalatest.mockito.MockitoSugar

import scala.util.Random

class SidechainsCommitmentTreeTest extends JUnitSuite with MockitoSugar{

  def getWithPadding(bytes: Array[Byte]): Array[Byte] = Bytes.concat(new Array[Byte](32 - bytes.length), bytes)

  def getSidechains(sidechainsCount: Int): (Seq[ByteArrayWrapper], ByteArrayWrapper, ByteArrayWrapper, ByteArrayWrapper) = {
    val beforeLeftMostSidechainId = new ByteArrayWrapper(
      getWithPadding(
        Bytes.concat(
          BytesUtils.fromHexString("abcdef"),
          Ints.toByteArray(1)
        )
      )
    )

    val afterRightMostSidechainId = new ByteArrayWrapper(
      getWithPadding(
        Bytes.concat(
          BytesUtils.fromHexString("abcdef"),
          Ints.toByteArray(sidechainsCount*2 + 1)
        )
      )
    )

    val innerSidechainId = new ByteArrayWrapper(
      getWithPadding(
        Bytes.concat(
          BytesUtils.fromHexString("abcdef"),
          Ints.toByteArray(sidechainsCount)
        )
      )
    )

    val sidechainIdSeq = (2 to sidechainsCount * 2 by 2)
      .map(v =>
        Bytes.concat(
          BytesUtils.fromHexString("abcdef"),
          Ints.toByteArray(v)
        )
      )
      .map(v => new ByteArrayWrapper(getWithPadding(v)))

    (sidechainIdSeq, beforeLeftMostSidechainId, innerSidechainId, afterRightMostSidechainId)
  }

  private def getRandomFieldElementBytes: Array[Byte] = {
    // Last 5 bytes = 0 to be sure that bytes represent a valid field element
    val safeFieldElementSize = FieldElement.FIELD_ELEMENT_LENGTH - 5
    val feBytes = new Array[Byte](safeFieldElementSize)
    Random.nextBytes(feBytes)
    Bytes.concat(feBytes, Array.fill[Byte](5)(0))
  }

  private def checkNeighboursProofs(sidechainsNumber: Int): Unit = {
    // Test neighbours proofs for the list of N elements
    val commitmentTree = new SidechainsCommitmentTree()

    val (sidechainIdSeq, beforeLeftMostSidechainId, innerSidechainId, afterRightMostSidechainId) = getSidechains(sidechainsNumber)

    val mockedOutput = mock[SidechainRelatedMainchainOutput[_ <: Box[_ <: Proposition]]]
    Mockito.when(mockedOutput.fieldElementBytes()).thenReturn(getRandomFieldElementBytes)

    sidechainIdSeq.foreach(v => commitmentTree.addForwardTransfers(v, Seq(mockedOutput)))

    val rootfe: FieldElement = FieldElement.deserialize(commitmentTree.getMerkleRoot())

    val (lProof1, rProof1) = commitmentTree.getNeighbourSidechainCommitmentEntryProofs(beforeLeftMostSidechainId)
    assertTrue("Proof for left neighbour must not exist.", lProof1.isEmpty)
    assertTrue("Proof for right neighbour must exist.", rProof1.isDefined)

    assertTrue("Right neighbour must be leftmost", rProof1.get.merklePath.isLeftmost)
    assertTrue("Right neighbour proof must be valid.",
      rProof1.get.merklePath.verify(FieldElement.deserialize(SidechainCommitmentEntry.getSidechainCommitmentEntryHash(rProof1.get)), rootfe)
    )

    val (lProof2, rProof2) = commitmentTree.getNeighbourSidechainCommitmentEntryProofs(innerSidechainId)

    assertTrue("Proof for left neighbour must exist.", lProof2.isDefined)
    assertTrue("Proof for right neighbour must exist.", rProof2.isDefined)

    assertEquals("Left neighbour must have different leaf index",
      sidechainIdSeq.lastIndexWhere(_ < innerSidechainId), lProof2.get.merklePath.leafIndex())
    assertTrue("Left neighbour proof must be valid.",
      lProof2.get.merklePath.verify(FieldElement.deserialize(SidechainCommitmentEntry.getSidechainCommitmentEntryHash(lProof2.get)), rootfe)
    )

    assertEquals("Right neighbour must have different leaf index",
      sidechainIdSeq.indexWhere(_ > innerSidechainId), rProof2.get.merklePath.leafIndex())
    assertTrue("Right neighbour proof must be valid.",
      rProof2.get.merklePath.verify(FieldElement.deserialize(SidechainCommitmentEntry.getSidechainCommitmentEntryHash(rProof2.get)), rootfe)
    )

    val (lProof3, rProof3) = commitmentTree.getNeighbourSidechainCommitmentEntryProofs(afterRightMostSidechainId)

    assertTrue("Proof for left neighbour must exist.", lProof3.isDefined)
    assertTrue("Proof for right neighbour must not exist.", rProof3.isEmpty)

    assertTrue("Left neighbour must be rightmost",
      lProof3.get.merklePath.isNonEmptyRightmost)
    assertTrue("Left neighbour proof must be valid.",
      lProof3.get.merklePath.verify(FieldElement.deserialize(SidechainCommitmentEntry.getSidechainCommitmentEntryHash(lProof3.get)), rootfe)
    )
  }

  @Test
  def checkNeighboursProofs11(): Unit = {
    checkNeighboursProofs(11)
  }

  @Test
  def checkNeighboursProofs23(): Unit = {
    checkNeighboursProofs(23)
  }

  @Test
  def checkNeighboursProofs47(): Unit = {
    checkNeighboursProofs(47)
  }
}
