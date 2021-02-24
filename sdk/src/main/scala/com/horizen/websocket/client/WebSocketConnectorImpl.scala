package com.horizen.websocket.client

import java.net.URI

import javax.websocket._
import org.glassfish.tyrus.client.{ClientManager, ClientProperties}
import scorex.util.ScorexLogging

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, Promise}
import scala.util.Try

@ClientEndpoint
class WebSocketConnectorImpl(bindAddress: String, connectionTimeout: FiniteDuration, messageHandler: WebSocketMessageHandler, reconnectionHandler: WebSocketReconnectionHandler) extends WebSocketConnector with WebSocketChannel with ScorexLogging {

  private var userSession: Session = _
  private val client = ClientManager.createClient()
  private val reconnectHandler: ClientManager.ReconnectHandler = new ClientManager.ReconnectHandler() {
    /**
      * Delay before next connection attempt.
      */
    override def getDelay: Long = {
      reconnectionHandler.getDelay.toSeconds
    }

    // will be executed whenever @OnClose annotated method (or Endpoint.onClose(..)) is executed on client side.
    // this should happen when established connection is lost for any reason
    override def onDisconnect(closeReason: CloseReason): Boolean = {
      log.info("onDisconnect. Reason: " + closeReason.toString)
      if (closeReason.getCloseCode.getCode == 1000)
        reconnectionHandler.onDisconnection(DisconnectionCode.ON_SUCCESS, closeReason.getReasonPhrase)
      else
        reconnectionHandler.onDisconnection(DisconnectionCode.UNEXPECTED, closeReason.getReasonPhrase)
    }

    // is invoked when client fails to connect to remote endpoint
    override def onConnectFailure(exception: Exception): Boolean = reconnectionHandler.onConnectionFailed(exception)
  }

  override def isStarted: Boolean =
    userSession != null && userSession.isOpen

  override def start(): Try[Unit] = Try {

    if (isStarted) throw new IllegalStateException("Connector is already started.")

    client.getProperties.put(ClientProperties.RECONNECT_HANDLER, reconnectHandler)
    client.getProperties.put(ClientProperties.HANDSHAKE_TIMEOUT, String.valueOf(connectionTimeout.toMillis))
    log.info("Starting web socket connector...")
    userSession = client.connectToServer(this, new URI(bindAddress))
    reconnectionHandler.onConnectionSuccess()
    log.info("Web socket connector started.")

    userSession.addMessageHandler(new MessageHandler.Whole[String]() {
      override def onMessage(t: String): Unit = {
        log.info("Message received from server: " + t)
        messageHandler.onReceivedMessage(t)
      }
    })

  }

  override def asyncStart(): Future[Try[Unit]] = {
    val promise: Promise[Try[Unit]] = Promise[Try[Unit]]

    new Thread(new Runnable {
      override def run(): Unit = {
        promise.success(start())
      }
    }).start()

    promise.future
  }

  override def stop(): Try[Unit] = Try {
    log.info("Stopping web socket connector...")
    userSession.close()
    log.info("Web socket connector stopped.")
  }

  override def sendMessage(message: String): Unit = {
    try {
      userSession.getAsyncRemote().sendText(message, new SendHandler {
        override def onResult(sendResult: SendResult): Unit = {
          if (!sendResult.isOK) {
            log.info("Send message failed.")
            messageHandler.onSendMessageErrorOccurred(message, sendResult.getException)
          }
          else log.info("Message sent")
        }
      }
      )
    } catch {
      case e: Throwable => messageHandler.onSendMessageErrorOccurred(message, e)
    }

  }

}
