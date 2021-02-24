package com.horizen.websocket.client

import com.horizen.mainchain.api.{BackwardTransferEntry, SendCertificateRequest}
import com.horizen.params.MainNetParams
import com.horizen.utils.BytesUtils
import org.junit.Assert._
import org.junit.Test
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.junit.JUnitSuite
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Promise
import scala.concurrent.duration.{FiniteDuration, _}
import scala.io.Source
import scala.util.Success

class MainchainNodeChannelImplTest extends JUnitSuite with MockitoSugar {

  @Test
  def getBlockByHeight(): Unit = {
    val mockedCommunicationClient: CommunicationClient = mock[CommunicationClient]

    val timeoutDuration: FiniteDuration = new FiniteDuration(100, MILLISECONDS)
    val height = 473173
    val hash = "0000000024ebb5c6d558daa34ad9b9a4c5503b057e14815a48e241612b1eb660"
    val mcBlockHex = Source.fromResource("mcblock473173_mainnet").getLines().next()
    val expectedReqType = 0

    Mockito.when(mockedCommunicationClient.requestTimeoutDuration()).thenReturn(timeoutDuration)
    Mockito.when(mockedCommunicationClient.sendRequest[RequestPayload, ResponsePayload](
      ArgumentMatchers.any[Int], ArgumentMatchers.any[RequestPayload], ArgumentMatchers.any[Class[ResponsePayload]]
    )).thenAnswer( answer => {
        val reqType = answer.getArgument(0).asInstanceOf[Int]
        assertEquals("Get block by height request type is wrong.", expectedReqType, reqType)
        val req = answer.getArgument(1).asInstanceOf[GetBlockByHeightRequestPayload]
        assertEquals("Get block by height request data (height) is wrong.", height, req.height)
        assertTrue("Get block by height response payload type is wrong", answer.getArgument(2).isInstanceOf[Class[BlockResponsePayload]])

        val p = Promise[ResponsePayload]
        val thread = new Thread {
          override def run() {
            Thread.sleep(timeoutDuration.div(2L).toMillis)
            p.complete(Success(BlockResponsePayload(height, hash, mcBlockHex)))
          }
        }
        thread.start()
        p.future
      }
    )

    val params = MainNetParams()
    val mcnode = new MainchainNodeChannelImpl(mockedCommunicationClient, params)

    val mcRefTry = mcnode.getBlockByHeight(height)
    assertTrue("MCBlock ref expected to be retrieved.", mcRefTry.isSuccess)
    assertEquals("MCBlock ref hash is different.", hash, mcRefTry.get.header.hashHex)
  }


  @Test
  def getBlockByHash(): Unit = {
    val mockedCommunicationClient: CommunicationClient = mock[CommunicationClient]

    val timeoutDuration: FiniteDuration = new FiniteDuration(100, MILLISECONDS)
    val height = 473173
    val hash = "0000000024ebb5c6d558daa34ad9b9a4c5503b057e14815a48e241612b1eb660"
    val mcBlockHex = Source.fromResource("mcblock473173_mainnet").getLines().next()
    val expectedReqType = 0

    Mockito.when(mockedCommunicationClient.requestTimeoutDuration()).thenReturn(timeoutDuration)
    Mockito.when(mockedCommunicationClient.sendRequest[RequestPayload, ResponsePayload](
      ArgumentMatchers.any[Int], ArgumentMatchers.any[RequestPayload], ArgumentMatchers.any[Class[ResponsePayload]]
    )).thenAnswer( answer => {
        val reqType = answer.getArgument(0).asInstanceOf[Int]
        assertEquals("Get block by hash request type is wrong.", expectedReqType, reqType)
        val req = answer.getArgument(1).asInstanceOf[GetBlockByHashRequestPayload]
        assertEquals("Get block by hash request data (hash) is wrong.", hash, req.hash)
        assertTrue("Get block by hash response payload type is wrong", answer.getArgument(2).isInstanceOf[Class[BlockResponsePayload]])

        val p = Promise[ResponsePayload]
        val thread = new Thread {
          override def run() {
            Thread.sleep(timeoutDuration.div(2L).toMillis)
            p.complete(Success(BlockResponsePayload(height, hash, mcBlockHex)))
          }
        }
        thread.start()
        p.future
      }
    )

    val params = MainNetParams()
    val mcnode = new MainchainNodeChannelImpl(mockedCommunicationClient, params)

    val mcRefTry = mcnode.getBlockByHash(hash)
    assertTrue("MCBlock ref expected to be retrieved.", mcRefTry.isSuccess)
    assertEquals("MCBlock ref hash is different.",
      "0000000024ebb5c6d558daa34ad9b9a4c5503b057e14815a48e241612b1eb660", mcRefTry.get.header.hashHex)
  }

  @Test
  def getNewBlockHashes(): Unit = {
    val mockedCommunicationClient: CommunicationClient = mock[CommunicationClient]

    val timeoutDuration: FiniteDuration = new FiniteDuration(100, MILLISECONDS)

    val reqHashes = Seq(
      "0000000024ebb5c6d558daa34ad9b9a4c5503b057e14815a48e241612b1eb660",
      "0000000024ebb5c6d558daa34ad9b9a4c5503b057e14815a48e241612b1eb661",
      "0000000024ebb5c6d558daa34ad9b9a4c5503b057e14815a48e241612b1eb662"
    )
    val limit = 10
    val expectedReqType = 2

    val respHeight = 1000
    val respHashes = Seq(
      "0000000024ebb5c6d558daa34ad9b9a4c5503b057e14815a48e241612b1eb662",
      "0000000024ebb5c6d558daa34ad9b9a4c5503b057e14815a48e241612b1eb663",
      "0000000024ebb5c6d558daa34ad9b9a4c5503b057e14815a48e241612b1eb664"
    )

    Mockito.when(mockedCommunicationClient.requestTimeoutDuration()).thenReturn(timeoutDuration)
    Mockito.when(mockedCommunicationClient.sendRequest[RequestPayload, ResponsePayload](
      ArgumentMatchers.any[Int], ArgumentMatchers.any[RequestPayload], ArgumentMatchers.any[Class[ResponsePayload]]
    )).thenAnswer( answer => {
      val reqType = answer.getArgument(0).asInstanceOf[Int]
      assertEquals("Get new block hashes request type is wrong.", expectedReqType, reqType)
      val req = answer.getArgument(1).asInstanceOf[GetNewBlocksRequestPayload]
      assertEquals("Get new block hashes request data (locatorHashes) is wrong.", reqHashes, req.locatorHashes)
      assertEquals("Get new block hashes request data (limit) is wrong.", limit, req.limit)
      assertTrue("Get bnew block hashes response payload type is wrong", answer.getArgument(2).isInstanceOf[Class[NewBlocksResponsePayload]])

      val p = Promise[ResponsePayload]
      val thread = new Thread {
        override def run() {
          Thread.sleep(timeoutDuration.div(2L).toMillis)
          p.complete(Success(NewBlocksResponsePayload(respHeight, respHashes)))
        }
      }
      thread.start()
      p.future
    }
    )

    val params = MainNetParams()
    val mcnode = new MainchainNodeChannelImpl(mockedCommunicationClient, params)

    val hashesTry = mcnode.getNewBlockHashes(reqHashes, limit)
    assertTrue("Result expected to be successful.", hashesTry.isSuccess)
    assertEquals("Result hashes is different.", (respHeight, respHashes), hashesTry.get)
  }

  @Test
  def onUpdateTipEvent(): Unit = {
    val mockedCommunicationClient: CommunicationClient = mock[CommunicationClient]

    val height = 473173
    val hash = "0000000024ebb5c6d558daa34ad9b9a4c5503b057e14815a48e241612b1eb660"
    val mcBlockHex = Source.fromResource("mcblock473173_mainnet").getLines().next()
    val expectedEventType = 0

    class OnUpdateEventHandlerImpl extends OnUpdateTipEventHandler {
      override def onEvent(eventPayload: OnUpdateTipEventPayload): Unit = {

      }
    }
    val handler = new OnUpdateEventHandlerImpl()


    // Test 1: subscribing
    Mockito.when(mockedCommunicationClient.registerEventHandler[EventPayload](
      ArgumentMatchers.any[Int], ArgumentMatchers.any[EventHandler[EventPayload]], ArgumentMatchers.any[Class[EventPayload]]
    )).thenAnswer( answer => {
        val eventType = answer.getArgument(0).asInstanceOf[Int]
        assertEquals("Subscribe on tip event type is wrong.", expectedEventType, eventType)
        assertEquals("Subscribe on tip handler is different", handler, answer.getArgument(1))
        assertEquals("Subscribe on tip event payload type is different", classOf[OnUpdateTipEventPayload], answer.getArgument(2))
        Success(Unit)
      }
    )

    val params = MainNetParams()
    val mcnode = new MainchainNodeChannelImpl(mockedCommunicationClient, params)

    val res = mcnode.subscribeOnUpdateTipEvent(handler)
    assertTrue("SubscribeOnUpdateTipEvent result expected to be successful", res.isSuccess)


    // Test 2: unsubscribing
    Mockito.when(mockedCommunicationClient.unregisterEventHandler[EventPayload](
      ArgumentMatchers.any[Int], ArgumentMatchers.any[EventHandler[EventPayload]]
    )).thenAnswer( answer => {
        val eventType = answer.getArgument(0).asInstanceOf[Int]
        assertEquals("Subscribe on tip event type is wrong.", expectedEventType, eventType)
        assertEquals("Subscribe on tip handler is different", handler, answer.getArgument(1))
      }
    )
    mcnode.unsubscribeOnUpdateTipEvent(handler)
  }

  @Test
  def sendCertificate(): Unit = {
    val mockedCommunicationClient: CommunicationClient = mock[CommunicationClient]
    val timeoutDuration: FiniteDuration = new FiniteDuration(100, MILLISECONDS)

    val scid = "3f78cb790f5e6f30440af7968a8a63ce3dc95913082cfb2476c572999997025b"
    val epochNumber = 0
    val quality = 7
    val endEpochBlockHash = "00491da3a761bc100ea8e15cc8f8e072df8eeb24073c87aa5579be4a4ead1f45"
    val scProof = "f596814b17a5d9bae82b7c377d8cbf44b4d07e6430b26346dde4fef807b1138d877817226dbf9648a87ba278d6de63a0f5a4702e6e16b8e4b6794889de709f80f04b331af7c7b49a41518c73cf4a7d869f933b9241e1dfc584997963f7d40000405fb19b84afd7704bf9823d42cbb6ef8c14fd703f84866e0f6a9177d5060052bb51d1fc4696f505642251ac6041f2762d9386635e7f222b7106f73341f2f7e907e99034955648df11a1f0aeffece9e1ae3add7436d43c6444cefa4b286a0100007547473dec029e91dc774e40b91377ae63591dd3347f15ced6df93956a1b3a587df464f9c0900ba6870e87cdc262318b8f1d5f8823cdd4347697bc1237c2c2a085515623d0814486f69e5b04c9426834855dd8515f16f1fbe66252096ae600002f2a6b043302f126ca7fe6fb6c50b1ea882d5f7023a7de427b68108a7e9f53e0b630f771f5d7d3dec0e978e2cad364350bd6ac15bd7f27d7a2de92e9d87a5fc71e7e14427b2f49e1e01e21ef2f79533e70ac0cba5ae2a0bb1a2c12ca9223010071e2aca4f2ed6b5d393c6ac783e3c60a2a885906d299cf32ddb733e5a21886e2db0157af9ba53fe2158c4d165a64f80741eaa49fd9c6441730b5543b738f49b980f6c4fd80000ac4dc82950ca08d8b2c67c31b1073531569c0f5ae1603920000290260cf79ce7b036e0bffcea983f6b9f7aac23743fd0d3af2ff8c16c3778c7aaedddeb0b3ab6c976364c7e50227133c47a2dc53e8fc92efe380a416e24961a283e84513882d61707d7f498c94242c921521cfa99e697ed64edc49d2b5ad00000043cb9976c1a1b16bdf5e360f536f553c9f7da716881d80dcec703bd919c14498059595358644de34ccc976dc56467b3d7e61c2f9b4d352158de64f6f708c869943d9363bd196e8ec486529e197a319815d0f6a46f1a902558074c3b16d8e0100cd3daed0c59fafb2008797eb0261cabea67fc0d6b2cb087d2ca902a30e0e76bf87987d24e650cc0178c5ecb8ef986b490c6ba8985dc1e18fbf2892fa2a0d89c716fb3452f4fde026623ea38e558099092d55439c1aca427e0386e712ac9f010000"

    val certHash = "a853d5f5251a8ef5dc248d3fff45301249934bdda48d1d3c0c97b58918e05aa0"
    val expectedReqType = 3

    Mockito.when(mockedCommunicationClient.requestTimeoutDuration()).thenReturn(timeoutDuration)
    Mockito.when(mockedCommunicationClient.sendRequest[RequestPayload, ResponsePayload](
      ArgumentMatchers.any[Int], ArgumentMatchers.any[RequestPayload], ArgumentMatchers.any[Class[ResponsePayload]]
    )).thenAnswer(answer => {
      val reqType = answer.getArgument(0).asInstanceOf[Int]
      assertEquals("Send certificate request type is wrong.", expectedReqType, reqType)
      val req = answer.getArgument(1).asInstanceOf[SendCertificateRequestPayload]
      assertEquals("Send certificate request data (scid) is wrong.", scid, req.scid)
      assertEquals("Send certificate request data (epochNumber) is wrong.", epochNumber, req.epochNumber)
      assertEquals("Send certificate request data (quality) is wrong.", quality, req.quality)
      assertEquals("Send certificate request data (endEpochBlockHash) is wrong.", endEpochBlockHash, req.endEpochBlockHash)
      assertEquals("Send certificate request data (scProof) is wrong.", scProof, req.scProof)

      assertTrue("Send certificate response payload type is wrong", answer.getArgument(2).isInstanceOf[Class[ResponsePayload]])

      val p = Promise[ResponsePayload]
      val thread = new Thread {
        override def run() {
          Thread.sleep(timeoutDuration.div(2L).toMillis)
          p.complete(Success(CertificateResponsePayload(certHash)))
        }
      }
      thread.start()
      p.future
    }
    )

    val params = MainNetParams()
    val mcnode = new MainchainNodeChannelImpl(mockedCommunicationClient, params)
    val certificate = SendCertificateRequest(BytesUtils.fromHexString(scid), epochNumber,
      BytesUtils.fromHexString(endEpochBlockHash), BytesUtils.fromHexString(scProof),
      quality, Seq())

    val mcRefTry = mcnode.sendCertificate(certificate)

    assertTrue("Certificate is expected to be sent.", mcRefTry.isSuccess)
    assertEquals("Certificate hash is different.", certHash, BytesUtils.toHexString(mcRefTry.get.certificateId))
  }

  @Test
  def sendCertificateWithBackwardTransfer(): Unit = {
    val mockedCommunicationClient: CommunicationClient = mock[CommunicationClient]
    val timeoutDuration: FiniteDuration = new FiniteDuration(100, MILLISECONDS)

    val scid = "3f78cb790f5e6f30440af7968a8a63ce3dc95913082cfb2476c572999997025b"
    val epochNumber = 0
    val quality = 7
    val endEpochBlockHash = "00491da3a761bc100ea8e15cc8f8e072df8eeb24073c87aa5579be4a4ead1f45"
    val scProof = "f596814b17a5d9bae82b7c377d8cbf44b4d07e6430b26346dde4fef807b1138d877817226dbf9648a87ba278d6de63a0f5a4702e6e16b8e4b6794889de709f80f04b331af7c7b49a41518c73cf4a7d869f933b9241e1dfc584997963f7d40000405fb19b84afd7704bf9823d42cbb6ef8c14fd703f84866e0f6a9177d5060052bb51d1fc4696f505642251ac6041f2762d9386635e7f222b7106f73341f2f7e907e99034955648df11a1f0aeffece9e1ae3add7436d43c6444cefa4b286a0100007547473dec029e91dc774e40b91377ae63591dd3347f15ced6df93956a1b3a587df464f9c0900ba6870e87cdc262318b8f1d5f8823cdd4347697bc1237c2c2a085515623d0814486f69e5b04c9426834855dd8515f16f1fbe66252096ae600002f2a6b043302f126ca7fe6fb6c50b1ea882d5f7023a7de427b68108a7e9f53e0b630f771f5d7d3dec0e978e2cad364350bd6ac15bd7f27d7a2de92e9d87a5fc71e7e14427b2f49e1e01e21ef2f79533e70ac0cba5ae2a0bb1a2c12ca9223010071e2aca4f2ed6b5d393c6ac783e3c60a2a885906d299cf32ddb733e5a21886e2db0157af9ba53fe2158c4d165a64f80741eaa49fd9c6441730b5543b738f49b980f6c4fd80000ac4dc82950ca08d8b2c67c31b1073531569c0f5ae1603920000290260cf79ce7b036e0bffcea983f6b9f7aac23743fd0d3af2ff8c16c3778c7aaedddeb0b3ab6c976364c7e50227133c47a2dc53e8fc92efe380a416e24961a283e84513882d61707d7f498c94242c921521cfa99e697ed64edc49d2b5ad00000043cb9976c1a1b16bdf5e360f536f553c9f7da716881d80dcec703bd919c14498059595358644de34ccc976dc56467b3d7e61c2f9b4d352158de64f6f708c869943d9363bd196e8ec486529e197a319815d0f6a46f1a902558074c3b16d8e0100cd3daed0c59fafb2008797eb0261cabea67fc0d6b2cb087d2ca902a30e0e76bf87987d24e650cc0178c5ecb8ef986b490c6ba8985dc1e18fbf2892fa2a0d89c716fb3452f4fde026623ea38e558099092d55439c1aca427e0386e712ac9f010000"

    val backwardTransfer:Seq[BackwardTransfer] = Seq(BackwardTransfer("350a277a722333536ad3c044176b80aae8993c25","7"),
                                                     BackwardTransfer("19227fd1a33c2e0817925de63070136244925058","3"))

    val backwardTransferInput:Seq[BackwardTransferEntry] = backwardTransfer.map(bt => BackwardTransferEntry(BytesUtils.fromHexString(bt.pubkeyhash), bt.amount))

    val certHash = "a853d5f5251a8ef5dc248d3fff45301249934bdda48d1d3c0c97b58918e05aa0"
    val expectedReqType = 3

    Mockito.when(mockedCommunicationClient.requestTimeoutDuration()).thenReturn(timeoutDuration)
    Mockito.when(mockedCommunicationClient.sendRequest[RequestPayload, ResponsePayload](
      ArgumentMatchers.any[Int], ArgumentMatchers.any[RequestPayload], ArgumentMatchers.any[Class[ResponsePayload]]
    )).thenAnswer(answer => {
      val reqType = answer.getArgument(0).asInstanceOf[Int]
      assertEquals("Send certificate request type is wrong.", expectedReqType, reqType)
      val req = answer.getArgument(1).asInstanceOf[SendCertificateRequestPayload]
      assertEquals("Send certificate request data (scid) is wrong.", scid, req.scid)
      assertEquals("Send certificate request data (epochNumber) is wrong.", epochNumber, req.epochNumber)
      assertEquals("Send certificate request data (quality) is wrong.", quality, req.quality)
      assertEquals("Send certificate request data (endEpochBlockHash) is wrong.", endEpochBlockHash, req.endEpochBlockHash)
      assertEquals("Send certificate request data (scProof) is wrong.", scProof, req.scProof)
      assertEquals("Send certificate request data (backward transfers) is wrong.", backwardTransfer, req.backwardTransfers)

      assertTrue("Send certificate response payload type is wrong", answer.getArgument(2).isInstanceOf[Class[ResponsePayload]])

      val p = Promise[ResponsePayload]
      val thread = new Thread {
        override def run() {
          Thread.sleep(timeoutDuration.div(2L).toMillis)
          p.complete(Success(CertificateResponsePayload(certHash)))
        }
      }
      thread.start()
      p.future
    }
    )

    val params = MainNetParams()
    val mcnode = new MainchainNodeChannelImpl(mockedCommunicationClient, params)
    val certificate = SendCertificateRequest(BytesUtils.fromHexString(scid), epochNumber,
      BytesUtils.fromHexString(endEpochBlockHash), BytesUtils.fromHexString(scProof),
      quality, backwardTransferInput)

    val mcRefTry = mcnode.sendCertificate(certificate)

    assertTrue("Certificate is expected to be sent.", mcRefTry.isSuccess)
    assertEquals("Certificate hash is different.", certHash, BytesUtils.toHexString(mcRefTry.get.certificateId))
  }
}
