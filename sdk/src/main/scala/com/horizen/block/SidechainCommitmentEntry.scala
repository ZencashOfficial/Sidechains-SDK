package com.horizen.block

import com.horizen.cryptolibprovider.{FieldElementUtils, InMemoryOptimizedMerkleTreeUtils}
import com.horizen.utils.BytesUtils

import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConverters._

class SidechainCommitmentEntry
{
  private var forwardTransfersHash: Option[Array[Byte]] = None
  private var withdrawalCertificateHash: Option[Array[Byte]] = None
  private val backwardTransferRequestHash: Option[Array[Byte]] = None

  def setForwardTransfersHash(hash: Array[Byte]): Unit = {
    forwardTransfersHash = Some(hash)
  }

  def setWithdrawalCertificateHash(hash: Array[Byte]): Unit = {
    withdrawalCertificateHash = Some(hash)
  }

  def getForwardTransfersHash: Option[Array[Byte]] = forwardTransfersHash

  def getBackwardTransferRequestHash: Option[Array[Byte]] = backwardTransferRequestHash

  def getWCertHash: Option[Array[Byte]] = withdrawalCertificateHash

  def getSidechainCommitmentEntryHash(sidechainId: Array[Byte]): Array[Byte] = {
    SidechainCommitmentEntry.getSidechainCommitmentEntryHash(
      getForwardTransfersHash,
      getBackwardTransferRequestHash,
      getWCertHash,
      sidechainId
    )
  }
}

object SidechainCommitmentEntry
{

  private def getSidechainCommitmentEntryHash(ftsHash: Option[Array[Byte]],
                                              btrsHash: Option[Array[Byte]],
                                              wcertHash: Option[Array[Byte]],
                                              sidechainId: Array[Byte]): Array[Byte] = {
    val singleSidechainComponents: ArrayBuffer[Array[Byte]] = ArrayBuffer()

    // TODO: restore this solution, after the changes on MC side
    /*ftsHash.foreach(singleSidechainComponents.append(_))
    btrsHash.foreach(singleSidechainComponents.append(_))
    wcertHash.foreach(singleSidechainComponents.append(_))*/
    singleSidechainComponents.append(ftsHash.orNull)
    singleSidechainComponents.append(btrsHash.orNull)
    singleSidechainComponents.append(wcertHash.orNull)

    val sidechainIdFE: Array[Byte] = new Array[Byte](FieldElementUtils.maximumFieldElementLength)
    val sidechainIdLE: Array[Byte] = BytesUtils.reverseBytes(sidechainId)
    System.arraycopy(sidechainIdLE, 0, sidechainIdFE, 0, sidechainIdLE.length)

    singleSidechainComponents.append(sidechainIdFE)

    InMemoryOptimizedMerkleTreeUtils.merkleTreeRootHash(singleSidechainComponents.asJava)
  }

  def getSidechainCommitmentEntryHash(proof: SidechainCommitmentEntryProof): Array[Byte] = {
    getSidechainCommitmentEntryHash(proof.ftsHash, proof.btrsHash, proof.wcertHash, proof.sidechainId)
  }
}
