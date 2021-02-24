package com.horizen.websocket.server

import java.net.URI
import java.util

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit
import akka.testkit.{TestActor, TestProbe}
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.horizen.{SidechainMemoryPool}
import com.horizen.api.http.{NodeViewHolderUtilMocks, SidechainApiMockConfiguration}
import javax.websocket.{ClientEndpointConfig, Endpoint, EndpointConfig, MessageHandler, Session}
import org.glassfish.tyrus.client.ClientManager
import org.junit.Assert.{assertEquals, assertTrue}
import org.junit.{Before, Test}
import org.scalatest.junit.JUnitSuite
import org.scalatest.mockito.MockitoSugar
import scorex.core.NodeViewHolder.ReceivableMessages.GetDataFromCurrentView
import scorex.core.network.NodeViewSynchronizer.ReceivableMessages.{ChangedMempool, SemanticallySuccessfulModifier}

import scala.concurrent.{ExecutionContext, Promise}

class WebSocketServerEndpointTest extends JUnitSuite with MockitoSugar{
  private var server: ActorRef = _
  private var mapper : ObjectMapper = new ObjectMapper().registerModule(DefaultScalaModule)
  implicit val ec:ExecutionContext = ExecutionContext.Implicits.global

  implicit lazy val actorSystem: ActorSystem = ActorSystem("test-wwebsocket-server")
  val mockedSidechainNodeViewHolder = TestProbe()
  val sidechainApiMockConfiguration: SidechainApiMockConfiguration = new SidechainApiMockConfiguration()
  val utilMocks = new NodeViewHolderUtilMocks()

  mockedSidechainNodeViewHolder.setAutoPilot(new testkit.TestActor.AutoPilot {
    override def run(sender: ActorRef, msg: Any): TestActor.AutoPilot = {
      msg match {
        case GetDataFromCurrentView(f) =>
            sender ! f(utilMocks.getNodeView(sidechainApiMockConfiguration))
      }
      TestActor.KeepRunning
    }
  })
  val mockedSidechainNodeViewHolderRef: ActorRef = mockedSidechainNodeViewHolder.ref

  @Before
  def setUp(): Unit = {
    // start server on default port
    server = WebSocketServerRef(mockedSidechainNodeViewHolderRef, 9025)
    Thread.sleep(3000)
  }


  @Test
  def badRequestMessage(): Unit = {
    //Verify that processError of WebSocketServerEndpoint is called with wrong message structure
    val cec = ClientEndpointConfig.Builder.create.build
    val client = ClientManager.createClient

    val endpoint = new WsEndpoint
    var session: Session = client.connectToServer(endpoint, cec, new URI("ws://localhost:9025/"))

    val wrongRequest = mapper.createObjectNode()
        .put("wrong_key", 1)

    session.getBasicRemote.sendText(wrongRequest.toString)
    Thread.sleep(2000)

    var json = mapper.readTree(endpoint.receivedMessage.get(0))

    assertTrue(checkStaticResponseFields(json,3,-1,-1))

    assertTrue(json.has("errorCode"))
    assertEquals(5,json.get("errorCode").asInt())

    assertTrue(json.has("responsePayload"))
    assertEquals("WebSocket message error!",json.get("responsePayload").asText())


    //Verify that processError of WebSocketServerEndpoint is called with wrong message structure
    val badMsgTypeRequest = mapper.createObjectNode()
      .put("msgType", 2)
      .put("requestId",0)
      .put("requestType", 5)
      .put("requestPayload", "{}")
    session.getBasicRemote.sendText(badMsgTypeRequest.toString)
    Thread.sleep(2000)

    json = mapper.readTree(endpoint.receivedMessage.get(1))
    assertTrue(checkStaticResponseFields(json,3,0,5))

    assertTrue(json.has("errorCode"))
    assertEquals(5,json.get("errorCode").asInt())

    assertTrue(json.has("responsePayload"))
    assertEquals("WebSocket message error!",json.get("responsePayload").asText())

    session.close()
  }

  @Test
  def getRawMempoolTest(): Unit = {
    // Test the getRawMempool request
    val cec = ClientEndpointConfig.Builder.create.build
    val client = ClientManager.createClient

    val endpoint = new WsEndpoint
    var session: Session = client.connectToServer(endpoint, cec, new URI("ws://localhost:9025/"))

    // Get raw mempool
    val rawMempoolRequest = mapper.createObjectNode()
      .put("msgType", 1)
      .put("requestId",0)
      .put("requestType", 5)
      .put("requestPayload", "{}")
    session.getBasicRemote.sendText(rawMempoolRequest.toString)
    Thread.sleep(2000)
    val json = mapper.readTree(endpoint.receivedMessage.get(0))

    assertTrue(checkStaticResponseFields(json,2,0,5))

    assertTrue(json.has("responsePayload"))
    val responsePayload = json.get("responsePayload")
    assertTrue(responsePayload.has("size"))
    assertEquals(2, responsePayload.get("size").asInt())
    assertTrue(responsePayload.has("transactions"))
    assertTrue(responsePayload.get("transactions").isArray)
    var nTx = 0
    responsePayload.get("transactions").forEach(tx => {
      nTx += 1
      assertEquals( "9e8d287524000a128f3d936ffdc1df1f2a54fa85a2800bc7b681e934d251efac", tx.asText())
    })
    assertEquals(2, nTx)
    session.close()

  }

  @Test
  def getMempoolTxsTest(): Unit = {
    // Test the getMempoolTxs request
    val cec = ClientEndpointConfig.Builder.create.build
    val client = ClientManager.createClient

    val endpoint = new WsEndpoint
    val session: Session = client.connectToServer(endpoint, cec, new URI("ws://localhost:9025/"))

    val rawMempoolRequest = mapper.createObjectNode()
      .put("msgType", 1)
      .put("requestId",0)
      .put("requestType", 4)
    rawMempoolRequest.putObject("requestPayload").putArray("hash").add("9e8d287524000a128f3d936ffdc1df1f2a54fa85a2800bc7b681e934d251efac")

    session.getBasicRemote.sendText(rawMempoolRequest.toString)
    Thread.sleep(3000)

    val json = mapper.readTree(endpoint.receivedMessage.get(0))

    assertTrue(checkStaticResponseFields(json,2,0,4))

    assertTrue(json.has("responsePayload"))
    val responsePayload = json.get("responsePayload")
    assertTrue(responsePayload.has("transactions"))
    assertTrue(responsePayload.get("transactions").isArray)

    var counter: Int = 0
    responsePayload.get("transactions").forEach(tx => {
      counter += 1
      assertEquals("9e8d287524000a128f3d936ffdc1df1f2a54fa85a2800bc7b681e934d251efac", tx.get("id").asText())
    })
    assertEquals(1, counter)

    session.close()

  }

  @Test
  def getSingleBlockTest():Unit = {
    // Test the getSingleBlock request
    val cec = ClientEndpointConfig.Builder.create.build
    val client = ClientManager.createClient

    val endpoint = new WsEndpoint
    val session: Session = client.connectToServer(endpoint, cec, new URI("ws://localhost:9025/"))

    // Get block by hash
    val blockByHashtRequest = mapper.createObjectNode()
      .put("msgType", 1)
      .put("requestId",0)
      .put("requestType", 0)
    blockByHashtRequest.putObject("requestPayload").put("hash","21438dfafec6d70317574cc3307bedf801e3f9137835ae8b36d12653c4d26e95")

    session.getBasicRemote.sendText(blockByHashtRequest.toString)
    Thread.sleep(3000)

    var json = mapper.readTree(endpoint.receivedMessage.get(0))

    assertTrue(checkStaticResponseFields(json,2,0,0))

    assertTrue(json.has("responsePayload"))
    var responsePayload = json.get("responsePayload")
    assertTrue(responsePayload.has("block"))
    assertTrue(responsePayload.has("hash"))
    assertTrue(responsePayload.has("height"))

    assertEquals("21438dfafec6d70317574cc3307bedf801e3f9137835ae8b36d12653c4d26e95", responsePayload.get("hash").asText())

    // Get block by height
    val blockByHeightRequest = mapper.createObjectNode()
      .put("msgType", 1)
      .put("requestId",0)
      .put("requestType", 0)
    blockByHeightRequest.putObject("requestPayload").put("height",100)

    session.getBasicRemote.sendText(blockByHeightRequest.toString)
    Thread.sleep(3000)

    json = mapper.readTree(endpoint.receivedMessage.get(1))

    assertTrue(checkStaticResponseFields(json,2,0,0))

    assertTrue(json.has("responsePayload"))
    responsePayload = json.get("responsePayload")
    assertTrue(responsePayload.has("block"))
    assertTrue(responsePayload.has("hash"))
    assertTrue(responsePayload.has("height"))

    session.close()
  }

  @Test
  def getNewBlockHashes():Unit = {
    // Test the getNewBlockHashes request
    val cec = ClientEndpointConfig.Builder.create.build
    val client = ClientManager.createClient

    val endpoint = new WsEndpoint
    val session: Session = client.connectToServer(endpoint, cec, new URI("ws://localhost:9025/"))

    // Get block by hash
    val newBlockHashesRequest = mapper.createObjectNode()
      .put("msgType", 1)
      .put("requestId",0)
      .put("requestType", 2)
    newBlockHashesRequest.putObject("requestPayload").putArray("locatorHashes").add("some_bock_hash")
    newBlockHashesRequest.findParent("locatorHashes").put("limit",5)

    session.getBasicRemote.sendText(newBlockHashesRequest.toString)
    Thread.sleep(3000)

    var json = mapper.readTree(endpoint.receivedMessage.get(0))

    assertTrue(checkStaticResponseFields(json,2,0,2))

    assertTrue(json.has("responsePayload"))
    val responsePayload = json.get("responsePayload")
    assertTrue(responsePayload.has("height"))
    assertTrue(responsePayload.has("hashes"))
    assertTrue(responsePayload.get("hashes").isArray)

    // Test with limit greater than max
    newBlockHashesRequest.findParent("locatorHashes").put("limit",100)
    session.getBasicRemote.sendText(newBlockHashesRequest.toString)
    Thread.sleep(3000)
    json = mapper.readTree(endpoint.receivedMessage.get(1))

    assertTrue(checkStaticResponseFields(json,3,0,2))
    assertTrue(json.has("errorCode"))
    assertEquals(4,json.get("errorCode").asInt())
    assertTrue(json.has("responsePayload"))
    assertEquals(json.get("responsePayload").asText(), "Invalid limit size! Max limit is 50")

    session.close()

  }

  @Test
  def eventsTest():Unit = {
    // Test the websocket server events

    //Connect to the websocket server with some request
    val cec = ClientEndpointConfig.Builder.create.build
    val client = ClientManager.createClient

    val endpoint = new WsEndpoint
    val session: Session = client.connectToServer(endpoint, cec, new URI("ws://localhost:9025/"))

    // Get block by hash
    val blockByHashtRequest = mapper.createObjectNode()
      .put("msgType", 1)
      .put("requestId",0)
      .put("requestType", 0)
    blockByHashtRequest.putObject("requestPayload").put("hash","some_block_hash")

    session.getBasicRemote.sendText(blockByHashtRequest.toString)
    Thread.sleep(3000)

    val testActor = wsActorRef()
    Thread.sleep(5000)

    //Check SemanticallySuccessfulModifier event
    val tipJson = mapper.readTree(endpoint.receivedMessage.get(1))

    assertTrue(checkStaticResponseFields(tipJson, 0, -1, 0))

    assertTrue(tipJson.has("eventPayload"))
    var eventPayload = tipJson.get("eventPayload")
    assertTrue(eventPayload.has("height"))
    assertTrue(eventPayload.has("hash"))
    assertTrue(eventPayload.has("block"))

    //Check ChangedMempool event
    val mempoolJson = mapper.readTree(endpoint.receivedMessage.get(2))

    assertTrue(checkStaticResponseFields(mempoolJson, 0, -1, 2))

    assertTrue(mempoolJson.has("eventPayload"))
    eventPayload = mempoolJson.get("eventPayload")
    assertTrue(eventPayload.has("size"))
    assertEquals(2, eventPayload.get("size").asInt())
    assertTrue(eventPayload.has("transactions"))
    assertTrue(eventPayload.get("transactions").isArray)
    var nTx = 0
    eventPayload.get("transactions").forEach(tx => {
      nTx += 1
      assertEquals( "9e8d287524000a128f3d936ffdc1df1f2a54fa85a2800bc7b681e934d251efac", tx.asText())
    })
    assertEquals(2, nTx)

    session.close()
  }

  @Test
  def sessionTest():Unit = {
    // Test the handle of multiple client connections

    // Add client 1
    val cec = ClientEndpointConfig.Builder.create.build
    val client = ClientManager.createClient

    val endpoint = new WsEndpoint
    val session: Session = client.connectToServer(endpoint, cec, new URI("ws://localhost:9025/"))

    // Get block by hash
    val blockByHashtRequest = mapper.createObjectNode()
      .put("msgType", 1)
      .put("requestId",0)
      .put("requestType", 0)
    blockByHashtRequest.putObject("requestPayload").put("hash","some_block_hash")

    session.getBasicRemote.sendText(blockByHashtRequest.toString)
    Thread.sleep(3000)

    // Add client 2
    val cec2 = ClientEndpointConfig.Builder.create.build
    val client2 = ClientManager.createClient

    val endpoint2 = new WsEndpoint
    val session2: Session = client2.connectToServer(endpoint2, cec2, new URI("ws://localhost:9025/"))

    // Get raw mempool
    val rawMempoolRequest = mapper.createObjectNode()
      .put("msgType", 1)
      .put("requestId",1)
      .put("requestType", 5)
      .put("requestPayload", "{}")

    session2.getBasicRemote.sendText(rawMempoolRequest.toString)
    Thread.sleep(3000)

    // Send event
    val testActor = wsActorRef()
    Thread.sleep(5000)

    //Both client1 and client2 have 3 message (1 request and 2 events)
    assertEquals(3, endpoint.receivedMessage.size())
    assertEquals(3, endpoint2.receivedMessage.size())

    val request1 = mapper.readTree(endpoint.receivedMessage.get(0))
    val request2 = mapper.readTree(endpoint2.receivedMessage.get(0))

    assertTrue(checkStaticResponseFields(request1, 2, 0, 0))
    assertTrue(checkStaticResponseFields(request2, 2, 1, 5))

    val event1Of1 = mapper.readTree(endpoint.receivedMessage.get(1))
    val event1Of2 = mapper.readTree(endpoint2.receivedMessage.get(1))

    assertTrue(checkStaticResponseFields(event1Of1, 0, -1, 0))
    assertTrue(checkStaticResponseFields(event1Of2, 0, -1, 0))

    val event2Of1 = mapper.readTree(endpoint.receivedMessage.get(2))
    val event2Of2 = mapper.readTree(endpoint2.receivedMessage.get(2))

    assertTrue(checkStaticResponseFields(event2Of1, 0, -1, 2))
    assertTrue(checkStaticResponseFields(event2Of2, 0, -1, 2))

    // Disconnect client 2
    session2.close()

    // Resend event only on client 1
    val testActor2 = wsActorRef()
    Thread.sleep(5000)

    assertEquals(3, endpoint2.receivedMessage.size())
    assertEquals(5, endpoint.receivedMessage.size())

    val event4Of1 = mapper.readTree(endpoint.receivedMessage.get(3))
    assertTrue(checkStaticResponseFields(event4Of1, 0, -1, 0))
    val event5Of1 = mapper.readTree(endpoint.receivedMessage.get(4))
    assertTrue(checkStaticResponseFields(event5Of1, 0, -1, 2))

    // Disconnect client 1
    session.close()
  }

  class wsActor  ()
    extends Actor {

    override def receive: Receive = {
      case _  => {
      }
    }

    override def preStart(): Unit = {
      context.system.eventStream.publish(SemanticallySuccessfulModifier[scorex.core.PersistentNodeViewModifier](mock[scorex.core.PersistentNodeViewModifier]))
      Thread.sleep(2000)
      context.system.eventStream.publish(ChangedMempool[SidechainMemoryPool](mock[SidechainMemoryPool]))
      Thread.sleep(2000)
    }

  }
  object wsActorRef {

    def props()
             (implicit ec: ExecutionContext) : Props = {
      Props(new wsActor())
    }

    def apply()
             (implicit system: ActorSystem, ec: ExecutionContext): ActorRef =
      system.actorOf(props())

    def apply(name: String)
             (implicit system: ActorSystem, ec: ExecutionContext): ActorRef =
      system.actorOf(props(), name)
  }


  private def checkStaticResponseFields(json: JsonNode, msgType: Int, requestId: Int, answerType: Int): Boolean = {
    if (requestId == -1)
      json.has("msgType") &&
        msgType == json.get("msgType").asInt() &&
        json.has("answerType") &&
        answerType == json.get("answerType").asInt()
    else
    json.has("msgType") &&
      msgType == json.get("msgType").asInt() &&
      json.has("requestId") &&
      requestId == json.get("requestId").asInt() &&
      json.has("answerType") &&
      answerType == json.get("answerType").asInt()
  }
}

private class WsEndpoint extends Endpoint {
  var receivedMessage: util.ArrayList[String] = new util.ArrayList[String]()
  override def onOpen(session: Session, config: EndpointConfig): Unit = {
    session.addMessageHandler(new MessageHandler.Whole[String]() {
      override def onMessage(message: String): Unit = {
        receivedMessage.add(message)
      }
    })
  }
}
