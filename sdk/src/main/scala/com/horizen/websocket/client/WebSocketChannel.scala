package com.horizen.websocket.client

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

object DisconnectionCode extends Enumeration {
  val ON_SUCCESS = Value(0)
  val UNEXPECTED = Value(1)
}

trait WebSocketMessageHandler {
  def onReceivedMessage(message: String): Unit

  def onSendMessageErrorOccurred(message: String, cause: Throwable): Unit
}

trait WebSocketReconnectionHandler {
  // when client fails to connect to remote endpoint
  def onConnectionFailed(cause: Throwable): Boolean

  // when established connection is lost for any reason
  def onDisconnection(code: DisconnectionCode.Value, reason: String): Boolean

  // Delay before next connection attempt
  def getDelay: FiniteDuration

  def onConnectionSuccess(): Unit
}

trait  WebSocketConnector{
  def isStarted(): Boolean

  def start(): Try[Unit]

  def asyncStart(): Future[Try[Unit]]

  def stop(): Try[Unit]
}

trait WebSocketChannel {
  def sendMessage(message: String): Unit
}
