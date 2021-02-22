package com.horizen.cryptolib;

import com.horizen.cryptolibprovider.InMemoryOptimizedMerkleTreeUtils;
import com.horizen.librustsidechains.FieldElement;
import com.horizen.merkletreenative.InMemoryOptimizedMerkleTree;
import com.horizen.utils.BytesUtils;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class InMemoryOptimizedMerkleTreeUtilsTest {

    @Test
    public void emptyMerkleTreeRoot() {
        ArrayList<byte[]> emptyLeaves = new ArrayList<>();
        String emptyFieldHex = BytesUtils.toHexString(new byte[FieldElement.FIELD_ELEMENT_LENGTH]);

        InMemoryOptimizedMerkleTree merkleTree = InMemoryOptimizedMerkleTree.init(0, 1);
        merkleTree.finalizeTreeInPlace();
        FieldElement feRoot = merkleTree.root();
        String rootHex = BytesUtils.toHexString(feRoot.serializeFieldElement());
        assertEquals("Empty tree root hash expected to be zero.", emptyFieldHex, rootHex);

        feRoot.freeFieldElement();
        merkleTree.freeInMemoryOptimizedMerkleTree();


        merkleTree = InMemoryOptimizedMerkleTreeUtils.merkleTree(emptyLeaves);
        feRoot = merkleTree.root();
        rootHex = BytesUtils.toHexString(feRoot.serializeFieldElement());
        assertEquals("Empty tree root hash expected to be zero.", emptyFieldHex, rootHex);

        feRoot.freeFieldElement();
        merkleTree.freeInMemoryOptimizedMerkleTree();


        rootHex = BytesUtils.toHexString(InMemoryOptimizedMerkleTreeUtils.merkleTreeRootHash(emptyLeaves));
        assertEquals("Empty tree root hash expected to be zero.", emptyFieldHex, rootHex);
    }

    @Test
    public void merkleTreeHeight() {
        int leaves = 1;
        for(int height = 0; height < 31; height++) {
            assertEquals(String.format("Tree height is different for %d leaves.", leaves), height, InMemoryOptimizedMerkleTreeUtils.treeHeightForLeaves(leaves));
            if(leaves > 1) {
                // increment leaves number to get the case with log_2(leaves)=H very close to floor(H)
                int nonPowerOfTwoLeaves = leaves + 1;
                assertEquals(String.format("Tree height is different for %d leaves.", nonPowerOfTwoLeaves),
                        height + 1, InMemoryOptimizedMerkleTreeUtils.treeHeightForLeaves(nonPowerOfTwoLeaves));
            }
            leaves = leaves << 1;
        }
    }
}
