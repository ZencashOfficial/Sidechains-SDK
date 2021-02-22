package com.horizen.block

import com.fasterxml.jackson.annotation.{JsonProperty, JsonView}
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.horizen.merkletreenative.MerklePath
import com.horizen.serialization.{JsonMerklePathSerializer, Views}
import scorex.core.serialization.{BytesSerializable, ScorexSerializer}
import scorex.util.serialization.{Reader, Writer}

@JsonView(Array(classOf[Views.Default]))
case class SidechainCommitmentEntryProof(sidechainId: Array[Byte],
                                         ftsHash: Option[Array[Byte]],
                                         btrsHash: Option[Array[Byte]],
                                         wcertHash: Option[Array[Byte]],
                                         @JsonProperty("merklePath")
                                         @JsonSerialize(using = classOf[JsonMerklePathSerializer])
                                         merklePath: MerklePath // TODO: store as bytes
                                        )
  extends BytesSerializable
{
  override type M = SidechainCommitmentEntryProof

  override def serializer: ScorexSerializer[SidechainCommitmentEntryProof] = SidechainCommitmentEntryProofSerializer
}

object SidechainCommitmentEntryProofSerializer extends ScorexSerializer[SidechainCommitmentEntryProof]
{
  override def serialize(proof: SidechainCommitmentEntryProof, w: Writer): Unit = {
    w.putBytes(proof.sidechainId)
    w.putOption(proof.ftsHash){ case (writer: Writer, bytes: Array[Byte]) => writer.putBytes(bytes) }
    w.putOption(proof.btrsHash){ case (writer: Writer, bytes: Array[Byte]) => writer.putBytes(bytes) }
    w.putOption(proof.wcertHash){ case (writer: Writer, bytes: Array[Byte]) => writer.putBytes(bytes) }
    w.putInt(proof.merklePath.serialize().length)
    w.putBytes(proof.merklePath.serialize())
  }

  override def parse(r: Reader): SidechainCommitmentEntryProof = {
    val sidechainId = r.getBytes(32)
    val ftsHash = r.getOption(r.getBytes(32))
    val btrsHash = r.getOption(r.getBytes(32))
    val wcertHash = r.getOption(r.getBytes(32))
    val merklePathSize = r.getInt()
    val merklePath = MerklePath.deserialize(r.getBytes(merklePathSize))

    SidechainCommitmentEntryProof(sidechainId, ftsHash, btrsHash, wcertHash, merklePath)
  }
}
