package com.horizen.forge;


import com.horizen.chain.SidechainBlockInfo;
import com.horizen.proposition.PublicKey25519Proposition;
import com.horizen.transaction.SidechainTransaction;

import java.util.List;

public interface ApplicationForger {

    /**
     * This method is called by the forger every time he tries to build a block.
     *
     * @param parentBlockInfo info about the previous block
     * @param blockSignPublicKey public key of the block forger
     * @return list of newly created SidechainTransactions to be added to the next block forged.
     * Return an empty list if no specific transactions must me added.
     */
    List<SidechainTransaction> onForge(SidechainBlockInfo parentBlockInfo, PublicKey25519Proposition blockSignPublicKey);

}
