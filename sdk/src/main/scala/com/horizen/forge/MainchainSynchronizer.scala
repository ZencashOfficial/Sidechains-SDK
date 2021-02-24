package com.horizen.forge

import com.horizen.SidechainHistory
import com.horizen.block.MainchainBlockReference
import com.horizen.chain.{MainchainHeaderHash, byteArrayToMainchainHeaderHash}
import com.horizen.utils.BytesUtils
import com.horizen.utils._
import com.horizen.websocket.client.MainchainNodeChannel

import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

class MainchainSynchronizer(mainchainNodeChannel: MainchainNodeChannel) {
  // Get divergent mainchain suffix between SC Node and MC Node
  // Return last common header with height + divergent suffix
  def getMainchainDivergentSuffix(history: SidechainHistory, limit: Int): Try[(Int, Seq[MainchainHeaderHash])] = Try {
    val (_: Int, commonHashHex: String) = getMainchainCommonBlockHashAndHeight(history).get
    mainchainNodeChannel.getNewBlockHashes(Seq(commonHashHex), limit) match {
      case Success((height, hashes)) => (height, hashes.map(hex => byteArrayToMainchainHeaderHash(BytesUtils.fromHexString(hex))))
      case Failure(ex) => throw ex
    }
  }

  // Return common block height and hash as a hex string.
  def getMainchainCommonBlockHashAndHeight(history: SidechainHistory): Try[(Int, String)] = Try {
    // Bitcoin-style Locator is ordered from tip to genesis
    val locatorHashes: Seq[String] = history.getMainchainHashesLocator.map(baw => BytesUtils.toHexString(baw.data))
    val (commonHeight, commonHashHex) = mainchainNodeChannel.getBestCommonPoint(locatorHashes).get
    val commonHash: MainchainHeaderHash = byteArrayToMainchainHeaderHash(BytesUtils.fromHexString(commonHashHex))

    if(commonHashHex == locatorHashes.head) {
      // No orphan mainchain blocks -> return result as is
      (commonHeight, commonHashHex)
    } else {
      // Orphan mainchain blocks present
      // Check if there is more recent common block, that was not a part of locatorHashes
      val commonHashLocatorIndex: Int = locatorHashes.indexOf(commonHashHex)
      val firstOrphanedMainchainHeaderHash: MainchainHeaderHash = byteArrayToMainchainHeaderHash(BytesUtils.fromHexString(locatorHashes(commonHashLocatorIndex - 1)))
      // Get the list of MainchainHeader Hashes between previously found common point and first orphaned point.
      // Order them from newest to oldest same as bitcoin-style locator.
      val locator: Seq[String] = history.getMainchainHashes(commonHash, firstOrphanedMainchainHeaderHash).map(baw => BytesUtils.toHexString(baw.data)).reverse

      mainchainNodeChannel.getBestCommonPoint(locator) match {
        case Success((height, hash)) => (height, hash)
        case Failure(ex) => throw ex
      }
    }
  }

  def getMainchainBlockReferences(history: SidechainHistory, hashes: Seq[MainchainHeaderHash]): Try[Seq[MainchainBlockReference]] = Try {
    val references = ListBuffer[MainchainBlockReference]()
    for(hash <- hashes) {
      mainchainNodeChannel.getBlockByHash(BytesUtils.toHexString(hash.data)) match {
        case Success(ref) =>
          references.append(ref)
        case Failure(ex) =>
          throw new IllegalStateException(s"Can't retrieve MainchainBlockReference for hash $hash. Connection error.", ex)
      }
    }
    references
  }
}

object MainchainSynchronizer {
  val MAX_BLOCKS_REQUEST: Int = 50
}
