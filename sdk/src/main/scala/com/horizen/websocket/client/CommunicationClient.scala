package com.horizen.websocket.client

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

trait EventPayload

trait EventHandler[E <: EventPayload] {
  def onEvent(eventPayload: E): Unit
}

trait RequestPayload

trait ResponsePayload

trait CommunicationClient {
  def sendRequest[Req <: RequestPayload, Resp <: ResponsePayload](requestType: Int, request: Req, responseClazz: Class[Resp]): Future[Resp]

  def registerEventHandler[E <: EventPayload](eventType: Int, handler: EventHandler[E], eventClazz: Class[E]): Try[Unit]

  def unregisterEventHandler[E <: EventPayload](eventType: Int, handler: EventHandler[E]): Unit

  def requestTimeoutDuration(): FiniteDuration
}

trait WebSocketChannelCommunicationClient extends CommunicationClient {
  def setWebSocketChannel(channel : WebSocketChannel)
}
