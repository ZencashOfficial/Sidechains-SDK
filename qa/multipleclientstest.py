#!/usr/bin/env python2
from SidechainTestFramework.sc_test_framework import SidechainComparisonTestFramework
from SidechainTestFramework.sidechainauthproxy import SCAPIException
from util import assert_true, assert_raises

'''SC Node 1 uses an old twinsChain.jar in which there are GET calls. We call one of that method by using POST instead, so we expect an exception'''
class MultipleClientsTest(SidechainComparisonTestFramework):
    def run_test(self):
        i = 0
        for node in self.nodes:
            res = node.getinfo()
            assert_true(res is not None, "MC node {0} not alive !".format(i))
            print("MC node {0} alive !\nResponse to getinfo RPC call: {1} ".format(i, res))
            i = i + 1
        i = 0
        for sc_node in self.sc_nodes:
            if i == 0:
                res = sc_node.debug_info()
                assert_true(res is not None, "SC node {0} not alive !".format(i))
                print("SC node {0} alive !\nResponse to debuginfo API call: {1}".format(i, res))
            if i == 1:
                assert_raises(SCAPIException, sc_node.debug_info, "SC node {0} not alive !".format(i))
                print("SC node {0} is alive and raised an exception as expected".format(i))
            i = i + 1
            
if __name__ == "__main__":
    MultipleClientsTest().main()