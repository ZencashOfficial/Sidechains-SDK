package com.horizen.forge

import com.horizen.block.SidechainBlock

sealed trait ForgeResult

case class ForgeSuccess(block: SidechainBlock) extends ForgeResult {
  override def toString: String = s"Successfully generated block ${block.id}"
}

sealed trait ForgeFailure extends ForgeResult


case class SkipSlot(reason: String = "") extends ForgeFailure {
  override def toString: String = s"Skipped slot for forging" + (if(reason.nonEmpty) s" with reason: $reason" else "")
}

case object NoOwnedForgingStake extends ForgeFailure {
  override def toString: String = s"Can't forge block, no forging stake is present for epoch."
}

case class ForgeFailed(ex: Throwable) extends ForgeFailure {
  override def toString: String = s"Failed block generation due $ex"
}
