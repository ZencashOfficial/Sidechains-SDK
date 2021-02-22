package com.horizen.transaction.mainchain;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.horizen.block.MainchainTxSidechainCreationCrosschainOutput;
import com.horizen.block.MainchainTxSidechainCreationCrosschainOutputData;
import com.horizen.box.ForgerBox;
import com.horizen.box.data.ForgerBoxData;
import com.horizen.cryptolibprovider.FieldElementUtils;
import com.horizen.librustsidechains.FieldElement;
import com.horizen.proposition.PublicKey25519Proposition;
import com.horizen.utils.BytesUtils;
import com.horizen.utils.Utils;
import com.horizen.proposition.VrfPublicKey;
import scorex.crypto.hash.Blake2b256;

import java.util.Arrays;

public final class SidechainCreation implements SidechainRelatedMainchainOutput<ForgerBox> {

    private MainchainTxSidechainCreationCrosschainOutput output;
    private byte[] containingTxHash;
    private int index;

    public SidechainCreation(MainchainTxSidechainCreationCrosschainOutput output, byte[] containingTxHash, int index) {
        this.output = output;
        this.containingTxHash = containingTxHash;
        this.index = index;
    }

    // TODO: this can be removed later, so `fieldElementBytes` is used.
    @Override
    public byte[] hash() {
        return BytesUtils.reverseBytes(Utils.doubleSHA256Hash(Bytes.concat(
                BytesUtils.reverseBytes(output.hash()),
                BytesUtils.reverseBytes(containingTxHash),
                BytesUtils.reverseBytes(Ints.toByteArray(index))
        )));
    }

    @Override
    public byte[] fieldElementBytes() {
        byte[] fieldElement = new byte[FieldElement.FIELD_ELEMENT_LENGTH];
        byte[] data = Bytes.concat(
                BytesUtils.reverseBytes(output.hash()),
                BytesUtils.reverseBytes(containingTxHash),
                BytesUtils.reverseBytes(Ints.toByteArray(index))
        );
        // Copy data and leave unused bytes equal to 0.
        System.arraycopy(data, 0, fieldElement, 0, data.length);
        return fieldElement;
    }

    @Override
    public ForgerBox getBox() {
        PublicKey25519Proposition proposition = new PublicKey25519Proposition(output.address());
        long value = output.amount();
        VrfPublicKey vrfPublicKey = new VrfPublicKey(output.customData());

        ForgerBoxData forgerBoxData = new ForgerBoxData(proposition, value, proposition, vrfPublicKey);

        byte[] hash = Blake2b256.hash(Bytes.concat(containingTxHash, Ints.toByteArray(index)));
        long nonce = BytesUtils.getLong(hash, 0);

        return forgerBoxData.getBox(nonce);
    }

    @Override
    public byte[] bytes() {
        return Bytes.concat(
                Ints.toByteArray(output.size()),
                output.sidechainCreationOutputBytes(),
                containingTxHash,
                Ints.toByteArray(index)
        );
    }

    public byte[] getGenSysConstant() {
        return output.constant();
    }

    public static SidechainCreation parseBytes(byte[] bytes) {

        int offset = 0;

        int sidechainCreationSize = BytesUtils.getInt(bytes, offset);
        offset += 4;

        if(bytes.length < 40 + sidechainCreationSize)
            throw new IllegalArgumentException("Input data corrupted.");

        MainchainTxSidechainCreationCrosschainOutputData output = MainchainTxSidechainCreationCrosschainOutputData.create(bytes, offset).get();
        offset += sidechainCreationSize;

        byte[] txHash = Arrays.copyOfRange(bytes, offset, offset + 32);
        offset += 32;

        int index = BytesUtils.getInt(bytes, offset);

        byte[] sidechainId = MainchainTxSidechainCreationCrosschainOutput.calculateSidechainId(txHash, index);
        return new SidechainCreation(new MainchainTxSidechainCreationCrosschainOutput(sidechainId, output), txHash, index);
    }

    @Override
    public SidechainRelatedMainchainOutputSerializer serializer() {
        return SidechainCreationSerializer.getSerializer();
    }

    public int withdrawalEpochLength() {
        return output.withdrawalEpochLength();
    }
}
