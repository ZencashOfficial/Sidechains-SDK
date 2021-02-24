package com.horizen.websocket.client

import javax.websocket.server.ServerEndpoint
import javax.websocket.{OnOpen, Session}

@ServerEndpoint("/")
class WebSocketServerPushEndpoint {

  @OnOpen
  def onOpen(session: Session): Unit = {
    for (i <- 1 to 5){
      session.getAsyncRemote.sendText("Message #"+i)
    }
  }

}
