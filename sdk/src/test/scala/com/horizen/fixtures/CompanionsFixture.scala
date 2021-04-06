package com.horizen.fixtures

import java.lang.{Byte => JByte}
import java.util.{HashMap => JHashMap}

import cats.instances.byte
import com.horizen.SidechainTypes
import com.horizen.companion.{SidechainBoxesDataCompanion, SidechainProofsCompanion, SidechainSecretsCompanion, SidechainTransactionsCompanion}
import com.horizen.secret.SecretSerializer
import com.horizen.transaction.{RegularTransactionSerializer, TransactionSerializer}

trait CompanionsFixture
{
  def getDefaultTransactionsCompanion: SidechainTransactionsCompanion = {
    val sidechainBoxesDataCompanion = SidechainBoxesDataCompanion(new JHashMap())
    val sidechainProofsCompanion = SidechainProofsCompanion(new JHashMap())

    SidechainTransactionsCompanion(new JHashMap[JByte, TransactionSerializer[SidechainTypes#SCBT]](){{
      put(111.byteValue(), RegularTransactionSerializer.getSerializer.asInstanceOf[TransactionSerializer[SidechainTypes#SCBT]])
    }}, sidechainBoxesDataCompanion, sidechainProofsCompanion)
  }

  def getTransactionsCompanionWithCustomTransactions(customSerializers: JHashMap[JByte, TransactionSerializer[SidechainTypes#SCBT]]): SidechainTransactionsCompanion = {
    val sidechainBoxesDataCompanion = SidechainBoxesDataCompanion(new JHashMap())
    val sidechainProofsCompanion = SidechainProofsCompanion(new JHashMap())

    SidechainTransactionsCompanion(customSerializers, sidechainBoxesDataCompanion, sidechainProofsCompanion)
  }

  def getDefaultSecretCompanion: SidechainSecretsCompanion = {
    SidechainSecretsCompanion(new JHashMap[JByte, SecretSerializer[SidechainTypes#SCS]]())
  }
}

class CompanionsFixtureClass extends CompanionsFixture