package com.horizen

import java.io.{BufferedReader, BufferedWriter, File, FileReader, FileWriter}
import java.util.Optional
import java.{lang, util}

import com.horizen.box.WithdrawalRequestBox
import com.horizen.box.data.WithdrawalRequestBoxData
import com.horizen.cryptolibprovider.SchnorrFunctions.KeyType
import com.horizen.cryptolibprovider.{CryptoLibProvider, SchnorrFunctionsImplZendoo, ThresholdSignatureCircuitImplZendoo}
import com.horizen.proposition.MCPublicKeyHashProposition
import com.horizen.schnorrnative.SchnorrSecretKey
import com.horizen.utils.BytesUtils
import org.junit.Assert.{assertEquals, assertTrue, fail}
import org.junit.{After, Ignore, Test}
import com.google.common.io.Files

import com.horizen.fixtures.FieldElementFixture

import scala.collection.JavaConverters._
import scala.util.Random

class SigProofTest {
  private val classLoader: ClassLoader = getClass.getClassLoader
  private val sigCircuit: ThresholdSignatureCircuitImplZendoo = new ThresholdSignatureCircuitImplZendoo()
  private val schnorrFunctions: SchnorrFunctionsImplZendoo = new SchnorrFunctionsImplZendoo()

  private val tmpDir: File = Files.createTempDir
  private val provingKeyPath: String = tmpDir.getAbsolutePath + "/snark_pk"
  private val verificationKeyPath: String = tmpDir.getAbsolutePath + "/snark_vk"

  @After
  def cleanup(): Unit = {
    new File(provingKeyPath).delete()
    new File(verificationKeyPath).delete()
    tmpDir.delete()
  }

  // Use this method to regenerate Schnorr PrivateKeys and save them to resources.
  // Note: currently schnorr keys are generated non-deterministically
  private def generateSchnorrPrivateKeys(): Unit = {
    (0 to 9).foreach(index => {
      val bts = schnorrFunctions.generateSchnorrKeys(s"$index".getBytes()).get(KeyType.SECRET)
      val bw = new BufferedWriter(new FileWriter("src/test/resources/schnorr_sk0"+ index + "_hex"))
      bw.write(BytesUtils.toHexString(bts))
      bw.close()
    })
  }

  private def buildSchnorrPrivateKey(index: Int): SchnorrSecretKey = {
    var bytes: Array[Byte] = null
    try {
      val resourceName = "schnorr_sk0"+ index + "_hex"
      val file = new FileReader(classLoader.getResource(resourceName).getFile)
      bytes = BytesUtils.fromHexString(new BufferedReader(file).readLine())
    }
    catch {
      case e: Exception =>
        assertEquals(e.toString(), true, false)
    }

    SchnorrSecretKey.deserialize(bytes)
  }

  //Test will take around 2 minutes, enable for sanity checking of ThresholdSignatureCircuit
  @Ignore
  @Test
  def simpleCheck(): Unit = {
    val keyPairsLen = 7
    val threshold = 5 //hardcoded value

    val keyPairs = (0 until keyPairsLen).view.map(buildSchnorrPrivateKey).map(secret => (secret, secret.getPublicKey))
    val publicKeysBytes: util.List[Array[Byte]] = keyPairs.map(_._2.serializePublicKey()).toList.asJava

    val sysConstant = sigCircuit.generateSysDataConstant(publicKeysBytes, threshold)

    val epochNumber: Int = 10;
    val btrFee: Long = 100;
    val ftMinAmount: Long = 100;
    val endCumulativeScTxCommTreeRoot = FieldElementFixture.generateFieldElement()
    val sidechainId = FieldElementFixture.generateFieldElement()

    val wb: util.List[WithdrawalRequestBox] = Seq(new WithdrawalRequestBox(new WithdrawalRequestBoxData(new MCPublicKeyHashProposition(Array.fill(20)(Random.nextInt().toByte)), 2345), 42)).asJava

    val messageToBeSigned = sigCircuit.generateMessageToBeSigned(wb, sidechainId, epochNumber, endCumulativeScTxCommTreeRoot, btrFee, ftMinAmount)

    val emptySigs = List.fill[Optional[Array[Byte]]](keyPairsLen - threshold)(Optional.empty[Array[Byte]]())
    val signatures: util.List[Optional[Array[Byte]]] = (keyPairs
      .map{case (secret, public) => schnorrFunctions.sign(secret.serializeSecretKey(), public.serializePublicKey(), messageToBeSigned)}
      .map(b => Optional.of(b))
      .take(threshold)
      .toList ++ emptySigs)
      .asJava

    // Setup proving system keys
    println(s"Generating Marlin dlog key.")
    if (!CryptoLibProvider.sigProofThresholdCircuitFunctions.generateCoboundaryMarlinDLogKeys()) {
      fail("Error occurred during dlog key generation.")
    }

    println(s"Generating Marlin snark keys. Path: pk=$provingKeyPath, vk=$verificationKeyPath")
    if (!CryptoLibProvider.sigProofThresholdCircuitFunctions.generateCoboundaryMarlinSnarkKeys(keyPairsLen, provingKeyPath, verificationKeyPath)) {
      fail("Error occurred during snark keys generation.")
    }

    println("Generating snark proof...")
    val proofAndQuality: utils.Pair[Array[Byte], lang.Long] = sigCircuit.createProof(wb, sidechainId, epochNumber, endCumulativeScTxCommTreeRoot,
      btrFee, ftMinAmount, signatures, publicKeysBytes, threshold, provingKeyPath, true, true)

    val result = sigCircuit.verifyProof(wb, sidechainId, epochNumber, endCumulativeScTxCommTreeRoot, btrFee, ftMinAmount, sysConstant,
      proofAndQuality.getValue, proofAndQuality.getKey, true, verificationKeyPath, true)

    assertTrue("Proof verification expected to be successfully", result)
  }

}
