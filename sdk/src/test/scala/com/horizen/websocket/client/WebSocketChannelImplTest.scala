package com.horizen.websocket.client

import com.horizen.WebSocketSettings
import org.glassfish.tyrus.server.Server
import org.junit.Assert.{assertEquals, assertFalse, _}
import org.junit.{Before, Test}
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.junit.JUnitSuite
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Await
import scala.concurrent.duration.{FiniteDuration, _}
import scala.util.{Failure, Success}

class WebSocketChannelImplTest extends JUnitSuite with MockitoSugar {

  private val serverHost = "localhost"
  private var serverPort: Int = _
  private var server: Server = _

  @Before
  def setUp(): Unit = {
    // start server on available port
    server = new Server(serverHost, 0, null, null, classOf[WebSocketServerEchoEndpoint])
    // get real port value
    serverPort = server.getPort
  }

  @Test
  def multipleStartConnectionOperations(): Unit = {
    server.stop()

    val mockedWebSocketMessageHandler: WebSocketMessageHandler = mock[WebSocketMessageHandler]

    server.start()

    val conf = WebSocketSettings(
      address = "ws://" + serverHost + ":" + serverPort,
      connectionTimeout = 10 milliseconds,
      reconnectionDelay = 0 seconds,
      reconnectionMaxAttempts = 1,
      wsServer = false,
      wsServerPort = 0
    )

    val defaultReconnectionHandler: WebSocketReconnectionHandler = new DefaultWebSocketReconnectionHandler(conf)

    val mockedWebSocketReconnectionHandler: WebSocketReconnectionHandler = mock[WebSocketReconnectionHandler]
    Mockito.when(mockedWebSocketReconnectionHandler.onDisconnection(ArgumentMatchers.any[DisconnectionCode.Value], ArgumentMatchers.any[String])).thenAnswer(asw =>
      defaultReconnectionHandler.onDisconnection(asw.getArgument(0), asw.getArgument(1)))
    Mockito.when(mockedWebSocketReconnectionHandler.onConnectionFailed(ArgumentMatchers.any[Throwable])).thenAnswer(asw =>
      defaultReconnectionHandler.onConnectionFailed(asw.getArgument(0)))
    Mockito.when(mockedWebSocketReconnectionHandler.getDelay).thenAnswer(_ => defaultReconnectionHandler.getDelay)

    val connector = new WebSocketConnectorImpl(conf.address, conf.connectionTimeout, mockedWebSocketMessageHandler, mockedWebSocketReconnectionHandler)

    val attempt_1 = connector.start()
    assertEquals(classOf[Success[_]], attempt_1.getClass)
    assertTrue("Web socket connector is not started.", connector.isStarted())

    val attempt_2 = connector.start()
    assertEquals(classOf[Failure[_]], attempt_2.getClass)
    assertTrue("Web socket connector is not started.", connector.isStarted())

    val attempt_3 = connector.start()
    assertEquals(classOf[Failure[_]], attempt_3.getClass)
    assertTrue("Web socket connector is not started.", connector.isStarted())

    // Post-conditions. All mocked methods are not called.
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onReceivedMessage(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onSendMessageErrorOccurred(ArgumentMatchers.any(), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onDisconnection(ArgumentMatchers.eq(DisconnectionCode.ON_SUCCESS), ArgumentMatchers.any())

    assertTrue("Web socket connector is not started.", connector.isStarted())

    server.stop()

  }

  @Test
  def multipleCloseConnectionOperationsWithoutStart(): Unit = {
    server.stop()

    val mockedWebSocketMessageHandler: WebSocketMessageHandler = mock[WebSocketMessageHandler]

    server.start()

    val conf = new WebSocketSettings(
      address = "ws://" + serverHost + ":" + serverPort,
      connectionTimeout = 10 milliseconds,
      reconnectionDelay = 0 seconds,
      reconnectionMaxAttempts = 1,
      wsServer = false,
      wsServerPort = 0
    )

    val defaultReconnectionHandler: WebSocketReconnectionHandler = new DefaultWebSocketReconnectionHandler(conf)

    val mockedWebSocketReconnectionHandler: WebSocketReconnectionHandler = mock[WebSocketReconnectionHandler]
    Mockito.when(mockedWebSocketReconnectionHandler.onDisconnection(ArgumentMatchers.any[DisconnectionCode.Value], ArgumentMatchers.any[String])).thenAnswer(asw =>
      defaultReconnectionHandler.onDisconnection(asw.getArgument(0), asw.getArgument(1)))
    Mockito.when(mockedWebSocketReconnectionHandler.onConnectionFailed(ArgumentMatchers.any[Throwable])).thenAnswer(asw =>
      defaultReconnectionHandler.onConnectionFailed(asw.getArgument(0)))
    Mockito.when(mockedWebSocketReconnectionHandler.getDelay).thenAnswer(_ => defaultReconnectionHandler.getDelay)

    val connector = new WebSocketConnectorImpl(conf.address, conf.connectionTimeout, mockedWebSocketMessageHandler, mockedWebSocketReconnectionHandler)

    assertFalse("Web socket connector is started.", connector.isStarted())

    assertFalse(connector.stop().isSuccess)
    assertFalse(connector.stop().isSuccess)
    assertFalse(connector.stop().isSuccess)
    assertFalse(connector.stop().isSuccess)

    // Post-conditions. All mocked methods are not called.
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onReceivedMessage(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onSendMessageErrorOccurred(ArgumentMatchers.any(), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onDisconnection(ArgumentMatchers.eq(DisconnectionCode.ON_SUCCESS), ArgumentMatchers.any())

    assertFalse("Web socket connector is started.", connector.isStarted())

    server.stop()

  }

  @Test
  def multipleCloseConnectionOperationsWithoutServerStarted(): Unit = {
    server.stop()

    val mockedWebSocketMessageHandler: WebSocketMessageHandler = mock[WebSocketMessageHandler]

    val conf = new WebSocketSettings(
      address = "ws://" + serverHost + ":" + serverPort,
      connectionTimeout = 10 milliseconds,
      reconnectionDelay = 0 seconds,
      reconnectionMaxAttempts = 1,
      wsServer = false,
      wsServerPort = 0
    )

    val defaultReconnectionHandler: WebSocketReconnectionHandler = new DefaultWebSocketReconnectionHandler(conf)

    val mockedWebSocketReconnectionHandler: WebSocketReconnectionHandler = mock[WebSocketReconnectionHandler]
    Mockito.when(mockedWebSocketReconnectionHandler.onDisconnection(ArgumentMatchers.any[DisconnectionCode.Value], ArgumentMatchers.any[String])).thenAnswer(asw =>
      defaultReconnectionHandler.onDisconnection(asw.getArgument(0), asw.getArgument(1)))
    Mockito.when(mockedWebSocketReconnectionHandler.onConnectionFailed(ArgumentMatchers.any[Throwable])).thenAnswer(asw =>
      defaultReconnectionHandler.onConnectionFailed(asw.getArgument(0)))
    Mockito.when(mockedWebSocketReconnectionHandler.getDelay).thenAnswer(_ => defaultReconnectionHandler.getDelay)

    val connector = new WebSocketConnectorImpl(conf.address, conf.connectionTimeout, mockedWebSocketMessageHandler, mockedWebSocketReconnectionHandler)

    assertFalse("Web socket connector is started.", connector.isStarted())

    assertFalse(connector.stop().isSuccess)
    assertFalse(connector.stop().isSuccess)
    assertFalse(connector.stop().isSuccess)
    assertFalse(connector.stop().isSuccess)

    // Post-conditions. All mocked methods are not called.
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onReceivedMessage(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onSendMessageErrorOccurred(ArgumentMatchers.any(), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onDisconnection(ArgumentMatchers.eq(DisconnectionCode.ON_SUCCESS), ArgumentMatchers.any())

    assertFalse("Web socket connector is started.", connector.isStarted())

  }

  @Test
  def multipleCloseConnectionOperations(): Unit = {
    server.stop()

    val mockedWebSocketMessageHandler: WebSocketMessageHandler = mock[WebSocketMessageHandler]

    server.start()

    val conf = new WebSocketSettings(
      address = "ws://" + serverHost + ":" + serverPort,
      connectionTimeout = 10 milliseconds,
      reconnectionDelay = 0 seconds,
      reconnectionMaxAttempts = 1,
      wsServer = false,
      wsServerPort = 0
    )

    val defaultReconnectionHandler: WebSocketReconnectionHandler = new DefaultWebSocketReconnectionHandler(conf)

    val mockedWebSocketReconnectionHandler: WebSocketReconnectionHandler = mock[WebSocketReconnectionHandler]
    Mockito.when(mockedWebSocketReconnectionHandler.onDisconnection(ArgumentMatchers.any[DisconnectionCode.Value], ArgumentMatchers.any[String])).thenAnswer(asw =>
      defaultReconnectionHandler.onDisconnection(asw.getArgument(0), asw.getArgument(1)))
    Mockito.when(mockedWebSocketReconnectionHandler.onConnectionFailed(ArgumentMatchers.any[Throwable])).thenAnswer(asw =>
      defaultReconnectionHandler.onConnectionFailed(asw.getArgument(0)))
    Mockito.when(mockedWebSocketReconnectionHandler.getDelay).thenAnswer(_ => defaultReconnectionHandler.getDelay)

    val connector = new WebSocketConnectorImpl(conf.address, conf.connectionTimeout, mockedWebSocketMessageHandler, mockedWebSocketReconnectionHandler)

    val startOp = connector.start()
    assertEquals(classOf[Success[_]], startOp.getClass)
    assertTrue("Web socket connector is not started.", connector.isStarted())

    assertTrue(connector.stop().isSuccess)
    assertTrue(connector.stop().isSuccess)
    assertTrue(connector.stop().isSuccess)
    assertTrue(connector.stop().isSuccess)

    // The 'onDisconnection' is called once. So, multiple calls of 'connector.close()' have no side effects
    // Other mocked methods are not called.
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onReceivedMessage(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onSendMessageErrorOccurred(ArgumentMatchers.any(), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.times(1)).onDisconnection(ArgumentMatchers.eq(DisconnectionCode.ON_SUCCESS), ArgumentMatchers.any())

    assertFalse("Web socket connector is started.", connector.isStarted())

    server.stop()

  }

  @Test
  def openAndCloseConnection(): Unit = {
    server.stop()

    val mockedWebSocketMessageHandler: WebSocketMessageHandler = mock[WebSocketMessageHandler]

    server.start()

    val conf = new WebSocketSettings(
      address = "ws://" + serverHost + ":" + serverPort,
      connectionTimeout = 10 milliseconds,
      reconnectionDelay = 1 seconds,
      reconnectionMaxAttempts = 1,
      wsServer = false,
      wsServerPort = 0
    )

    val defaultReconnectionHandler: WebSocketReconnectionHandler = new DefaultWebSocketReconnectionHandler(conf)

    val mockedWebSocketReconnectionHandler: WebSocketReconnectionHandler = mock[WebSocketReconnectionHandler]
    Mockito.when(mockedWebSocketReconnectionHandler.onDisconnection(ArgumentMatchers.any[DisconnectionCode.Value], ArgumentMatchers.any[String])).thenAnswer(asw =>
      defaultReconnectionHandler.onDisconnection(asw.getArgument(0), asw.getArgument(1)))
    Mockito.when(mockedWebSocketReconnectionHandler.onConnectionFailed(ArgumentMatchers.any[Throwable])).thenAnswer(asw =>
      defaultReconnectionHandler.onConnectionFailed(asw.getArgument(0)))
    Mockito.when(mockedWebSocketReconnectionHandler.getDelay).thenAnswer(_ => defaultReconnectionHandler.getDelay)

    val connector = new WebSocketConnectorImpl(conf.address, conf.connectionTimeout, mockedWebSocketMessageHandler, mockedWebSocketReconnectionHandler)

    val startOp = connector.start()
    assertEquals(classOf[Success[_]], startOp.getClass)
    assertTrue("Web socket connector is not started.", connector.isStarted())

    // Post-conditions. All mocked methods are not called.
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onReceivedMessage(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onSendMessageErrorOccurred(ArgumentMatchers.any(), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onDisconnection(ArgumentMatchers.any(), ArgumentMatchers.any())

    val newStartOp = connector.start()
    assertEquals(classOf[Failure[_]], newStartOp.getClass)
    assertTrue("Web socket connector is not started.", connector.isStarted())

    // Post-conditions. All mocked methods are not called.
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onReceivedMessage(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onSendMessageErrorOccurred(ArgumentMatchers.any(), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onDisconnection(ArgumentMatchers.any(), ArgumentMatchers.any())

    val stopOp = connector.stop()
    assertEquals("Connector already stopped.", classOf[Success[_]], stopOp.getClass)
    assertFalse("Web socket connector is started.", connector.isStarted())

    // The 'onDisconnection' is called once.
    // Other mocked methods are not called.
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onReceivedMessage(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onSendMessageErrorOccurred(ArgumentMatchers.any(), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.times(1)).onDisconnection(ArgumentMatchers.eq(DisconnectionCode.ON_SUCCESS), ArgumentMatchers.any())

    val newStopOp = connector.stop()
    assertEquals("Connector was started.", classOf[Success[_]], newStopOp.getClass)
    assertFalse("Web socket connector is started.", connector.isStarted())

    // The 'onDisconnection' is called once.
    // Other mocked methods are not called.
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onReceivedMessage(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onSendMessageErrorOccurred(ArgumentMatchers.any(), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.times(1)).onDisconnection(ArgumentMatchers.eq(DisconnectionCode.ON_SUCCESS), ArgumentMatchers.any())

    server.stop()

    // The 'onDisconnection' is called once.
    // Other mocked methods are not called.
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onReceivedMessage(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onSendMessageErrorOccurred(ArgumentMatchers.any(), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.times(1)).onDisconnection(ArgumentMatchers.eq(DisconnectionCode.ON_SUCCESS), ArgumentMatchers.any())

  }

  @Test
  def successEchoMessage(): Unit = {
    server.stop()

    val mockedWebSocketMessageHandler: WebSocketMessageHandler = mock[WebSocketMessageHandler]

    server.start()

    val conf = new WebSocketSettings(
      address = "ws://" + serverHost + ":" + serverPort,
      connectionTimeout = 10 milliseconds,
      reconnectionDelay = 1 seconds,
      reconnectionMaxAttempts = 3,
      wsServer = false,
      wsServerPort = 0
    )

    val defaultReconnectionHandler: WebSocketReconnectionHandler = new DefaultWebSocketReconnectionHandler(conf)

    val mockedWebSocketReconnectionHandler: WebSocketReconnectionHandler = mock[WebSocketReconnectionHandler]
    Mockito.when(mockedWebSocketReconnectionHandler.onDisconnection(ArgumentMatchers.any[DisconnectionCode.Value], ArgumentMatchers.any[String])).thenAnswer(asw =>
      defaultReconnectionHandler.onDisconnection(asw.getArgument(0), asw.getArgument(1)))
    Mockito.when(mockedWebSocketReconnectionHandler.onConnectionFailed(ArgumentMatchers.any[Throwable])).thenAnswer(asw =>
      defaultReconnectionHandler.onConnectionFailed(asw.getArgument(0)))
    Mockito.when(mockedWebSocketReconnectionHandler.getDelay).thenAnswer(_ => defaultReconnectionHandler.getDelay)

    val connector = new WebSocketConnectorImpl(conf.address, conf.connectionTimeout, mockedWebSocketMessageHandler, mockedWebSocketReconnectionHandler)

    val startOp = connector.start()
    assertEquals(classOf[Success[_]], startOp.getClass)
    assertTrue("Web socket connector is not started.", connector.isStarted())

    // Post-conditions. All mocked methods are not called.
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onReceivedMessage(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onSendMessageErrorOccurred(ArgumentMatchers.any(), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onDisconnection(ArgumentMatchers.any(), ArgumentMatchers.any())

    val message: String = "the message"

    connector.sendMessage(message)
    Thread.sleep(100)

    // Verify that the mocked handler received from the server the same message sent by the client.
    // Other mocked methods are not called.
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.times(1)).onReceivedMessage(ArgumentMatchers.eq(message))
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onSendMessageErrorOccurred(ArgumentMatchers.any(), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onDisconnection(ArgumentMatchers.any(), ArgumentMatchers.any())

    server.stop()
    Thread.sleep(100)

    /**
      * Post-conditions:
      *   - The mocked handler received from the server the same message sent by the client.
      *   - The 'onDisconnection' method is called once.
      *   - Other mocked methods are not called.
      */
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.times(1)).onReceivedMessage(ArgumentMatchers.eq(message))
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onSendMessageErrorOccurred(ArgumentMatchers.any(), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.times(1)).onDisconnection(ArgumentMatchers.eq(DisconnectionCode.UNEXPECTED), ArgumentMatchers.any())

  }

  @Test
  def connectionFailed(): Unit = {
    server.stop()

    val mockedWebSocketMessageHandler: WebSocketMessageHandler = mock[WebSocketMessageHandler]

    // We try to connect but the server is not started
    // Remember the parameter of the configuration 'reconnectionMaxAttempts = 3'
    val conf = new WebSocketSettings(
      address = "ws://" + serverHost + ":" + serverPort,
      connectionTimeout = 10 milliseconds,
      reconnectionDelay = 0 seconds,
      reconnectionMaxAttempts = 3,
      wsServer = false,
      wsServerPort = 0
    )

    val defaultReconnectionHandler: WebSocketReconnectionHandler = new DefaultWebSocketReconnectionHandler(conf)

    val mockedWebSocketReconnectionHandler: WebSocketReconnectionHandler = mock[WebSocketReconnectionHandler]
    Mockito.when(mockedWebSocketReconnectionHandler.onDisconnection(ArgumentMatchers.any[DisconnectionCode.Value], ArgumentMatchers.any[String])).thenAnswer(asw =>
      defaultReconnectionHandler.onDisconnection(asw.getArgument(0), asw.getArgument(1)))
    Mockito.when(mockedWebSocketReconnectionHandler.onConnectionFailed(ArgumentMatchers.any[Throwable])).thenAnswer(asw =>
      defaultReconnectionHandler.onConnectionFailed(asw.getArgument(0)))
    Mockito.when(mockedWebSocketReconnectionHandler.getDelay).thenAnswer(_ => defaultReconnectionHandler.getDelay)

    val connector = new WebSocketConnectorImpl(conf.address, conf.connectionTimeout, mockedWebSocketMessageHandler, mockedWebSocketReconnectionHandler)

    val startOp = connector.start()

    // Assert connection failed
    assertEquals(classOf[Failure[_]], startOp.getClass)
    assertTrue("Web socket connector is started.", !connector.isStarted)
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onReceivedMessage(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.times(4)).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onSendMessageErrorOccurred(ArgumentMatchers.any(), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onDisconnection(ArgumentMatchers.any(), ArgumentMatchers.any())

    val stopOp = connector.stop()
    assertEquals(classOf[Failure[_]], stopOp.getClass)
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onReceivedMessage(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.times(4)).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onSendMessageErrorOccurred(ArgumentMatchers.any(), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onDisconnection(ArgumentMatchers.any(), ArgumentMatchers.any())
  }

  @Test
  def connectionSuccessAfterConnectionFailed(): Unit = {
    server.stop()

    val mockedWebSocketMessageHandler: WebSocketMessageHandler = mock[WebSocketMessageHandler]

    // We try to connect but the server is not started
    // Remember the parameter of the configuration 'reconnectionMaxAttempts = 3'
    val conf = new WebSocketSettings(
      address = "ws://" + serverHost + ":" + serverPort,
      connectionTimeout = 10 milliseconds,
      reconnectionDelay = 0 seconds,
      reconnectionMaxAttempts = 3,
      wsServer = false,
      wsServerPort = 0
    )

    val defaultReconnectionHandler: WebSocketReconnectionHandler = new DefaultWebSocketReconnectionHandler(conf)

    val mockedWebSocketReconnectionHandler: WebSocketReconnectionHandler = mock[WebSocketReconnectionHandler]
    Mockito.when(mockedWebSocketReconnectionHandler.onDisconnection(ArgumentMatchers.any[DisconnectionCode.Value], ArgumentMatchers.any[String])).thenAnswer(asw =>
      defaultReconnectionHandler.onDisconnection(asw.getArgument(0), asw.getArgument(1)))
    Mockito.when(mockedWebSocketReconnectionHandler.onConnectionFailed(ArgumentMatchers.any[Throwable])).thenAnswer(asw =>
      defaultReconnectionHandler.onConnectionFailed(asw.getArgument(0)))
    Mockito.when(mockedWebSocketReconnectionHandler.getDelay).thenAnswer(_ => defaultReconnectionHandler.getDelay)

    val connector = new WebSocketConnectorImpl(conf.address, conf.connectionTimeout, mockedWebSocketMessageHandler, mockedWebSocketReconnectionHandler)

    val startOp = connector.start()

    // Assert connection failed
    assertEquals(classOf[Failure[_]], startOp.getClass)
    assertTrue("Web socket connector is started.", !connector.isStarted)

    // Verify that the 'onConnectFailure' method of the mocked handler is called 3 times (value provided by 'reconnectionMaxAttempts').
    // Other mocked methods are not called.
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onReceivedMessage(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.times(4)).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onSendMessageErrorOccurred(ArgumentMatchers.any(), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onDisconnection(ArgumentMatchers.any(), ArgumentMatchers.any())

    // now we start the server
    server.start()

    val newStartOp = connector.start()

    // Assert connection not failed
    assertEquals(classOf[Success[_]], newStartOp.getClass)
    assertTrue("Web socket connector is not started.", connector.isStarted)

    val message: String = "a message"
    connector.sendMessage(message)
    Thread.sleep(100)

    Mockito.verify(mockedWebSocketMessageHandler, Mockito.times(1)).onReceivedMessage(ArgumentMatchers.eq(message))
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.times(4)).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onSendMessageErrorOccurred(ArgumentMatchers.any(), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onDisconnection(ArgumentMatchers.any(), ArgumentMatchers.any())


    val stopOp = connector.stop()
    assertEquals(classOf[Success[_]], stopOp.getClass)
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.times(1)).onReceivedMessage(ArgumentMatchers.eq(message))
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.times(4)).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onSendMessageErrorOccurred(ArgumentMatchers.any(), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.times(1)).onDisconnection(ArgumentMatchers.eq(DisconnectionCode.ON_SUCCESS), ArgumentMatchers.any())

    server.stop()
  }

  @Test
  def failEchoMessage(): Unit = {
    server.stop()

    val mockedWebSocketMessageHandler: WebSocketMessageHandler = mock[WebSocketMessageHandler]

    server.start()

    val conf = new WebSocketSettings(
      address = "ws://" + serverHost + ":" + serverPort,
      connectionTimeout = 10 milliseconds,
      reconnectionDelay = 1 seconds,
      reconnectionMaxAttempts = 2,
      wsServer = false,
      wsServerPort = 0
    )

    val defaultReconnectionHandler: WebSocketReconnectionHandler = new DefaultWebSocketReconnectionHandler(conf)

    val mockedWebSocketReconnectionHandler: WebSocketReconnectionHandler = mock[WebSocketReconnectionHandler]
    Mockito.when(mockedWebSocketReconnectionHandler.onDisconnection(ArgumentMatchers.any[DisconnectionCode.Value], ArgumentMatchers.any[String])).thenAnswer(asw =>
      defaultReconnectionHandler.onDisconnection(asw.getArgument(0), asw.getArgument(1)))
    Mockito.when(mockedWebSocketReconnectionHandler.onConnectionFailed(ArgumentMatchers.any[Throwable])).thenAnswer(asw =>
      defaultReconnectionHandler.onConnectionFailed(asw.getArgument(0)))
    Mockito.when(mockedWebSocketReconnectionHandler.getDelay).thenAnswer(_ => defaultReconnectionHandler.getDelay)

    val connector = new WebSocketConnectorImpl(conf.address, conf.connectionTimeout, mockedWebSocketMessageHandler, mockedWebSocketReconnectionHandler)

    val startOp = connector.start()
    assertEquals(classOf[Success[_]], startOp.getClass)
    assertTrue("Web socket connector is not started.", connector.isStarted())

    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onReceivedMessage(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onSendMessageErrorOccurred(ArgumentMatchers.any(), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onDisconnection(ArgumentMatchers.any(), ArgumentMatchers.any())

    val message: String = "the message"

    server.stop()
    Thread.sleep(100)
    connector.sendMessage(message)
    Thread.sleep(100)

    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onReceivedMessage(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.times(1)).onSendMessageErrorOccurred(ArgumentMatchers.eq(message), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.times(1)).onDisconnection(ArgumentMatchers.eq(DisconnectionCode.UNEXPECTED), ArgumentMatchers.any())

    val closeOp = connector.stop()
    assertEquals(classOf[Success[_]], closeOp.getClass)
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onReceivedMessage(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.times(1)).onSendMessageErrorOccurred(ArgumentMatchers.eq(message), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.times(1)).onDisconnection(ArgumentMatchers.eq(DisconnectionCode.UNEXPECTED), ArgumentMatchers.any())
  }

  @Test
  def asyncConnectorStart(): Unit = {
    server.stop()

    val mockedWebSocketMessageHandler: WebSocketMessageHandler = mock[WebSocketMessageHandler]

    val conf = new WebSocketSettings(
      address = "ws://" + serverHost + ":" + serverPort,
      connectionTimeout = 10 milliseconds,
      reconnectionDelay = 1 seconds,
      reconnectionMaxAttempts = 3,
      wsServer = false,
      wsServerPort = 0
    )

    val defaultReconnectionHandler: WebSocketReconnectionHandler = new DefaultWebSocketReconnectionHandler(conf)

    val mockedWebSocketReconnectionHandler: WebSocketReconnectionHandler = mock[WebSocketReconnectionHandler]
    Mockito.when(mockedWebSocketReconnectionHandler.onDisconnection(ArgumentMatchers.any[DisconnectionCode.Value], ArgumentMatchers.any[String])).thenAnswer(asw =>
      defaultReconnectionHandler.onDisconnection(asw.getArgument(0), asw.getArgument(1)))
    Mockito.when(mockedWebSocketReconnectionHandler.onConnectionFailed(ArgumentMatchers.any[Throwable])).thenAnswer(asw =>
      defaultReconnectionHandler.onConnectionFailed(asw.getArgument(0)))
    Mockito.when(mockedWebSocketReconnectionHandler.getDelay).thenAnswer(_ => defaultReconnectionHandler.getDelay)

    val connector = new WebSocketConnectorImpl(conf.address, conf.connectionTimeout, mockedWebSocketMessageHandler, mockedWebSocketReconnectionHandler)

    server.start()

    val futureOfChannel = connector.asyncStart()
    assertFalse("The connector is started.", connector.isStarted)

    val timeout: FiniteDuration = 4 seconds
    val fut = Await.result(futureOfChannel, timeout)

    assertTrue("The connector is not started.", connector.isStarted)
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onReceivedMessage(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onSendMessageErrorOccurred(ArgumentMatchers.any(), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onDisconnection(ArgumentMatchers.any(), ArgumentMatchers.any())

    server.stop()
    Thread.sleep(100)

    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onReceivedMessage(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onSendMessageErrorOccurred(ArgumentMatchers.any(), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.times(1)).onDisconnection(ArgumentMatchers.eq(DisconnectionCode.UNEXPECTED), ArgumentMatchers.any())
  }

  //@Test
  // NOTE: Doesn't work pon Windows as expected. Seems, that conf.connectionTimeout value is ignored.
  // TO DO: verify why and restore the test.
  def asyncConnectorRetrySuccess(): Unit = {
    server.stop()

    val mockedWebSocketMessageHandler: WebSocketMessageHandler = mock[WebSocketMessageHandler]

    val conf = new WebSocketSettings(
      address = "ws://" + serverHost + ":" + serverPort,
      connectionTimeout = 10 milliseconds,
      reconnectionDelay = 1 seconds,
      reconnectionMaxAttempts = 3,
      wsServer = false,
      wsServerPort = 0
    )

    val defaultReconnectionHandler: WebSocketReconnectionHandler = new DefaultWebSocketReconnectionHandler(conf)

    val mockedWebSocketReconnectionHandler: WebSocketReconnectionHandler = mock[WebSocketReconnectionHandler]
    Mockito.when(mockedWebSocketReconnectionHandler.onDisconnection(ArgumentMatchers.any[DisconnectionCode.Value], ArgumentMatchers.any[String])).thenAnswer(asw =>
      defaultReconnectionHandler.onDisconnection(asw.getArgument(0), asw.getArgument(1)))
    Mockito.when(mockedWebSocketReconnectionHandler.onConnectionFailed(ArgumentMatchers.any[Throwable])).thenAnswer(asw =>
      defaultReconnectionHandler.onConnectionFailed(asw.getArgument(0)))
    Mockito.when(mockedWebSocketReconnectionHandler.getDelay).thenAnswer(_ => defaultReconnectionHandler.getDelay)

    val connector = new WebSocketConnectorImpl(conf.address, conf.connectionTimeout, mockedWebSocketMessageHandler, mockedWebSocketReconnectionHandler)

    val futureOfChannel = connector.asyncStart()
    assertFalse("The connector is started.", connector.isStarted)
    Thread.sleep(1000)

    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onReceivedMessage(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.atLeastOnce()).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onSendMessageErrorOccurred(ArgumentMatchers.any(), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onDisconnection(ArgumentMatchers.any(), ArgumentMatchers.any())

    assertFalse("The connector is started.", connector.isStarted)
    server.start()

    try {
      val timeout: FiniteDuration = 5 seconds
      val fut = Await.result(futureOfChannel, timeout)
    } catch {
      case e: Throwable =>
        server.stop()
        fail(e)
    }

    assertTrue("The connector is not started.", connector.isStarted)
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onReceivedMessage(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.atLeastOnce()).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.atMost(3)).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onSendMessageErrorOccurred(ArgumentMatchers.any(), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onDisconnection(ArgumentMatchers.any(), ArgumentMatchers.any())

    server.stop()
    Thread.sleep(100)

    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onReceivedMessage(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.atLeastOnce()).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.atMost(3)).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onSendMessageErrorOccurred(ArgumentMatchers.any(), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.times(1)).onDisconnection(ArgumentMatchers.eq(DisconnectionCode.UNEXPECTED), ArgumentMatchers.any())

  }

  @Test
  def receiveMultipleMessages(): Unit = {
    server.stop()

    val mockedWebSocketMessageHandler: WebSocketMessageHandler = mock[WebSocketMessageHandler]

    val pushServerHost = "localhost"

    var pushServer: Server = new Server(pushServerHost, 0, null, null, classOf[WebSocketServerPushEndpoint])
    val pushServerPort = pushServer.getPort

    pushServer.start()

    val conf = new WebSocketSettings(
      address = "ws://" + pushServerHost + ":" + pushServerPort,
      connectionTimeout = 10 milliseconds,
      reconnectionDelay = 0 seconds,
      reconnectionMaxAttempts = 2,
      wsServer = false,
      wsServerPort = 0
    )

    val defaultReconnectionHandler: WebSocketReconnectionHandler = new DefaultWebSocketReconnectionHandler(conf)

    val mockedWebSocketReconnectionHandler: WebSocketReconnectionHandler = mock[WebSocketReconnectionHandler]
    Mockito.when(mockedWebSocketReconnectionHandler.onDisconnection(ArgumentMatchers.any[DisconnectionCode.Value], ArgumentMatchers.any[String])).thenAnswer(asw =>
      defaultReconnectionHandler.onDisconnection(asw.getArgument(0), asw.getArgument(1)))
    Mockito.when(mockedWebSocketReconnectionHandler.onConnectionFailed(ArgumentMatchers.any[Throwable])).thenAnswer(asw =>
      defaultReconnectionHandler.onConnectionFailed(asw.getArgument(0)))
    Mockito.when(mockedWebSocketReconnectionHandler.getDelay).thenAnswer(_ => defaultReconnectionHandler.getDelay)

    val connector = new WebSocketConnectorImpl(conf.address, conf.connectionTimeout, mockedWebSocketMessageHandler, mockedWebSocketReconnectionHandler)

    val pushedMessages = 5

    val startOp = connector.start()
    assertEquals(classOf[Success[_]], startOp.getClass)
    assertTrue("Web socket connector is not started.", connector.isStarted())

    Thread.sleep(200)


    // Verify that the mocked handler received from the server the 'pushedMessages' number of messages.
    // Other mocked methods are not called.
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.times(pushedMessages)).onReceivedMessage(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onSendMessageErrorOccurred(ArgumentMatchers.any(), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onDisconnection(ArgumentMatchers.any(), ArgumentMatchers.any())

    val stopOp = connector.stop()
    assertEquals(classOf[Success[_]], stopOp.getClass)
    assertFalse("Web socket connector is started.", connector.isStarted())

    /**
      * Post-conditions:
      *   - The mocked handler received from the server the 'pushedMessages' number of messages.
      *   - The 'onDisconnection' method is called once.
      *   - Other mocked methods are not called.
      */
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.times(pushedMessages)).onReceivedMessage(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.never()).onConnectionFailed(ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketMessageHandler, Mockito.never()).onSendMessageErrorOccurred(ArgumentMatchers.any(), ArgumentMatchers.any())
    Mockito.verify(mockedWebSocketReconnectionHandler, Mockito.times(1)).onDisconnection(ArgumentMatchers.eq(DisconnectionCode.ON_SUCCESS), ArgumentMatchers.any())

    pushServer.stop()

  }

}
