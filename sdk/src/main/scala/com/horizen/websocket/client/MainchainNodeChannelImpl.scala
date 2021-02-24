package com.horizen.websocket.client

import com.horizen.block.MainchainBlockReference
import com.horizen.mainchain.api.{SendCertificateRequest, SendCertificateResponse}
import com.horizen.params.NetworkParams
import com.horizen.utils.BytesUtils

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}


case class GetBlockByHeightRequestPayload(height: Int) extends RequestPayload
case class GetBlockByHashRequestPayload(hash: String) extends RequestPayload
case class GetBlocksAfterHeightRequestPayload(afterHeight: Int, limit: Int) extends RequestPayload
case class GetBlocksAfterHashRequestPayload(afterHash: String, limit: Int) extends RequestPayload
case class GetNewBlocksRequestPayload(locatorHashes: Seq[String], limit: Int) extends RequestPayload
case class BackwardTransfer(pubkeyhash: String, amount: String)
case class SendCertificateRequestPayload(scid: String, epochNumber: Int, quality: Long, endEpochBlockHash: String,
                                        scProof: String, backwardTransfers: Seq[BackwardTransfer]) extends RequestPayload


case class BlockResponsePayload(height: Int, hash: String, block: String) extends ResponsePayload
case class BlocksResponsePayload(height: Int, hashes: Seq[String]) extends ResponsePayload
case class NewBlocksResponsePayload(height: Int, hashes: Seq[String]) extends ResponsePayload
case class CertificateResponsePayload(certificateHash: String) extends ResponsePayload


class MainchainNodeChannelImpl(client: CommunicationClient, params: NetworkParams) extends MainchainNodeChannel { // to do: define EC inside?

  override def getBlockByHeight(height: Int): Try[MainchainBlockReference] = Try {
    val future: Future[BlockResponsePayload] =
      client.sendRequest(0, GetBlockByHeightRequestPayload(height), classOf[BlockResponsePayload])

    processBlockResponsePayload(future).get
  }

  override def getBlockByHash(hash: String): Try[MainchainBlockReference] = Try {
    val future: Future[BlockResponsePayload] =
      client.sendRequest(0, GetBlockByHashRequestPayload(hash), classOf[BlockResponsePayload])

    processBlockResponsePayload(future).get
  }

  private def processBlockResponsePayload(future: Future[BlockResponsePayload]): Try[MainchainBlockReference] = Try {
    val response: BlockResponsePayload = Await.result(future, client.requestTimeoutDuration())
    val blockBytes = BytesUtils.fromHexString(response.block)
    MainchainBlockReference.create(blockBytes, params).get
  }

  def getBlockHashesAfterHeight(height: Int, limit: Int): Try[Seq[String]] = Try {
    val future: Future[BlocksResponsePayload] =
      client.sendRequest(1, GetBlocksAfterHeightRequestPayload(height, limit), classOf[BlocksResponsePayload])

    val response: BlocksResponsePayload = Await.result(future, client.requestTimeoutDuration())
    response.hashes
  }

  def getBlockHashesAfterHash(hash: String, limit: Int): Try[Seq[String]] = Try {
    val future: Future[BlocksResponsePayload] =
      client.sendRequest(1, GetBlocksAfterHashRequestPayload(hash, limit), classOf[BlocksResponsePayload])

    val response: BlocksResponsePayload = Await.result(future, client.requestTimeoutDuration())
    response.hashes
  }


  override def getNewBlockHashes(locatorHashes: Seq[String], limit: Int): Try[(Int, Seq[String])] = Try {
    val future: Future[NewBlocksResponsePayload] =
      client.sendRequest(2, GetNewBlocksRequestPayload(locatorHashes, limit), classOf[NewBlocksResponsePayload])

    val response: NewBlocksResponsePayload = Await.result(future, client.requestTimeoutDuration())
    (response.height, response.hashes)
  }

  override def getBestCommonPoint(locatorHashes: Seq[String]): Try[(Int, String)] = {
    getNewBlockHashes(locatorHashes, 1) match {
      case Success((height, hashes)) => Success(height, hashes.head)
      case Failure(ex) => throw ex
    }
  }

  override def subscribeOnUpdateTipEvent(handler: OnUpdateTipEventHandler): Try[Unit] = {
    client.registerEventHandler(0, handler, classOf[OnUpdateTipEventPayload])
  }

  override def unsubscribeOnUpdateTipEvent(handler: OnUpdateTipEventHandler): Unit = {
    client.unregisterEventHandler(0, handler)
  }

  override def sendCertificate(certificateRequest: SendCertificateRequest): Try[SendCertificateResponse] = Try {
    val backwardTransfer:Seq[BackwardTransfer] = certificateRequest.backwardTransfers.map(bt => BackwardTransfer(BytesUtils.toHexString(bt.pubkeyhash), bt.amount))

    val requestPayload: SendCertificateRequestPayload = SendCertificateRequestPayload(BytesUtils.toHexString(certificateRequest.sidechainId),
      certificateRequest.epochNumber, certificateRequest.quality, BytesUtils.toHexString(certificateRequest.endEpochBlockHash),
      BytesUtils.toHexString(certificateRequest.proofBytes), backwardTransfer)

    val future: Future[CertificateResponsePayload] = client.sendRequest(3, requestPayload, classOf[CertificateResponsePayload])

    processCertificateResponsePayload(future)
  }

  private def processCertificateResponsePayload(future: Future[CertificateResponsePayload]): SendCertificateResponse = {
    val response: CertificateResponsePayload = Await.result(future, client.requestTimeoutDuration())
    SendCertificateResponse(BytesUtils.fromHexString(response.certificateHash))
  }
}
