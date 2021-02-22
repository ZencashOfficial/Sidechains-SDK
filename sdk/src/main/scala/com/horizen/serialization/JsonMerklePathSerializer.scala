package com.horizen.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind
import com.fasterxml.jackson.databind.SerializerProvider
import com.horizen.merkletreenative.MerklePath
import com.horizen.utils.BytesUtils

class JsonMerklePathSerializer extends databind.JsonSerializer[MerklePath] {

  override def serialize(merklePath: MerklePath, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
    jsonGenerator.writeString(BytesUtils.toHexString(merklePath.serialize()))
  }
}
class JsonMerklePathOptionSerializer extends databind.JsonSerializer[Option[MerklePath]] {

  override def serialize(merklePathOption: Option[MerklePath], jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
    merklePathOption match {
      case Some(merklePath) =>
        new JsonMerklePathSerializer().serialize(merklePath, jsonGenerator, serializerProvider)
      case None =>
        jsonGenerator.writeStartArray()
        jsonGenerator.writeEndArray()
    }
  }
}
