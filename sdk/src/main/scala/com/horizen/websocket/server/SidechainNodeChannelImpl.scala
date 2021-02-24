package com.horizen.websocket.server

import java.util

import scorex.util.{ModifierId, ScorexLogging}
import akka.pattern.ask
import akka.util.Timeout
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.horizen.{SidechainHistory, SidechainMemoryPool, SidechainState, SidechainSyncInfo, SidechainWallet}
import com.horizen.block.SidechainBlock
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try
import com.horizen.serialization.SerializationUtil
import scorex.core.NodeViewHolder.CurrentView
import scorex.core.NodeViewHolder.ReceivableMessages.GetDataFromCurrentView


class SidechainNodeChannelImpl() extends SidechainNodeChannel with ScorexLogging{

  implicit val duration: Timeout = 20 seconds
  implicit val ec:ExecutionContext = ExecutionContext.Implicits.global
  private val mapper = new ObjectMapper().registerModule(DefaultScalaModule)

  type View = CurrentView[SidechainHistory, SidechainState, SidechainWallet, SidechainMemoryPool]
  protected def viewAsync(): Future[View] = {
    def f(v: View) = v

    (WebSocketServerRef.sidechainNodeViewHolderRef ? GetDataFromCurrentView[SidechainHistory, SidechainState, SidechainWallet, SidechainMemoryPool, View](f))
      .mapTo[View]
  }


  override def getBlockByHeight(height: Int): Try[ObjectNode] = Try {
    val responsePayload = mapper.createObjectNode()
    val sidechainNodeView: View = Await.result(viewAsync(), 5000 millis)

    //get block hash by id
    val sidechainBlockHash = sidechainNodeView.history.blockIdByHeight(height).get
    //get block by hash
    val sblock = sidechainNodeView.history.modifierById(sidechainBlockHash.asInstanceOf[ModifierId]).get
    //serialize JSON
    val blockJson = mapper.readTree(SerializationUtil.serializeWithResult(sblock))
    responsePayload.put("block", blockJson.get("result"))
    responsePayload.put("hash",sidechainBlockHash)
    responsePayload.put("height",height)
    responsePayload
  }

  override def getBlockByHash(hash: String): Try[ObjectNode] = Try {
    val responsePayload = mapper.createObjectNode()
    val sidechainNodeView: View = Await.result(viewAsync(), 5000 millis)

    //get block by hash
    val sidechainBlock = sidechainNodeView.history.modifierById(hash.asInstanceOf[ModifierId]).get
    val height = sidechainNodeView.history.blockInfoById(hash.asInstanceOf[ModifierId]).height
    //serialize JSON
    val blockJson = mapper.readTree(SerializationUtil.serializeWithResult(sidechainBlock))
    responsePayload.put("block", blockJson.get("result"))
    responsePayload.put("hash",hash)
    responsePayload.put("height",height)

    responsePayload
  }

  override def getNewBlockHashes(locatorHashes: Seq[String], limit: Int): Try[ObjectNode] = Try {
    var lastHeight = -1
    val responsePayload = mapper.createObjectNode()

    val sidechainNodeView: View = Await.result(viewAsync(), 5000 millis)
    var scInfo: SidechainSyncInfo = new SidechainSyncInfo(locatorHashes.asInstanceOf[Seq[ModifierId]])
    var headerList = sidechainNodeView.history.continuationIds(scInfo ,limit)
    if (headerList.size > 0) {
      lastHeight = sidechainNodeView.history.blockInfoById(headerList.last._2.asInstanceOf[ModifierId]).height
    }

    val hashes = mapper.readTree(SerializationUtil.serializeWithResult(headerList.map(el =>el._2)))
    responsePayload.put("height",lastHeight)
    responsePayload.put("hashes", hashes.get("result"))

    responsePayload
  }

  override def getMempoolTxs(txids: Seq[String]): Try[ObjectNode] = Try {
    val responsePayload = mapper.createObjectNode()
    val sidechainNodeView: View = Await.result(viewAsync(), 5000 millis)

    val txs = sidechainNodeView.pool.getAll(txids.asInstanceOf[Seq[ModifierId]])

    val txsJson = mapper.readTree(SerializationUtil.serializeWithResult(txs))
    responsePayload.put("transactions",txsJson.get("result"))
    responsePayload
  }

  override def getRawMempool(): Try[ObjectNode] = Try {
    val responsePayload = mapper.createObjectNode()
    val sidechainNodeView: View = Await.result(viewAsync(), 5000 millis)

    val txids: util.ArrayList[String] = new util.ArrayList[String]()
    sidechainNodeView.pool.take(sidechainNodeView.pool.size).foreach(txs => txids.add(txs.id()))

    val json = mapper.readTree(SerializationUtil.serializeWithResult(txids.toArray()))

    responsePayload.put("transactions",json.get("result"))
    responsePayload.put("size", txids.size())

    responsePayload
  }

  override def getBestBlock(): Try[ObjectNode] = Try {
    val eventPayload = mapper.createObjectNode()
    val sidechainNodeView: View = Await.result(viewAsync(), 5000 millis)

    val bestBlock = sidechainNodeView.history.bestBlock
    val height = sidechainNodeView.history.getCurrentHeight
    val blockJson = mapper.readTree(SerializationUtil.serializeWithResult(bestBlock))

    eventPayload.put("height",height)
    eventPayload.put("hash",bestBlock.id)
    eventPayload.put("block",blockJson.get("result"))

    eventPayload
  }

}
