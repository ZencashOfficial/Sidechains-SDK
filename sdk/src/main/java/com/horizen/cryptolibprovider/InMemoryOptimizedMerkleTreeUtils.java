package com.horizen.cryptolibprovider;

import com.google.common.math.BigIntegerMath;
import com.horizen.librustsidechains.FieldElement;
import com.horizen.merkletreenative.InMemoryOptimizedMerkleTree;
import com.horizen.merkletreenative.MerklePath;
import com.horizen.utils.BytesUtils;
import com.horizen.utils.Utils;

import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

public class InMemoryOptimizedMerkleTreeUtils {

    public static InMemoryOptimizedMerkleTree merkleTree(List<byte[]> leaves) {
        int numberOfLeaves = leaves.size();
        if(numberOfLeaves == 0)
            numberOfLeaves = 1;

        int height = treeHeightForLeaves(numberOfLeaves);
        InMemoryOptimizedMerkleTree merkleTree = InMemoryOptimizedMerkleTree.init(height, numberOfLeaves);

        for(byte[] leaf : leaves) {
            if(leaf == null)
                continue;
            FieldElement feLeaf = FieldElementUtils.messageToFieldElement(leaf);
            merkleTree.append(feLeaf);
            feLeaf.freeFieldElement();
        }
        merkleTree.finalizeTreeInPlace();

        return merkleTree;
    }

    public static byte[] merkleTreeRootHash(List<byte[]> leaves) {
        InMemoryOptimizedMerkleTree merkleTree = merkleTree(leaves);

        FieldElement feRoot = merkleTree.root();
        byte[] root = feRoot.serializeFieldElement();

        feRoot.freeFieldElement();
        merkleTree.freeInMemoryOptimizedMerkleTree();

        return root;
    }

    public static int treeHeightForLeaves(int numberOfLeaves) {
        if (numberOfLeaves <= 0)
            throw new IllegalArgumentException(String.format("Unable to calculate tree height for '%d' leaves." +
                    "Valued expected to be positive.", numberOfLeaves));

        // Using BigInteger to avoid floating point precision issues.
        return BigIntegerMath.log2(BigInteger.valueOf(numberOfLeaves), RoundingMode.CEILING);
    }

    // Note: currently expectedRoot is not a FieldElement, but a double SHA256 hash of the element
    // expectedRoot is in big-endian notation.
    public static boolean verifyMerklePath(MerklePath merklePath, byte[] leaf, byte[] expectedRoot) {
        FieldElement feLeaf = FieldElementUtils.messageToFieldElement(leaf);
        FieldElement feRoot = merklePath.apply(feLeaf);

        byte[] feRootBytes = feRoot.serializeFieldElement();

        feLeaf.freeFieldElement();
        feRoot.freeFieldElement();

        // TODO: change later, when the MC header will contain exactly a Field Element root hash
        // actual root is a double SHA256 in big-endian notation.
        byte[] actualRoot = BytesUtils.reverseBytes(Utils.doubleSHA256Hash(feRootBytes));
        return java.util.Arrays.equals(actualRoot, expectedRoot);
    }
}
