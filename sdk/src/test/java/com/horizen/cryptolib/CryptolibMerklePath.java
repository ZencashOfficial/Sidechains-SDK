package com.horizen.cryptolib;

import com.horizen.cryptolibprovider.FieldElementUtils;
import com.horizen.cryptolibprovider.InMemoryOptimizedMerkleTreeUtils;
import com.horizen.librustsidechains.FieldElement;
import com.horizen.merkletreenative.InMemoryOptimizedMerkleTree;
import com.horizen.merkletreenative.MerklePath;
import com.horizen.utils.BytesUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class CryptolibMerklePath {

    @Test
    public void verifyMerklePath() {
        ArrayList<String> merkleTreeLeaves = new ArrayList<>();
        merkleTreeLeaves.add("b82c849c6abcd1ad17f4457333afc45723557348d2dda6974363253223b0f378"); // 0
        merkleTreeLeaves.add("1f4341337dde3bde02ba32919f12b0e73d668f26eaa8ca0e12ab2cff6e29a24e"); // 1
        merkleTreeLeaves.add("974f803832125b4dcc7ee9226e57cd9c39792663fdbd34ce63f2f6d052b3cf15"); // 2
        merkleTreeLeaves.add("2d9b08361ac900cb55c71516c513db868f38536ebac65c6de16e6db26367c970"); // 3
        merkleTreeLeaves.add("95ca7ba2319b55818eab3f20a6cc9b973f597204fb4632c717086a10f97211dc"); // 4
        merkleTreeLeaves.add("198dd7904fc9834464229bf4032b9487da096bb91e9c7ab0b40274fdd767015c"); // 5

        int merkleTreeHeight = InMemoryOptimizedMerkleTreeUtils.treeHeightForLeaves(merkleTreeLeaves.size());
        int leavesNumber = merkleTreeLeaves.size();

        List<byte[]> merkleTreeBytesLeaves = merkleTreeLeaves.stream().map(hex -> BytesUtils.fromHexString(hex)).collect(Collectors.toList());

        InMemoryOptimizedMerkleTree merkleTree = InMemoryOptimizedMerkleTreeUtils.merkleTree(merkleTreeBytesLeaves);
        merkleTree.finalizeTreeInPlace();

        FieldElement root = merkleTree.root();


        // Test: verify merkle path validity, isLeftmost, isRigtmost and leafIndex
        for(int idx = 0; idx < leavesNumber; idx++) {
            FieldElement leaf = FieldElementUtils.messageToFieldElement(merkleTreeBytesLeaves.get(idx));
            MerklePath merklePath = merkleTree.getMerklePath(idx);

            assertTrue(String.format("Merkle path with tree height expected to be valid for leaf %d", idx), merklePath.verify(merkleTreeHeight, leaf, root));
            assertTrue(String.format("Merkle path expected to be valid for leaf %d", idx), merklePath.verify(leaf, root));

            assertEquals("Different leaf index expected.", idx, merklePath.leafIndex());

            assertEquals("Is leftmost result is wrong.", idx == 0, merklePath.isLeftmost());
            assertEquals(String.format("Is rightmost result is wrong for leaf index %d of %d", idx, leavesNumber - 1),
                    idx == leavesNumber - 1, merklePath.isNonEmptyRightmost());

            leaf.freeFieldElement();
            merklePath.freeMerklePath();
        }

        root.freeFieldElement();
        merkleTree.freeInMemoryOptimizedMerkleTree();
    }
}
