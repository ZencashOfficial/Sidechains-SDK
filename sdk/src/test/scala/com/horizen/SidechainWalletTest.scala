package com.horizen

import java.util

import scorex.core.{VersionTag, bytesToId}
import com.horizen.box._
import com.horizen.companion._
import com.horizen.customtypes._
import com.horizen.fixtures._
import com.horizen.proposition._
import com.horizen.utils.{ByteArrayWrapper, BytesUtils}
import com.horizen.utils.Pair
import java.util.{ArrayList => JArrayList, HashMap => JHashMap, List => JList}
import java.lang.{Byte => JByte}

import com.horizen.block.SidechainBlock
import com.horizen.secret.{PrivateKey25519, PrivateKey25519Creator, Secret, SecretSerializer}
import com.horizen.storage.{SidechainOpenedWalletBoxStorage, SidechainSecretStorage, SidechainWalletBoxStorage, SidechainWalletTransactionStorage, Storage}
import com.horizen.transaction.{BoxTransaction, RegularTransaction, TransactionSerializer}
import com.horizen.wallet.ApplicationWallet
import org.junit.Assert._

import scala.collection.mutable.ListBuffer
import org.junit._
import org.scalatest.junit.JUnitSuite
import org.scalatest.mockito._

import scala.collection.JavaConverters._
import org.mockito._
import scorex.crypto.hash.Blake2b256

import scala.util.{Failure, Random, Try}

class SidechainWalletTest
  extends JUnitSuite
    with SecretFixture
    with TransactionFixture
    with IODBStoreFixture
    with MockitoSugar
{
  val mockedBoxStorage : Storage = mock[Storage]
  val mockedSecretStorage : Storage = mock[Storage]
  val mockedTransactionStorage : Storage = mock[Storage]
  val mockedOpenedWalletBoxStorage : Storage = mock[Storage]
  val mockedBoxOperationStorage : Storage = mock[Storage]

  val boxList = new ListBuffer[WalletBox]()
  val storedBoxList = new ListBuffer[Pair[ByteArrayWrapper, ByteArrayWrapper]]()
  val boxVersions = new ListBuffer[ByteArrayWrapper]()

  val secretList = new ListBuffer[Secret]()
  val storedSecretList = new ListBuffer[Pair[ByteArrayWrapper, ByteArrayWrapper]]()
  val secretVersions = new ListBuffer[ByteArrayWrapper]()

  val storedTransactionList = new ListBuffer[Pair[ByteArrayWrapper, ByteArrayWrapper]]()

  var customBoxesSerializers: JHashMap[JByte, BoxSerializer[SidechainTypes#SCB]] = new JHashMap()
  customBoxesSerializers.put(CustomBox.BOX_TYPE_ID, CustomBoxSerializer.getSerializer.asInstanceOf[BoxSerializer[SidechainTypes#SCB]])
  val sidechainBoxesCompanion = SidechainBoxesCompanion(customBoxesSerializers)

  val customSecretSerializers: JHashMap[JByte, SecretSerializer[SidechainTypes#SCS]] = new JHashMap()
  customSecretSerializers.put(CustomPrivateKey.SECRET_TYPE_ID, CustomPrivateKeySerializer.getSerializer.asInstanceOf[SecretSerializer[SidechainTypes#SCS]])
  val sidechainSecretsCompanion = SidechainSecretsCompanion(customSecretSerializers)

  val sidechainTransactionsCompanion = SidechainTransactionsCompanion(new JHashMap[JByte, TransactionSerializer[SidechainTypes#SCBT]]())

  @Before
  def setUp() : Unit = {

    // Set base Secrets data
    secretList ++= getSecretList(5).asScala
    secretVersions += getVersion

    for (s <- secretList) {
      storedSecretList.append({
        val key = new ByteArrayWrapper(Blake2b256.hash(s.publicImage().bytes))
        val value = new ByteArrayWrapper(sidechainSecretsCompanion.toBytes(s))
        new Pair(key, value)
      })
    }


    // Mock get and update methods of SecretStorage
    Mockito.when(mockedSecretStorage.getAll).thenReturn(storedSecretList.asJava)

    Mockito.when(mockedSecretStorage.get(ArgumentMatchers.any[ByteArrayWrapper]()))
      .thenAnswer(answer => {
        storedSecretList.filter(_.getKey.equals(answer.getArgument(0)))
      })

    Mockito.when(mockedSecretStorage.get(ArgumentMatchers.anyList[ByteArrayWrapper]()))
      .thenAnswer(answer => {
        storedSecretList.filter(p => answer.getArgument(0).asInstanceOf[JList[ByteArrayWrapper]].contains(p.getKey))
      })

    Mockito.when(mockedSecretStorage.update(ArgumentMatchers.any[ByteArrayWrapper](),
        ArgumentMatchers.anyList[Pair[ByteArrayWrapper,ByteArrayWrapper]](),
        ArgumentMatchers.anyList[ByteArrayWrapper]()))
      .thenAnswer(answer => {
        secretVersions.append(answer.getArgument(0))
        for (s <- answer.getArgument(2).asInstanceOf[JList[ByteArrayWrapper]].asScala) {
          val index = storedSecretList.indexWhere(p => p.getKey.equals(s))
          if (index != -1)
            storedSecretList.remove(index)
        }
        for (s <- answer.getArgument(1).asInstanceOf[JList[Pair[ByteArrayWrapper,ByteArrayWrapper]]].asScala) {
          val index = storedSecretList.indexWhere(p => p.getKey.equals(s.getKey))
          if (index != -1)
            storedSecretList.remove(index)
        }
        storedSecretList.appendAll(answer.getArgument(1).asInstanceOf[JList[Pair[ByteArrayWrapper,ByteArrayWrapper]]].asScala)
      })


    // Set base WalletBox data
    boxList ++= getWalletBoxList(getRegularBoxList(secretList.asJava)).asScala
    boxVersions += getVersion

    for (b <- boxList) {
      storedBoxList.append({
        val wbs = new WalletBoxSerializer(sidechainBoxesCompanion)
        val key = new ByteArrayWrapper(Blake2b256.hash(b.box.id()))
        val value = new ByteArrayWrapper(wbs.toBytes(b))
        new Pair(key,value)
      })
    }


    // Mock get and update methods of BoxStorage
    Mockito.when(mockedBoxStorage.getAll).thenReturn(storedBoxList.asJava)

    Mockito.when(mockedBoxStorage.get(ArgumentMatchers.any[ByteArrayWrapper]()))
      .thenAnswer(answer => {
        storedBoxList.filter(_.getKey.equals(answer.getArgument(0)))
      })

    Mockito.when(mockedBoxStorage.get(ArgumentMatchers.anyList[ByteArrayWrapper]()))
      .thenAnswer(answer => {
        storedBoxList.filter(p => answer.getArgument(0).asInstanceOf[JList[ByteArrayWrapper]].contains(p.getKey))
      })

    Mockito.when(mockedBoxStorage.update(ArgumentMatchers.any[ByteArrayWrapper](),
      ArgumentMatchers.anyList[Pair[ByteArrayWrapper,ByteArrayWrapper]](),
      ArgumentMatchers.anyList[ByteArrayWrapper]()))
      .thenAnswer(answer => {
        boxVersions.append(answer.getArgument(0))
        for (s <- answer.getArgument(2).asInstanceOf[JList[ByteArrayWrapper]].asScala) {
          storedBoxList.remove(storedBoxList.indexWhere(p => p.getKey.equals(s)))
        }
        for (s <- answer.getArgument(1).asInstanceOf[JList[Pair[ByteArrayWrapper,ByteArrayWrapper]]].asScala) {
          val index = storedBoxList.indexWhere(p => p.getKey.equals(s.getKey))
          if (index != -1)
            storedBoxList.remove(index)
        }
        storedBoxList.appendAll(answer.getArgument(1).asInstanceOf[JList[Pair[ByteArrayWrapper,ByteArrayWrapper]]].asScala)
      })


  }

  @Test
  def testScanPersistent(): Unit = {
    val mockedWalletBoxStorage1: SidechainWalletBoxStorage = mock[SidechainWalletBoxStorage]
    val mockedSecretStorage1: SidechainSecretStorage = mock[SidechainSecretStorage]
    val mockedWalletTransactionStorage1: SidechainWalletTransactionStorage = mock[SidechainWalletTransactionStorage]
    val mockedOpenedWalletBoxStorage1: SidechainOpenedWalletBoxStorage = mock[SidechainOpenedWalletBoxStorage]
    val mockedApplicationWallet: ApplicationWallet = mock[ApplicationWallet]
    val sidechainWallet = new SidechainWallet("seed".getBytes, mockedWalletBoxStorage1, mockedSecretStorage1,
      mockedWalletTransactionStorage1, mockedOpenedWalletBoxStorage1, mockedApplicationWallet)

    // Prepare list of transactions:
    /*
    val secret1 = PrivateKey25519Creator.getInstance.generateSecret("seed1".getBytes())
    val secret2 = PrivateKey25519Creator.getInstance.generateSecret("seed2".getBytes())
    val secret3 = PrivateKey25519Creator.getInstance.generateSecret("seed3".getBytes())
    val secret4 = PrivateKey25519Creator.getInstance.generateSecret("seed4".getBytes())
    */

    val transaction1 = getRegularTransaction(boxList.slice(0, 3).map(_.box.asInstanceOf[RegularBox]),
      secretList.map(_.asInstanceOf[PrivateKey25519]), secretList.slice(4, 5).map(_.publicImage().asInstanceOf[PublicKey25519Proposition]))
    val transaction2 = getRegularTransaction(boxList.slice(3, 5).map(_.box.asInstanceOf[RegularBox]),
      secretList.map(_.asInstanceOf[PrivateKey25519]), Seq(secretList(1), secretList(3)).map(_.publicImage().asInstanceOf[PublicKey25519Proposition]))

    // create mocked SidechainBlock
    val blockId = new Array[Byte](32)
    Random.nextBytes(blockId)
    val mockedBlock : SidechainBlock = mock[SidechainBlock]

    Mockito.when(mockedBlock.transactions)
      .thenReturn(Seq(
        transaction1.asInstanceOf[BoxTransaction[Proposition, Box[Proposition]]],
        transaction2.asInstanceOf[BoxTransaction[Proposition, Box[Proposition]]]
      ))

    Mockito.when(mockedBlock.id)
      .thenReturn(bytesToId(blockId))

    // Prepare mockedSecretStorage1 Secrets
    Mockito.when(mockedSecretStorage1.getAll).thenReturn(secretList.toList)


    // Test:
    // Prepare what we expect to receive for WalletBoxStorage.update
    Mockito.when(mockedWalletBoxStorage1.update(
      ArgumentMatchers.any[ByteArrayWrapper](),
      ArgumentMatchers.any[List[WalletBox]](),
      ArgumentMatchers.any[List[Array[Byte]]]()))
      .thenAnswer(answer => {
        val version = answer.getArgument(0).asInstanceOf[ByteArrayWrapper]
        val walletBoxUpdateList = answer.getArgument(1).asInstanceOf[List[WalletBox]]
        val boxIdsRemoveList = answer.getArgument(2).asInstanceOf[List[Array[Byte]]]

        // check
        assertEquals("ScanPersistent on WalletBoxStorage.update(...) actual version is wrong.", new ByteArrayWrapper(blockId), version)
        assertEquals("ScanPersistent on WalletBoxStorage.update(...) actual walletBoxUpdateList is wrong.", List(
          new WalletBox(transaction1.newBoxes().get(0), transaction1.id, transaction1.timestamp()),
          new WalletBox(transaction2.newBoxes().get(0), transaction2.id, transaction2.timestamp()),
          new WalletBox(transaction2.newBoxes().get(1), transaction2.id, transaction2.timestamp())
        ), walletBoxUpdateList)

        assertEquals("ScanPersistent on WalletBoxStorage.update(...) actual boxIdsRemoveList is wrong.", List(
          new ByteArrayWrapper(transaction1.unlockers().get(0).closedBoxId()),
          new ByteArrayWrapper(transaction1.unlockers().get(1).closedBoxId()),
          new ByteArrayWrapper(transaction1.unlockers().get(2).closedBoxId()),
          new ByteArrayWrapper(transaction2.unlockers().get(0).closedBoxId()),
          new ByteArrayWrapper(transaction2.unlockers().get(1).closedBoxId())
        ), boxIdsRemoveList.map(new ByteArrayWrapper(_)))

        Try {
          mockedWalletBoxStorage1
        }
      })

    Mockito.when(mockedWalletBoxStorage1.getAll)
        .thenReturn(boxList.toList)

    // Prepare what we expect to receive for ApplicationWallet.onChangeBoxes
    Mockito.when(mockedApplicationWallet.onChangeBoxes(
      ArgumentMatchers.any[Array[Byte]](),
      ArgumentMatchers.anyList[SidechainTypes#SCB](),
      ArgumentMatchers.anyList[Array[Byte]]()))
      .thenAnswer(answer => {
        val version = answer.getArgument(0).asInstanceOf[Array[Byte]]
        val boxesToUpdate = answer.getArgument(1).asInstanceOf[JList[SidechainTypes#SCB]]
        val boxIdsToRemove = answer.getArgument(2).asInstanceOf[JList[Array[Byte]]].asScala.map(new ByteArrayWrapper(_)).toList.asJava

        // check
        assertEquals("ScanPersistent on ApplicationWallet.onChangeBoxes(...) actual version is wrong.",
          new ByteArrayWrapper(blockId),
          new ByteArrayWrapper(version))

        assertEquals("ScanPersistent on ApplicationWallet.onChangeBoxes(...) actual boxesToUpdate list is wrong.", util.Arrays.asList(
          transaction1.newBoxes().get(0),
          transaction2.newBoxes().get(0),
          transaction2.newBoxes().get(1)
        ), boxesToUpdate)

        assertEquals("ScanPersistent on ApplicationWallet.onChangeBoxes(...) actual boxIdsToRemove list is wrong.", util.Arrays.asList(
          new ByteArrayWrapper(transaction1.unlockers().get(0).closedBoxId()),
          new ByteArrayWrapper(transaction1.unlockers().get(1).closedBoxId()),
          new ByteArrayWrapper(transaction1.unlockers().get(2).closedBoxId()),
          new ByteArrayWrapper(transaction2.unlockers().get(0).closedBoxId()),
          new ByteArrayWrapper(transaction2.unlockers().get(1).closedBoxId())
        ), boxIdsToRemove)

      })

    Mockito.when(mockedWalletTransactionStorage1.update(
      ArgumentMatchers.any[ByteArrayWrapper](),
      ArgumentMatchers.any[List[SidechainTypes#SCBT]]()))
      .thenAnswer(answer => {
        val version = answer.getArgument(0).asInstanceOf[ByteArrayWrapper]
        val transactionUpdateList = answer.getArgument(1).asInstanceOf[List[SidechainTypes#SCBT]]

        assertEquals("ScanPersistent on WalletTransactionStorage.update(...) actual version is wrong.", new ByteArrayWrapper(blockId), version)

        assertEquals("ScanPersistent on WalletTransactionStorage.update(...) actual transactionUpdateList list is wrong.",
          List(transaction1, transaction2),
          transactionUpdateList)

        Try {
          mockedWalletTransactionStorage1
        }
      })

    sidechainWallet.scanPersistent(mockedBlock)
  }

  @Test
  def testRollback(): Unit = {
    val mockedWalletBoxStorage1: SidechainWalletBoxStorage = mock[SidechainWalletBoxStorage]
    val mockedSecretStorage1: SidechainSecretStorage = mock[SidechainSecretStorage]
    val mockedWalletTransactionStorage1: SidechainWalletTransactionStorage = mock[SidechainWalletTransactionStorage]
    val mockedOpenedWalletBoxStorage1: SidechainOpenedWalletBoxStorage = mock[SidechainOpenedWalletBoxStorage]
    val mockedApplicationWallet: ApplicationWallet = mock[ApplicationWallet]
    val sidechainWallet = new SidechainWallet("seed".getBytes(), mockedWalletBoxStorage1, mockedSecretStorage1,
      mockedWalletTransactionStorage1, mockedOpenedWalletBoxStorage1, mockedApplicationWallet)

    val expectedException = new IllegalArgumentException("on rollback exception")
    var rollbackEventOccurred = false

    // Prepare block ID and corresponding version
    val blockId = new Array[Byte](32)
    Random.nextBytes(blockId)
    val versionTag: VersionTag = VersionTag @@ BytesUtils.toHexString(blockId)

    // Prepare what we expect to receive in WalletBoxStorage.rollback
    Mockito.when(mockedWalletBoxStorage1.rollback(
      ArgumentMatchers.any[ByteArrayWrapper]()))
      // For Test 1:
      .thenAnswer(answer => {
        val version = answer.getArgument(0).asInstanceOf[ByteArrayWrapper]

        assertEquals("Rollback on WalletBoxStorage.rollback(...) actual version is wrong.",
          new ByteArrayWrapper(blockId),
          version)
      })
      // For Test 2:
        .thenAnswer(answer => throw expectedException)

    // Prepare what we expect to receive in ApplicationWallet.onRollback
    Mockito.when(mockedApplicationWallet.onRollback(
      ArgumentMatchers.any[Array[Byte]]()))
      // For Test 1:
      .thenAnswer(answer => {
        val version = answer.getArgument(0).asInstanceOf[Array[Byte]]

        assertEquals("Rollback on ApplicationWallet.onRollback(...) actual version is wrong.",
          new ByteArrayWrapper(blockId),
          new ByteArrayWrapper(version))
      })
      // For Test 2:
      .thenAnswer(answer => rollbackEventOccurred = true)


    // Test 1: successful rollback
    sidechainWallet.rollback(versionTag)


    // Test 2: failed to rollback, WalletBoxStorage.rollback(...) threw an exception
    val res = sidechainWallet.rollback(versionTag)
    assertTrue("SidechainWallet failure expected during rollback.", res.isFailure)
    assertEquals("SidechainWallet different exception expected during rollback.", expectedException, res.failed.get)
    assertFalse("ApplicationWallet onRollback(...) event NOT expected.", rollbackEventOccurred)
  }

  @Test
  def testScanPersistentIntegration() : Unit = {
    val mockedBlock : SidechainBlock = mock[SidechainBlock]
    val blockId = Array[Byte](32)
    val from : JList[Pair[RegularBox, PrivateKey25519]] = new JArrayList()
    val to : JList[Pair[PublicKey25519Proposition, java.lang.Long]]= new JArrayList()
    val withdrawalRequests : JList[Pair[MCPublicKeyHashProposition, java.lang.Long]]= new JArrayList()

    Random.nextBytes(blockId)

    for (i <- 0 to 2)
      from.add(new Pair(boxList(i).box.asInstanceOf[RegularBox], secretList(i).asInstanceOf[PrivateKey25519]))

    for (i <- 0 to 2)
      to.add(new Pair(secretList(i).publicImage().asInstanceOf[PublicKey25519Proposition], 10L))

    val tx : RegularTransaction = RegularTransaction.create(from, to, withdrawalRequests, 10L, 1547798549470L)

    Mockito.when(mockedBlock.transactions)
      .thenReturn(Seq(tx.asInstanceOf[BoxTransaction[Proposition, Box[Proposition]]]))

    Mockito.when(mockedBlock.id)
      .thenReturn(bytesToId(blockId))

    val sidechainWallet = new SidechainWallet("seed".getBytes,
      new SidechainWalletBoxStorage(mockedBoxStorage, sidechainBoxesCompanion),
      new SidechainSecretStorage(mockedSecretStorage, sidechainSecretsCompanion),
      new SidechainWalletTransactionStorage(mockedTransactionStorage, sidechainTransactionsCompanion),
      new SidechainOpenedWalletBoxStorage(mockedOpenedWalletBoxStorage, sidechainBoxesCompanion),
      new CustomApplicationWallet())

    sidechainWallet.scanPersistent(mockedBlock)

    val wbl = sidechainWallet.boxes()

    assertEquals("Wallet must contain specified count of WallectBoxes.", boxList.size, wbl.size)
    assertTrue("Wallet must contain all specified WalletBoxes.",
      wbl.asJavaCollection.containsAll(boxList.slice(3, 5).asJavaCollection))
    assertTrue("Wallet must contain all specified WalletBoxes.",
      wbl.map(_.box).asJavaCollection.containsAll(tx.newBoxes))

  }

  @Test
  def testSecrets(): Unit = {
    val mockedWalletBoxStorage1: SidechainWalletBoxStorage = mock[SidechainWalletBoxStorage]
    val mockedSecretStorage1: SidechainSecretStorage = mock[SidechainSecretStorage]
    val mockedWalletTransactionStorage1: SidechainWalletTransactionStorage = mock[SidechainWalletTransactionStorage]
    val mockedOpenedWalletBoxStorage1: SidechainOpenedWalletBoxStorage = mock[SidechainOpenedWalletBoxStorage]
    val mockedApplicationWallet: ApplicationWallet = mock[ApplicationWallet]
    val sidechainWallet = new SidechainWallet("seed".getBytes(), mockedWalletBoxStorage1, mockedSecretStorage1,
      mockedWalletTransactionStorage1, mockedOpenedWalletBoxStorage1, mockedApplicationWallet)
    val secret1 = getSecret("testSeed1".getBytes())
    val secret2 = getSecret("testSeed2".getBytes())


    // Test 1: test secret(proposition) and secretByPublicKey(proposition)
    Mockito.when(mockedSecretStorage1.get(secret1.publicImage())).thenReturn(Some(secret1))

    var actualSecret = sidechainWallet.secret(secret1.publicImage()).get
    assertEquals("SidechainWallet failed to retrieve a proper Secret.", secret1, actualSecret)

    actualSecret = sidechainWallet.secretByPublicKey(secret1.publicImage()).get
    assertEquals("SidechainWallet failed to retrieve a proper Secret.", secret1, actualSecret)


    // Test 2: test secrets(), publicKeys(), allSecrets(), secretsOfType(type)
    Mockito.when(mockedSecretStorage1.getAll).thenReturn(List(secret1, secret2))

    val actualSecrets = sidechainWallet.secrets()
    assertEquals("SidechainWallet failed to retrieve a proper Secrets.", Set(secret1, secret2), actualSecrets)

    val actualPublicKeys = sidechainWallet.publicKeys()
    assertEquals("SidechainWallet failed to retrieve a proper Public Keys.", Set(secret1.publicImage(), secret2.publicImage()), actualPublicKeys)

    val actualSecretsJava = sidechainWallet.allSecrets()
    assertEquals("SidechainWallet failed to retrieve a proper Secrets.", util.Arrays.asList(secret1, secret2), actualSecretsJava)

    var actualPrivateKeysJava = sidechainWallet.secretsOfType(classOf[PrivateKey25519])
    assertEquals("SidechainWallet failed to retrieve a proper Secrets.", util.Arrays.asList(secret1, secret2), actualPrivateKeysJava)

    var actualCustomKeysJava = sidechainWallet.secretsOfType(classOf[CustomPrivateKey])
    assertEquals("SidechainWallet failed to retrieve a proper Secrets.", util.Arrays.asList(), actualCustomKeysJava)


    // Test 3: try to add new valid Secret
    // ApplicationWallet.onAddSecret() event expected
    var onAddSecretEvent: Boolean = false
    Mockito.when(mockedSecretStorage1.add(secret1)).thenReturn(Try{mockedSecretStorage1})
    Mockito.when(mockedApplicationWallet.onAddSecret(secret1)).thenAnswer(_ => onAddSecretEvent = true)

    var result: Boolean = sidechainWallet.addSecret(secret1).isSuccess
    assertTrue("SidechainWallet failed to add new Secret.", result)
    assertTrue("ApplicationWallet onAddSecret() event should be emitted.", onAddSecretEvent)


    // Test 4: try to add null Secret
    // ApplicationWallet.onAddSecret() event NOT expected
    onAddSecretEvent = false
    assertTrue("SidechainWallet failure expected during adding NULL Secret.", sidechainWallet.addSecret(null).isFailure)
    assertFalse("ApplicationWallet onAddSecret() event should NOT be emitted.", onAddSecretEvent)


    // Test 5: try to add some Secret, exception in SecretStorage occurred.
    // ApplicationWallet.onAddSecret() event NOT expected
    var expectedException = new IllegalArgumentException("on add exception")
    Mockito.when(mockedSecretStorage1.add(secret1)).thenReturn(Failure(expectedException))

    var failureResult = sidechainWallet.addSecret(secret1)
    assertTrue("SidechainWallet failure expected during adding new Secret.", failureResult.isFailure)
    assertEquals("SidechainWallet different exception expected during adding new Secret.", expectedException, failureResult.failed.get)
    assertFalse("ApplicationWallet onAddSecret() event should NOT be emitted.", onAddSecretEvent)


    // Test 6: try to remove valid Secret
    // ApplicationWallet.onRemoveSecret() event expected
    var onRemoveSecretEvent: Boolean = false
    Mockito.when(mockedSecretStorage1.remove(secret1.publicImage())).thenReturn(Try{mockedSecretStorage1})
    Mockito.when(mockedApplicationWallet.onRemoveSecret(secret1.publicImage())).thenAnswer(_ => onRemoveSecretEvent = true)
    result = sidechainWallet.removeSecret(secret1.publicImage()).isSuccess
    assertTrue("SidechainWallet failed to remove Secret.", result)
    assertTrue("ApplicationWallet onRemoveSecret() event should be emitted.", onRemoveSecretEvent)


    // Test 7: try to remove null Secret
    // ApplicationWallet.onRemoveSecret() event NOT expected
    onRemoveSecretEvent = false
    assertTrue("SidechainWallet failure expected during removing NULL Secret.", sidechainWallet.removeSecret(null).isFailure)
    assertFalse("ApplicationWallet onRemoveSecret() event should NOT be emitted.", onRemoveSecretEvent)


    // Test 8: try to remove some Secret, exception in SecretStorage occurred.
    // ApplicationWallet.onRemoveSecret() event NOT expected
    onRemoveSecretEvent = false
    expectedException = new IllegalArgumentException("on remove exception")
    Mockito.when(mockedSecretStorage1.remove(secret1.publicImage())).thenReturn(Failure(expectedException))

    failureResult = sidechainWallet.removeSecret(secret1.publicImage())
    assertTrue("SidechainWallet failure expected during removing new Secret.", failureResult.isFailure)
    assertEquals("SidechainWallet different exception expected during removing new Secret.", expectedException, failureResult.failed.get)
    assertFalse("ApplicationWallet onRemoveSecret() event should NOT be emitted.", onRemoveSecretEvent)
  }

  @Deprecated
  @Test
  def testSecretsDeprecated() : Unit = {

    val sidechainWallet = new SidechainWallet("seed".getBytes,
      new SidechainWalletBoxStorage(mockedBoxStorage, sidechainBoxesCompanion),
      new SidechainSecretStorage(mockedSecretStorage, sidechainSecretsCompanion),
      new SidechainWalletTransactionStorage(mockedTransactionStorage, sidechainTransactionsCompanion),
      new SidechainOpenedWalletBoxStorage(mockedOpenedWalletBoxStorage, sidechainBoxesCompanion),
      new CustomApplicationWallet())

    //TEST for - Wallet.secrets
    val sl = sidechainWallet.secrets()

    assertEquals("Wallet must contain specified count of Secrets.", secretList.size, sl.size)
    assertTrue("Wallet must contain all specified Secrets", sl.asJavaCollection.containsAll(secretList.asJavaCollection))

    //TEST for - Wallet.publicKeys
    val pkl = sidechainWallet.publicKeys()

    assertEquals("Wallet must contain specified count of public keys.", secretList.size, pkl.size)
    assertTrue("Wallet must contain public keys for all specified Secrets.",
      pkl.asJavaCollection.containsAll(secretList.map(_.publicImage()).asJavaCollection))

    //TEST for - Wallet.secret(publicImage)
    assertEquals("Wallet must contain specified Secret.", secretList(0), sidechainWallet.secret(secretList(0).publicImage()).get)

    //TEST for - Wallet.removeSecret
    var res = sidechainWallet.removeSecret(secretList(0).publicImage())
    assertTrue("Wallet successful Secret remove expected, instead exception occurred:\n %s".format(if(res.isFailure) res.failed.get.getMessage else ""), res.isSuccess)
    assertTrue("Wallet must not contain specified Secret.", sidechainWallet.secret(secretList(0).publicImage()).isEmpty)

    //TEST for - Wallet.addSecret
    val s = getCustomSecret()

    res = sidechainWallet.addSecret(s)
    assertTrue("Wallet successful Secret add expected, instead exception occurred:\n %s".format(if(res.isFailure) res.failed.get.getMessage else ""), res.isSuccess)
    assertEquals("Wallet must contain specified Secret.", s, sidechainWallet.secret(s.publicImage()).get)

    //TEST for - NodeWallet.secretsOfType
    assertTrue("Wallet must contain all Secrets of specified type.",
      sidechainWallet.secretsOfType(classOf[PrivateKey25519]).containsAll(secretList.slice(1, 5).asJavaCollection))
    assertTrue("Wallet must contain all Secrets of specified type.",
      sidechainWallet.secretsOfType(classOf[CustomPrivateKey]).contains(s))

    //TEST for - NodeWallet.allSecrets
    val sl1 = sidechainWallet.allSecrets()
    assertTrue("Wallet must contain all Secrets.", secretList.slice(1,5).+=(s).asJavaCollection.containsAll(sl1))
  }

  @Test
  def testWalletBoxes(): Unit = {
    val mockedWalletBoxStorage1: SidechainWalletBoxStorage = mock[SidechainWalletBoxStorage]
    val mockedSecretStorage1: SidechainSecretStorage = mock[SidechainSecretStorage]
    val mockedWalletTransactionStorage1: SidechainWalletTransactionStorage = mock[SidechainWalletTransactionStorage]
    val mockedOpenedWalletBoxStorage1: SidechainOpenedWalletBoxStorage = mock[SidechainOpenedWalletBoxStorage]
    val sidechainWallet = new SidechainWallet("seed".getBytes(),
      mockedWalletBoxStorage1, mockedSecretStorage1, mockedWalletTransactionStorage1,
      mockedOpenedWalletBoxStorage1, new CustomApplicationWallet())
    val walletBoxRegular1 = getWalletBox(classOf[RegularBox])
    val walletBoxRegular2 = getWalletBox(classOf[RegularBox])
    val walletBoxCustom = getWalletBox(classOf[RegularBox])


    // Test 1: test test boxes(), allBoxes(), allBoxes(boxIdsToExclude)
    Mockito.when(mockedWalletBoxStorage1.getAll).thenReturn(List(walletBoxRegular1, walletBoxRegular2, walletBoxCustom))

    val actualBoxes = sidechainWallet.boxes()
    assertEquals("SidechainWallet failed to retrieve a proper Boxes.",
      List(walletBoxRegular1, walletBoxRegular2, walletBoxCustom), actualBoxes)

    val actualBoxesJava = sidechainWallet.allBoxes
    assertEquals("SidechainWallet failed to retrieve a proper Boxes.",
      util.Arrays.asList(walletBoxRegular1.box, walletBoxRegular2.box, walletBoxCustom.box), actualBoxesJava)

    // exclude id of walletBoxRegular1
    val actualBoxesWithExcludeJava = sidechainWallet.allBoxes(util.Arrays.asList(walletBoxRegular1.box.id()))
    assertEquals("SidechainWallet failed to retrieve a proper Boxes with excluded ids.",
      util.Arrays.asList(walletBoxRegular2.box, walletBoxCustom.box), actualBoxesWithExcludeJava)


    // Test 2: test boxesOfType(type) and boxesOfType(type, boxIdsToExclude)
    Mockito.when(mockedWalletBoxStorage1.getByType(classOf[RegularBox])).thenReturn(List(walletBoxRegular1, walletBoxRegular2))

    val actualBoxesByTypeJava = sidechainWallet.boxesOfType(classOf[RegularBox])
    assertEquals("SidechainWallet failed to retrieve a proper Boxes of type RegularBox.",
      util.Arrays.asList(walletBoxRegular1.box, walletBoxRegular2.box), actualBoxesByTypeJava)

    val actualBoxesByTypeWithExcludeJava = sidechainWallet.boxesOfType(classOf[RegularBox], util.Arrays.asList(walletBoxRegular1.box.id()))
    assertEquals("SidechainWallet failed to retrieve a proper Boxes of type RegularBox with excluded ids.",
      util.Arrays.asList(walletBoxRegular2.box), actualBoxesByTypeWithExcludeJava)


    // Test 3: test boxesBalance(type)
    val balance = 100L
    Mockito.when(mockedWalletBoxStorage1.getBoxesBalance(classOf[RegularBox])).thenReturn(balance)

    val actualBalance = sidechainWallet.boxesBalance(classOf[RegularBox])
    assertEquals("SidechainWallet failed to retrieve a proper balance for type RegularBox.", balance, actualBalance)
  }

  @Deprecated
  @Test
  def testWalletBoxesDeprecated() : Unit = {

    val sidechainWallet = new SidechainWallet("seed".getBytes(), new SidechainWalletBoxStorage(mockedBoxStorage, sidechainBoxesCompanion),
      new SidechainSecretStorage(mockedSecretStorage, sidechainSecretsCompanion),
      new SidechainWalletTransactionStorage(mockedTransactionStorage, sidechainTransactionsCompanion),
      new SidechainOpenedWalletBoxStorage(mockedOpenedWalletBoxStorage, sidechainBoxesCompanion),
      new CustomApplicationWallet())

    //TEST for - Wallet.boxes
    val wbl = sidechainWallet.boxes()

    assertEquals("Wallet must contain specified count of WallectBoxes.", boxList.size, wbl.size)
    assertTrue("Wallet must contain all specified WalletBoxes.", wbl.asJavaCollection.containsAll(boxList.asJavaCollection))

    //TEST for - NodeWallet.allboxes
    var bl = sidechainWallet.allBoxes

    assertEquals("Wallet must contain specified count of Boxes.", boxList.size, bl.size)
    assertTrue("Wallet must contain all specified Boxes.", boxList.map(_.box).asJavaCollection.containsAll(bl))

    //TEST for - NodeWallet.allboxes(boxIdsToExclude)
    bl = sidechainWallet.allBoxes(boxList.slice(0, 1).map(_.box.id()).asJava)

    assertEquals("Wallet must contain specified count of Boxes.", boxList.size - 1, bl.size)
    assertTrue("Wallet must contain all specified Boxes.", boxList.slice(1, 5).map(_.box).asJavaCollection.containsAll(bl))

    //TEST for - NodeWallet.boxesOfType
    bl = sidechainWallet.boxesOfType(classOf[RegularBox])

    assertEquals("Wallet must contain specified count of Boxes.", boxList.size, bl.size)
    assertTrue("Wallet must contain all specified Boxes.", boxList.map(_.box).asJavaCollection.containsAll(bl))

    //TEST for - NodeWallet.boxesOfType(boxIdsToExclude)
    bl = sidechainWallet.boxesOfType(classOf[RegularBox], boxList.slice(0, 1).map(_.box.id()).asJava)

    assertEquals("Wallet must contain specified count of Boxes.", boxList.size - 1, bl.size)
    assertTrue("Wallet must contain all specified Boxes.", boxList.slice(1, 5).map(_.box).asJavaCollection.containsAll(bl))

    //TEST for - NodeWallet.boxesBalance
    assertEquals("", boxList.map(_.box.value()).sum, sidechainWallet.boxesBalance(classOf[RegularBox]))
  }

}
