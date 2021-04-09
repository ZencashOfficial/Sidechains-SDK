#!/usr/bin/env python2
import json
import time
import math

from SidechainTestFramework.sc_boostrap_info import SCNodeConfiguration, SCCreationInfo, MCConnectionInfo, \
    SCNetworkConfiguration
from SidechainTestFramework.sc_test_framework import SidechainTestFramework
from SidechainTestFramework.sidechainauthproxy import SCAPIException
from test_framework.util import fail, assert_equal, assert_true, start_nodes, \
    websocket_port_by_mc_node_index
from SidechainTestFramework.scutil import bootstrap_sidechain_nodes, \
    start_sc_nodes, generate_next_block, connect_sc_nodes, disconnect_sc_nodes_bi, sync_sc_blocks
from SidechainTestFramework.sc_forging_util import *

"""
Check ceased sidechain reaction:
1. No Certificates for given sidechain for given epoch -> ceasing detection -> chain growing prevention

Configuration:
    Start 1 MC node and 1 SC node (with default websocket configuration).
    SC node is forger, but not a certificate submitter.

Test:
    - generate MC blocks to reach one block before the end of the certificate submission window.
    - generate SC blocks to sync with MC node.
    - generate 1 MC block to reach the end of the certificate submission window.
    - check with MC node that sidechain is ceased from MC perspective.
    - try to generate SC block with 1 MC ref:
        * check that it's not valid
        * SC node can't grow SC chain, because sidechain is ceased.
    - generate 1 more MC block
    - try to generate SC block with 2 MC refs which exceed the submission window end:
        * check that it's not valid
        * SC node can't grow SC chain, because sidechain is ceased.
"""
class SCCeased(SidechainTestFramework):

    number_of_mc_nodes = 1
    number_of_sidechain_nodes = 1

    sc_nodes_bootstrap_info = None
    sc_withdrawal_epoch_length = 10
    sc_creation_amount = 100  # Zen

    def setup_nodes(self):
        return start_nodes(self.number_of_mc_nodes, self.options.tmpdir,
                           extra_args=[['-debug=sc', '-logtimemicros=1']] * self.number_of_mc_nodes)

    def sc_setup_chain(self):
        mc_node = self.nodes[0]
        sc_node_1_configuration = SCNodeConfiguration(
            MCConnectionInfo(address="ws://{0}:{1}".format(mc_node.hostname, websocket_port_by_mc_node_index(0))),
            False  # not a certificate submitter
        )

        network = SCNetworkConfiguration(
            SCCreationInfo(mc_node, self.sc_creation_amount, self.sc_withdrawal_epoch_length),
            sc_node_1_configuration)

        # rewind sc genesis block timestamp for 5 consensus epochs
        self.sc_nodes_bootstrap_info = bootstrap_sidechain_nodes(self.options.tmpdir, network, 720*120*5)

    def sc_setup_nodes(self):
        return start_sc_nodes(self.number_of_sidechain_nodes, self.options.tmpdir)

    def run_test(self):
        mc_node = self.nodes[0]
        sc_node = self.sc_nodes[0]

        # consider mc ref data in the sc genesis block
        mc_blocks_left_for_we = self.sc_withdrawal_epoch_length - 1

        # Generate MC blocks to reach the end of the withdrawal epoch
        mc_block_hashes = mc_node.generate(mc_blocks_left_for_we)
        # Generate SC blocks to sync with MC node
        for x in range(int(math.ceil(len(mc_block_hashes)/3.0))):
            generate_next_block(sc_node, "first node")

        # Generate MC blocks to reach one block before the end of the certificate submission window.
        mc_blocks_left_for_window_end = self.sc_withdrawal_epoch_length / 5
        mc_block_hashes = mc_node.generate(mc_blocks_left_for_window_end - 1)
        mc_blocks_left_for_window_end -= len(mc_block_hashes)
        assert_equal(1, mc_blocks_left_for_window_end, "1 MC block till the end of the withdrawal epoch expected.")
        # Check sidechain status
        sc_info = mc_node.getscinfo(self.sc_nodes_bootstrap_info.sidechain_id)['items'][0]
        assert_equal("ALIVE", sc_info['state'], "Sidechain expected to be alive.")

        # Generate SC blocks to sync with MC node
        for x in range(int(math.ceil(len(mc_block_hashes)/3.0))):
            generate_next_block(sc_node, "first node")

        # Generate 1 MC block to reach the end of the certificate submission window.
        mc_node.generate(1)
        # Check sidechain status
        sc_info = mc_node.getscinfo(self.sc_nodes_bootstrap_info.sidechain_id)['items'][0]
        # TODO: Current MC branch has a bug with "ceased" state definition. Uncomment when MBTR feature is merged.
        # assert_equal("CEASED", sc_info['state'], "Sidechain expected to be ceased.")

        # Try to generate 1 SC block. Node must fail on apply block, because of missed cert in the end of the window.
        # SC block should contain 1 MC block refs, so the block will reach the end of the submission window.
        error_occur = False
        try:
            generate_next_block(sc_node, "first node")
        except SCAPIException as e:
            print("Expected SCAPIException: " + e.error)
            error_occur = True

        assert_true(error_occur,
                    "Node wrongly verified block at the end of the submission window for epoch with no certs.")

        # Generate 1 more MC block.
        mc_node.generate(1)

        # Try to generate 1 SC block. Node must fail on apply block, because of missed cert in the end of the window.
        # SC block should contain 2 MC block refs, so the block will exceed the end of the submission window.
        error_occur = False
        try:
            generate_next_block(sc_node, "first node")
        except SCAPIException as e:
            print("Expected SCAPIException: " + e.error)
            error_occur = True

        assert_true(error_occur,
                    "Node wrongly verified block at the end of the submission window for epoch with no certs.")


if __name__ == "__main__":
    SCCeased().main()
