package com.horizen.wallet;

import com.horizen.OpenedWalletBox;
import com.horizen.WalletBox;
import com.horizen.box.Box;
import com.horizen.proposition.Proposition;
import com.horizen.transaction.BoxTransaction;

import java.util.List;
import java.util.Optional;

public interface SidechainWalletReader {

    Optional<WalletBox> getWalletBox(byte[] boxId);
    List<WalletBox> getAllWalletBoxes();

    Optional<OpenedWalletBox> getOpenedWalletBox(byte[] boxId);
    List<OpenedWalletBox> getAllOpenedWalletBoxes();

    Optional<WalletBox> getClosedWalletBox(byte[] boxId);
    List<WalletBox> getAllClosedWalletBoxes();

    Optional<BoxTransaction<Proposition,Box<Proposition>>> getTransaction(byte[] transactionId);
    List<BoxTransaction<Proposition,Box<Proposition>>> getAllTransaction();
}
