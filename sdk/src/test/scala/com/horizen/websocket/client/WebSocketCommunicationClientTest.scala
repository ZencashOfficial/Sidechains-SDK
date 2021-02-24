package com.horizen.websocket.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.junit.Assert._
import org.junit.{Before, Test}
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.junit.JUnitSuite
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.{Await, Future, TimeoutException}
import scala.util.{Failure, Success, Try}

class WebSocketCommunicationClientTest extends JUnitSuite with MockitoSugar {

  // No matter about real information (height, block hashes etc)
  private var mapper : ObjectMapper = null

  private def getServerResponse(message : String) : String = {
    var resp = ""
    try {
      val json = mapper.readTree(message)
      val reqId = Try(json.get("requestId").asText())
      if(reqId.isSuccess){
        val reqtype = json.get("requestType").asText() match {
          case "0" | "1" if reqId.get.length>0 =>
            resp =
              mapper.createObjectNode()
                .put("msgType", 2)
                .put("requestId", reqId.get)
                .set("responsePayload", mapper.createObjectNode()
                  .put("height", 50)
                  .put("hash", "0372229473df1b966945e2b307b86bd856323a54c645ccb91cabd1a49d8f87bf")
                  .put("block", "the block hex"))
                .toString
          case "2" if reqId.get.length>0 =>
            resp =
              mapper.createObjectNode()
                .put("msgType", 2)
                .put("requestId", reqId.get)
                .set("responsePayload", mapper.createObjectNode()
                  .put("height", 201)
                  .set("hashes", mapper.createArrayNode()
                    .add("08d34596dd7e137f603d6661d867eb083c0592e8333f838478de2ebd3efd8e5a")
                    .add("0e4d418de14c9aeaba0714bc23626bab1fe12001758dd6d4908029ad588b5861")
                    .add("0e812bd816810df31b1cbfdacec59768e3dc668dbd9186c421bc44730e61ecfa")
                    .add("0e760f0f0e47b5155905e155c26ee5af680bca459d1273cf2ba4eaaad4c1ca7d")
                    .add("001c2bca61711e2f74a001283eb3cb60645d0d42586ccb14879afcd68c2ed2f9")
                    .add("028db24dc6985679cdff9ce4648d110236123729760affd48836d26ca5cca7f4")))
                .toString
          case _ =>
            resp = mapper.createObjectNode()
              .put("msgType", 3)
              .put("requestId", reqId.get)
              .put("errorCode", 2)
              .put("message", "INVALID_COMMAND").toString
        }
      }else{
        resp = mapper.createObjectNode()
          .put("msgType", 3)
          .put("requestId", "")
          .put("errorCode", 5)
          .put("message", "MISSING_REQUESTID").toString
      }


    } catch {
      case ex: Throwable =>
        resp = mapper.createObjectNode()
          .put("msgType", 3)
          .put("requestId", "")
          .put("errorCode", 3)
          .put("message", "INVALID_JSON_FORMAT").toString
    }

    resp
  }

  @Before
  def setUp() : Unit = {
    mapper = new ObjectMapper().registerModule(DefaultScalaModule)
  }

  @Test
  def sendRequestWithoutChannel() : Unit = {

    val webSocketClient = new WebSocketCommunicationClient()

    val mockedChannel = mock[WebSocketChannel]
    val mockedConnector : WebSocketConnector = mock[WebSocketConnector]

    Mockito.when(mockedConnector.isStarted()).thenAnswer(asw => true)
    Mockito.when(mockedConnector.stop()).thenAnswer(asw => Success())
    Mockito.when(mockedConnector.start()).thenAnswer(asw => Try(mockedChannel))

    val fut = webSocketClient.sendRequest(0, null, null)
    assertTrue(fut.isCompleted)
    assertEquals(classOf[Failure[_]], fut.value.get.getClass)

  }

  @Test
  def sendWellFormedRequest() : Unit = {

    val webSocketClient = new WebSocketCommunicationClient()

    val mockedChannel = mock[WebSocketChannel]
    val mockedConnector : WebSocketConnector = mock[WebSocketConnector]

    Mockito.when(mockedConnector.isStarted()).thenAnswer(asw => true)
    Mockito.when(mockedConnector.stop()).thenAnswer(asw => Success())
    Mockito.when(mockedConnector.start()).thenAnswer(asw => Try(mockedChannel))

    webSocketClient.setWebSocketChannel(mockedChannel)

    val wellFormedGetBlockByHeightRequestPayload : RequestPayload = GetBlockByHeightRequestPayload(30) // request type -> 0
    val wellFormedGetBlockByHashRequestPayload : RequestPayload = GetBlockByHashRequestPayload("0372229473df1b966945e2b307b86bd856323a54c645ccb91cabd1a49d8f87bf") // request type -> 0
    val wellFormedGetNewBlocksRequestPayload : RequestPayload = GetNewBlocksRequestPayload(Seq[String]("hash_1", "hash_2", "hash_3"), 30) // request type -> 2

    Mockito.when(mockedChannel.sendMessage(ArgumentMatchers.any())).thenAnswer(
      asw =>
      {
        val req = asw.getArgument(0).asInstanceOf[String]
        // get response from server and send reply to the registered message handler
        webSocketClient.onReceivedMessage(getServerResponse(req))
      }
    )

    try {
      val future_1 : Future[BlockResponsePayload]= webSocketClient.sendRequest[RequestPayload, BlockResponsePayload](0, wellFormedGetBlockByHeightRequestPayload, classOf[BlockResponsePayload])
      val welFormedBlockResponsePayload_1 : BlockResponsePayload = Await.result(future_1, webSocketClient.requestTimeoutDuration())
      assertTrue(future_1.value.get.isSuccess)
      assertEquals(welFormedBlockResponsePayload_1.hash, "0372229473df1b966945e2b307b86bd856323a54c645ccb91cabd1a49d8f87bf")
      assertEquals(welFormedBlockResponsePayload_1.block, "the block hex")
      assertEquals(welFormedBlockResponsePayload_1.height, 50)

      val future_2 : Future[BlockResponsePayload] = webSocketClient.sendRequest[RequestPayload, BlockResponsePayload](0, wellFormedGetBlockByHashRequestPayload, classOf[BlockResponsePayload])
      val welFormedBlockResponsePayload_2 : BlockResponsePayload = Await.result(future_2, webSocketClient.requestTimeoutDuration())
      assertTrue(future_2.value.get.isSuccess)
      assertEquals(welFormedBlockResponsePayload_2.hash, "0372229473df1b966945e2b307b86bd856323a54c645ccb91cabd1a49d8f87bf")
      assertEquals(welFormedBlockResponsePayload_2.block, "the block hex")
      assertEquals(welFormedBlockResponsePayload_2.height, 50)

      val future_3 : Future[NewBlocksResponsePayload] = webSocketClient.sendRequest[RequestPayload, NewBlocksResponsePayload](2, wellFormedGetNewBlocksRequestPayload, classOf[NewBlocksResponsePayload])
      val welFormedNewBlocksResponsePayload : NewBlocksResponsePayload = Await.result(future_3, webSocketClient.requestTimeoutDuration())
      assertTrue(future_3.value.get.isSuccess)
      assertEquals(welFormedNewBlocksResponsePayload.hashes, Seq[String](
         "08d34596dd7e137f603d6661d867eb083c0592e8333f838478de2ebd3efd8e5a",
                "0e4d418de14c9aeaba0714bc23626bab1fe12001758dd6d4908029ad588b5861",
                "0e812bd816810df31b1cbfdacec59768e3dc668dbd9186c421bc44730e61ecfa",
                "0e760f0f0e47b5155905e155c26ee5af680bca459d1273cf2ba4eaaad4c1ca7d",
                "001c2bca61711e2f74a001283eb3cb60645d0d42586ccb14879afcd68c2ed2f9",
                "028db24dc6985679cdff9ce4648d110236123729760affd48836d26ca5cca7f4"
      ))
      assertEquals(welFormedNewBlocksResponsePayload.height, 201)

    } catch {
      case e : Throwable => fail(e)
    }
  }

  @Test
  def sendRequestWithMalformedType() : Unit = {

    val webSocketClient = new WebSocketCommunicationClient()

    val mockedChannel = mock[WebSocketChannel]
    val mockedConnector : WebSocketConnector = mock[WebSocketConnector]

    Mockito.when(mockedConnector.isStarted()).thenAnswer(asw => true)
    Mockito.when(mockedConnector.stop()).thenAnswer(asw => Success())
    Mockito.when(mockedConnector.start()).thenAnswer(asw => Try(mockedChannel))

    webSocketClient.setWebSocketChannel(mockedChannel)

    val wellFormedGetBlockByHeightRequestPayload : RequestPayload = GetBlockByHeightRequestPayload(30) // request type -> 0

    Mockito.when(mockedChannel.sendMessage(ArgumentMatchers.any())).thenAnswer(
      asw =>
      {
        val req = asw.getArgument(0).asInstanceOf[String]
        // get response from server and send reply to the registered message handler
        webSocketClient.onReceivedMessage(getServerResponse(req))
      }
    )

    try {
      val future_1 : Future[BlockResponsePayload]= webSocketClient.sendRequest[RequestPayload, BlockResponsePayload](10, wellFormedGetBlockByHeightRequestPayload, classOf[BlockResponsePayload])
      Await.result(future_1, webSocketClient.requestTimeoutDuration())
      fail("The response must be an error.")
    } catch {
      case e : Throwable =>
        assertEquals(classOf[RuntimeException], e.getClass)
    }
  }

  @Test
  def sendWellFormedRequestReceiveMalformedResponse() : Unit = {

    val webSocketClient = new WebSocketCommunicationClient()

    val mockedChannel = mock[WebSocketChannel]
    val mockedConnector : WebSocketConnector = mock[WebSocketConnector]

    Mockito.when(mockedConnector.isStarted()).thenAnswer(asw => true)
    Mockito.when(mockedConnector.stop()).thenAnswer(asw => Success())
    Mockito.when(mockedConnector.start()).thenAnswer(asw => Try(mockedChannel))

    webSocketClient.setWebSocketChannel(mockedChannel)

    val wellFormedGetBlockByHeightRequestPayload : RequestPayload = GetBlockByHeightRequestPayload(30) // request type -> 0

    Mockito.when(mockedChannel.sendMessage(ArgumentMatchers.any())).thenAnswer(
      asw =>
      {
        val req = asw.getArgument(0).asInstanceOf[String]
        // get response from server and send reply to the registered message handler
        webSocketClient.onReceivedMessage("just a simple response...")
      }
    )

    try {
      val future_1 : Future[BlockResponsePayload]= webSocketClient.sendRequest[RequestPayload, BlockResponsePayload](10, wellFormedGetBlockByHeightRequestPayload, classOf[BlockResponsePayload])
      Await.result(future_1, webSocketClient.requestTimeoutDuration())
      fail("The response must be an error.")
    } catch {
      case e : Throwable =>
        assertEquals(classOf[TimeoutException], e.getClass)
    }
  }

  @Test
  def receiveEvent() : Unit = {

    val webSocketClient = new WebSocketCommunicationClient()

    val mockedChannel = mock[WebSocketChannel]
    val mockedConnector : WebSocketConnector = mock[WebSocketConnector]
    val mockedEventHandler : EventHandler[OnUpdateTipEventPayload] = mock[OnUpdateTipEventHandler]

    Mockito.when(mockedConnector.isStarted()).thenAnswer(asw => true)
    Mockito.when(mockedConnector.stop()).thenAnswer(asw => Success())
    Mockito.when(mockedConnector.start()).thenAnswer(asw => Try(mockedChannel))

    webSocketClient.setWebSocketChannel(mockedChannel)

    val registration = webSocketClient.registerEventHandler[OnUpdateTipEventPayload](0, mockedEventHandler, classOf[OnUpdateTipEventPayload])
    assertTrue(registration.isSuccess)

    val eventJson = mapper.createObjectNode()
      .put("height", 265)
      .put("hash", "012eca4e7c57850527716dbbfa63798260c16c8d022d57e86499f4d845ed46a9")
      .put("block", "the block hex")

    val eventFromServer = mapper.createObjectNode()
      .put("msgType", 0)
      .put("eventType", 0)
      .set("eventPayload", eventJson).toString

    webSocketClient.onReceivedMessage(eventFromServer)
    webSocketClient.onReceivedMessage(eventFromServer)
    webSocketClient.onReceivedMessage(eventFromServer)

    val event = OnUpdateTipEventPayload(265, "012eca4e7c57850527716dbbfa63798260c16c8d022d57e86499f4d845ed46a9", "the block hex")
    Mockito.verify(mockedEventHandler, Mockito.times(3)).onEvent(ArgumentMatchers.eq(event))

    webSocketClient.unregisterEventHandler[OnUpdateTipEventPayload](0, mockedEventHandler)

    // the event handler 'onEvent' must not be called anymore
    webSocketClient.onReceivedMessage(eventFromServer)
    webSocketClient.onReceivedMessage(eventFromServer)
    Mockito.verify(mockedEventHandler, Mockito.times(3)).onEvent(ArgumentMatchers.eq(event))

  }

}
