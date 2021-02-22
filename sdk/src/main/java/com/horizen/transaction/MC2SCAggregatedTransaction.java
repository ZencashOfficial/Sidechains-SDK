package com.horizen.transaction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.horizen.box.Box;
import com.horizen.box.BoxUnlocker;
import com.horizen.proposition.Proposition;
import com.horizen.serialization.Views;
import com.horizen.transaction.mainchain.ForwardTransferSerializer;
import com.horizen.transaction.mainchain.SidechainCreationSerializer;
import com.horizen.transaction.mainchain.SidechainRelatedMainchainOutput;
import com.horizen.utils.*;
import scorex.core.serialization.ScorexSerializer;
import scorex.util.encode.Base16;

import java.util.*;

import static com.horizen.transaction.CoreTransactionsIdsEnum.MC2SCAggregatedTransactionId;

@JsonView(Views.Default.class)
@JsonIgnoreProperties({"mc2scTransactionsOutputs", "encoder"})
public final class MC2SCAggregatedTransaction
        extends BoxTransaction<Proposition, Box<Proposition>> {

    private byte[] mc2scTransactionsMerkleRootHash;
    private List<SidechainRelatedMainchainOutput> mc2scTransactionsOutputs;
    private long timestamp;

    private List<Box<Proposition>> newBoxes;

    // Serializers definition
    private static ListSerializer<SidechainRelatedMainchainOutput> mc2scTransactionsSerializer = new ListSerializer<>(
            new DynamicTypedSerializer<>(
                new HashMap<Byte, ScorexSerializer<SidechainRelatedMainchainOutput>>() {{
                    put((byte)1, (ScorexSerializer)ForwardTransferSerializer.getSerializer());
                    put((byte)3, (ScorexSerializer)SidechainCreationSerializer.getSerializer());
                }}, new HashMap<>()
            ));

    public MC2SCAggregatedTransaction(List<SidechainRelatedMainchainOutput> mc2scTransactionsOutputs, long timestamp) {
        if(mc2scTransactionsOutputs.isEmpty())
            throw new IllegalArgumentException("Empty sidechain related mainchain outputs passed.");
        if(timestamp < 0)
            throw new IllegalArgumentException("Negative timestamp passed.");
        this.mc2scTransactionsOutputs = mc2scTransactionsOutputs;
        this.timestamp = timestamp;
    }

    @Override
    public TransactionSerializer serializer() {
        return MC2SCAggregatedTransactionSerializer.getSerializer();
    }

    // no checker exists for current transaction type
    // keep check in mempool against this
    @Override
    public TransactionIncompatibilityChecker incompatibilityChecker() {
        return null;
    }

    @Override
    public synchronized List<BoxUnlocker<Proposition>> unlockers() {
        return new ArrayList<>();
    }

    @Override
    public synchronized List<Box<Proposition>> newBoxes() {
        if (newBoxes == null) {
            newBoxes = new ArrayList<>();
            for(SidechainRelatedMainchainOutput t : mc2scTransactionsOutputs) {
                newBoxes.add((Box<Proposition>) t.getBox());
            }
        }
        return Collections.unmodifiableList(newBoxes);
    }

    @Override
    public long fee() {
        return 0;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public byte transactionTypeId() {
        return MC2SCAggregatedTransactionId.id();
    }

    @Override
    public String id() {
        return Base16.encode(mc2scMerkleRootHash());
    }

    @Override
    public byte[] messageToSign() {
        throw new UnsupportedOperationException("MC2SCAggregatedTransaction can not be signed.");
    }

    // TODO: instead of using com.horizen.utils.MerkleTree we should use sc-cryptolib MerkleTree and `output.fieldElementBytes` method
    @JsonProperty("mc2scTransactionsMerkleRootHash")
    public synchronized byte[] mc2scMerkleRootHash() {
        if(mc2scTransactionsMerkleRootHash == null) {
            ArrayList<byte[]> hashes = new ArrayList<>();
            for (SidechainRelatedMainchainOutput t : mc2scTransactionsOutputs)
                hashes.add(t.hash());
            mc2scTransactionsMerkleRootHash = MerkleTree.createMerkleTree(hashes).rootHash();
        }
        return Arrays.copyOf(mc2scTransactionsMerkleRootHash, mc2scTransactionsMerkleRootHash.length);
    }

    public List<SidechainRelatedMainchainOutput> mc2scTransactionsOutputs() {
        return Collections.unmodifiableList(mc2scTransactionsOutputs);
    }

    public boolean semanticValidity() {
        return true;
    }


    @Override
    public byte[] bytes() {
        byte[] transactions = mc2scTransactionsSerializer.toBytes(mc2scTransactionsOutputs);
        return Bytes.concat(                                        // minimum MC2SCAggregatedTransaction length is 12 bytes
                Longs.toByteArray(timestamp()),                     // 8 bytes
                Ints.toByteArray(transactions.length),              // 4 bytes
                transactions                                        // depends on previous value (>=4 bytes)
        );
    }

    public static MC2SCAggregatedTransaction parseBytes(byte[] bytes) {
        if (bytes.length < 12)
            throw new IllegalArgumentException("Input data corrupted.");

        if (bytes.length > MAX_TRANSACTION_SIZE)
            throw new IllegalArgumentException("Input data length is too large.");

        int offset = 0;

        long timestamp = BytesUtils.getLong(bytes, offset);
        offset += 8;

        int batchSize = BytesUtils.getInt(bytes, offset);
        offset += 4;
        List<SidechainRelatedMainchainOutput> mc2scTransactions = mc2scTransactionsSerializer.parseBytes(Arrays.copyOfRange(bytes, offset, offset + batchSize));

        return new MC2SCAggregatedTransaction(mc2scTransactions, timestamp);
    }
}
