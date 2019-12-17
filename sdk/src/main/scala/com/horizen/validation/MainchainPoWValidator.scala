package com.horizen.validation

import com.horizen.SidechainHistory
import com.horizen.block.{ProofOfWorkVerifier, SidechainBlock}
import com.horizen.params.NetworkParams
import com.horizen.utils.BytesUtils
import scorex.util.idToBytes

import scala.util.{Failure, Success, Try}

class MainchainPoWValidator(params: NetworkParams) extends HistoryBlockValidator {
  override def validate(block: SidechainBlock, history: SidechainHistory): Try[Unit] = {
    if(ProofOfWorkVerifier.checkNextWorkRequired(block, history.sidechainBlocks, params)) {
      Success()
    }
    else {
      Failure(new IllegalArgumentException("Containing MC Blocks PoW difficulty is invalid for block %s".format(BytesUtils.toHexString(idToBytes(block.id)))))
    }
  }
}
