#!/usr/bin/env python2
import sys

from mc_sc_forging_delegation import MCSCForgingDelegation
from test_framework.util import assert_equal
from mc_node_alive import MCNodeAlive
from mc_sc_connected_nodes import MCSCConnectedNodes
from mc_sc_forging1 import MCSCForging1
from mc_sc_forging2 import MCSCForging2
from mc_sc_forging3 import MCSCForging3
from mc_sc_nodes_alive import MCSCNodesAlive
from sc_backward_transfer import SCBackwardTransfer
from sc_bootstrap import SCBootstrap
from sc_forward_transfer import SCForwardTransfer
from websocket_server import SCWsServer

def run_test(test):
    try:
        print("----> Running test - " + str(test.__class__))
        test.main()
    except SystemExit as e:
        return e.code
    return 0

def run_tests(log_file):
    sys.stdout = log_file
    result = run_test(MCNodeAlive())
    assert_equal(0, result, "mc_node_alive test failed!")

    result = run_test(MCSCConnectedNodes())
    assert_equal(0, result, "mc_node_alive test failed!")

    result = run_test(MCSCForging1())
    assert_equal(0, result, "mc_sc_forging1 test failed!")

    result = run_test(MCSCForging2())
    assert_equal(0, result, "mc_sc_forging2 test failed!")

    result = run_test(MCSCForging3())
    assert_equal(0, result, "mc_sc_forging3 test failed!")

    result = run_test(MCSCNodesAlive())
    assert_equal(0, result, "mc_sc_nodes_alive test failed!")

    result = run_test(SCBackwardTransfer())
    assert_equal(0, result, "sc_backward_transfer test failed!")

    result = run_test(SCBootstrap())
    assert_equal(0, result, "sc_bootstrap test failed!")

    result = run_test(SCForwardTransfer())
    assert_equal(0, result, "sc_forward_transfer test failed!")

    result = run_test(MCSCForgingDelegation())
    assert_equal(0, result, "mc_sc_forging_delegation test failed!")

    result = run_test(SCWsServer())
    assert_equal(0, result, "websocket_server test failed!")

if __name__ == "__main__":
    log_file = open("sc_test.log", "w")
    run_tests(log_file)
