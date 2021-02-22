package com.horizen.block

import com.horizen.box.Box
import com.horizen.cryptolibprovider.{FieldElementUtils, InMemoryOptimizedMerkleTreeUtils}
import com.horizen.merkletreenative.{InMemoryOptimizedMerkleTree, MerklePath}
import com.horizen.proposition.Proposition
import com.horizen.transaction.mainchain.SidechainRelatedMainchainOutput
import com.horizen.utils.{ByteArrayWrapper, BytesUtils}

import scala.collection.JavaConverters._
import scala.collection.mutable

class SidechainsCommitmentTree
{
  val sidechainsHashMap: mutable.Map[ByteArrayWrapper, SidechainCommitmentEntry] = new mutable.HashMap[ByteArrayWrapper, SidechainCommitmentEntry]()

  def addForwardTransfers(sidechainId: ByteArrayWrapper, ftOutputs: Seq[SidechainRelatedMainchainOutput[_ <: Box[_ <: Proposition]]]): Unit = {
    val forwardTransfersRootHash = InMemoryOptimizedMerkleTreeUtils.merkleTreeRootHash(ftOutputs.map(_.fieldElementBytes()).asJava)
    sidechainsHashMap.get(sidechainId) match {
      case Some(entry) =>
        entry.setForwardTransfersHash(forwardTransfersRootHash)
      case None =>
        val entry = new SidechainCommitmentEntry()
        entry.setForwardTransfersHash(forwardTransfersRootHash)
        sidechainsHashMap.put(sidechainId, entry)
    }
  }

  def addCertificate(certificate: WithdrawalEpochCertificate): Unit = {
    val sidechainId = new ByteArrayWrapper(certificate.sidechainId)

    val fieldElement: Array[Byte] = new Array[Byte](FieldElementUtils.maximumFieldElementLength)
    val hashLE: Array[Byte] = BytesUtils.reverseBytes(certificate.hash)
    System.arraycopy(hashLE, 0, fieldElement, 0, hashLE.length)

    sidechainsHashMap.get(sidechainId) match {
      case Some(entry) => entry.setWithdrawalCertificateHash(fieldElement)
      case None =>
        val entry = new SidechainCommitmentEntry()
        entry.setWithdrawalCertificateHash(fieldElement)
        sidechainsHashMap.put(sidechainId, entry)
    }
  }

  def getSidechainCommitmentEntryHash(sidechainId: ByteArrayWrapper): Array[Byte] = {
    sidechainsHashMap.get(sidechainId) match {
      case Some(entry) => entry.getSidechainCommitmentEntryHash(sidechainId.data)
      case None => Array.emptyByteArray
    }
  }

  private def getMerkleTreeLeaves(): Seq[Array[Byte]] = {
    getOrderedSidechainIds().map(id => getSidechainCommitmentEntryHash(id))
  }

  private[block] def getMerkleTree(): InMemoryOptimizedMerkleTree = {
    InMemoryOptimizedMerkleTreeUtils.merkleTree(getMerkleTreeLeaves().asJava)
  }

  def getMerkleRoot(): Array[Byte] = {
    InMemoryOptimizedMerkleTreeUtils.merkleTreeRootHash(getMerkleTreeLeaves().asJava)
  }

  def getSidechainCommitmentEntryMerklePath(sidechainId: ByteArrayWrapper): Option[MerklePath] = {
    sidechainsHashMap.get(sidechainId) match {
      case Some(entry) =>
        val merkleTree: InMemoryOptimizedMerkleTree = getMerkleTree()
        val entryHash = new ByteArrayWrapper(entry.getSidechainCommitmentEntryHash(sidechainId.data))
        val leafIndex = getMerkleTreeLeaves().view.map(l => new ByteArrayWrapper(l)).indexOf(entryHash)
        val merklePath = Some(merkleTree.getMerklePath(leafIndex))

        merkleTree.freeInMemoryOptimizedMerkleTree()

        merklePath
      case None => None
    }
  }

  def getNeighbourSidechainCommitmentEntryProofs(sidechainId: ByteArrayWrapper): (Option[SidechainCommitmentEntryProof], Option[SidechainCommitmentEntryProof]) = {
    // Collect and sort sidechain Ids
    val sidechainsIds: Seq[ByteArrayWrapper] = getOrderedSidechainIds()
    val littleEndianSidechainId = new ByteArrayWrapper(BytesUtils.reverseBytes(sidechainId.data))

    val leftNeighbourIndex: Int = sidechainsIds.lastIndexWhere(id => new ByteArrayWrapper(BytesUtils.reverseBytes(id.data)) < littleEndianSidechainId)
    val leftNeighbourProofOption = if(leftNeighbourIndex >= 0) {
      val leftNeighbourSidechainId: Array[Byte] = sidechainsIds(leftNeighbourIndex).data
      Some(getSidechainCommitmentEntryProof(leftNeighbourSidechainId, leftNeighbourIndex))
    } else {
      None
    }

    val rightNeighbourIndex: Int = sidechainsIds.indexWhere(id => new ByteArrayWrapper(BytesUtils.reverseBytes(id.data)) > littleEndianSidechainId, from = leftNeighbourIndex)
    val rightNeighbourProofOption = if(rightNeighbourIndex >= 0 ) {
      val rightNeighbourSidechainId: Array[Byte] = sidechainsIds(rightNeighbourIndex).data
      Some(getSidechainCommitmentEntryProof(rightNeighbourSidechainId, rightNeighbourIndex))
    } else {
      None
    }

    (leftNeighbourProofOption, rightNeighbourProofOption)
  }

  private def getSidechainCommitmentEntryProof(sidechainId: Array[Byte], leafIndex: Int): SidechainCommitmentEntryProof = {
    val merkleTree: InMemoryOptimizedMerkleTree = getMerkleTree()
    val entry = sidechainsHashMap(new ByteArrayWrapper(sidechainId))
    SidechainCommitmentEntryProof(
      sidechainId,
      entry.getForwardTransfersHash,
      entry.getBackwardTransferRequestHash,
      entry.getWCertHash,
      merkleTree.getMerklePath(leafIndex)
    )
  }

  // Sidechain ids are represented in a little-endian and ordered lexicographically same as in the MC.
  // Note: MC data in the SC represented as a big-endian.
  private def getOrderedSidechainIds(): Seq[ByteArrayWrapper] = {
    val littleEndianOrderedIds = sidechainsHashMap.map(entry => new ByteArrayWrapper(BytesUtils.reverseBytes(entry._1.data))).toSeq.sortWith(_ < _)
    littleEndianOrderedIds.map(id => new ByteArrayWrapper(BytesUtils.reverseBytes(id.data)))
  }
}