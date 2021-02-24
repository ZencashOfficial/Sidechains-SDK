package com.horizen.websocket.server

import com.fasterxml.jackson.databind.node.ObjectNode

import scala.util.Try

trait SidechainNodeChannel {
  // Get Sidechain block by height
  def getBlockByHeight(height: Int): Try[ObjectNode]

  // Get Sidechain block by hash
  def getBlockByHash(hash: String): Try[ObjectNode]

  // For given locator find the best known block in SC active chain - common point.
  // Then return common point height and seq of block hashes up to `limit` elements starting from common point.
  def getNewBlockHashes(locatorHashes: Seq[String], limit: Int): Try[ObjectNode]

  // Get mempool transaction based and the hashes provided
  def getMempoolTxs(txids: Seq[String]): Try[ObjectNode]

  // Get the current hashes of transactions in mempool
  def getRawMempool(): Try[ObjectNode]

  // Get current best block
  def getBestBlock(): Try[ObjectNode]

}
