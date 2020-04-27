package com.horizen.wallet;

import com.horizen.OpenedWalletBox;
import com.horizen.WalletBox;
import com.horizen.box.Box;
import com.horizen.proposition.Proposition;
import com.horizen.transaction.BoxTransaction;

import java.util.List;

public interface WalletDataAPI {

    void onStartup(SidechainWalletReader walletReader);
    void update(SidechainWalletReader walletReader, byte[] version, List<WalletBox> newBoxes,
                List<OpenedWalletBox> openedBoxes,
                List<BoxTransaction<Proposition, Box<Proposition>>> transactions);
    void rollback(SidechainWalletReader walletReader, byte[] version);
}
