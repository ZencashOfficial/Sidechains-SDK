package com.horizen

import java.util

import com.horizen.companion.SidechainBoxesCompanion
import scorex.core.serialization.ScorexSerializer
import scorex.core.{NodeViewModifier, bytesToId, idToBytes}
import scorex.util.ModifierId
import scorex.util.serialization.{Reader, Writer}

class OpenedWalletBox (override val box: SidechainTypes#SCB, override val transactionId: ModifierId,
                       override val createdAt: Long, val openTransactionId: ModifierId, val openedAt: Long)
  extends WalletBox (box, transactionId, createdAt)
{
  require(openTransactionId.length == NodeViewModifier.ModifierIdSize * 2,
    "Expected openTransactionId length is %d, actual length is %d".format(NodeViewModifier.ModifierIdSize * 2, openTransactionId.length))
  require(openedAt > 0, "Expected openedAt should be positive value, actual value is %d".format(openedAt))

  def this(walletBox: WalletBox, openTransaction: SidechainTypes#SCBT) = {
    this(walletBox.box, walletBox.transactionId, walletBox.createdAt,
      openTransaction.id, openTransaction.timestamp())
  }

  override def toString: String = s"OpenedWalletBox($box, ${encoder.encode(transactionId)}, $createdAt, " +
    s"${encoder.encode(transactionId)}, $openedAt)"

  override def serializer (sidechainBoxesCompanion: SidechainBoxesCompanion) : WalletBoxSerializer =
    new WalletBoxSerializer(sidechainBoxesCompanion)

  override def hashCode: Int = util.Arrays.hashCode(box.id)

  override def equals(obj: Any): Boolean = {
    obj match {
      case owb: OpenedWalletBox => box.equals(owb.box) && transactionId.equals(owb.transactionId) &&
        createdAt.equals(owb.createdAt) && openTransactionId.equals(owb.openTransactionId) &&
        openedAt.equals(owb.openedAt)
      case _ => false
    }
  }
}

class OpenedWalletBoxSerializer(sidechainBoxesCompanion : SidechainBoxesCompanion)
  extends ScorexSerializer[OpenedWalletBox]
{
  def serialize(openedWalletBox: OpenedWalletBox, writer: Writer): Unit = {
    writer.putBytes(idToBytes(openedWalletBox.transactionId))
    writer.putLong(openedWalletBox.createdAt)
    writer.putBytes(idToBytes(openedWalletBox.openTransactionId))
    writer.putLong(openedWalletBox.openedAt)
    sidechainBoxesCompanion.serialize(openedWalletBox.box, writer)
  }

  def parse(reader: Reader): OpenedWalletBox = {
    val txId = bytesToId(reader.getBytes(NodeViewModifier.ModifierIdSize))
    val createdAt = reader.getLong()
    val otxId = bytesToId(reader.getBytes(NodeViewModifier.ModifierIdSize))
    val openedAt = reader.getLong()
    val box = sidechainBoxesCompanion.parse(reader)
    new OpenedWalletBox(box, txId, createdAt, otxId, openedAt)
  }
}
