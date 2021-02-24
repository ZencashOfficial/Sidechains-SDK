package com.horizen.websocket.client

import javax.websocket.{OnMessage, Session}
import javax.websocket.server.ServerEndpoint

@ServerEndpoint("/")
class WebSocketServerEchoEndpoint {

  @OnMessage
  def echo(session: Session, message: String): Unit = {
    session.getAsyncRemote.sendText(message)
  }

}
