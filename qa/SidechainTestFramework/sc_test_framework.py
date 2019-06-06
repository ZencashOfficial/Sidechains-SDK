#!/usr/bin/env python2
from test_framework import BitcoinTestFramework
from authproxy import JSONRPCException
from SidechainTestFramework.sidechainauthproxy import SCAPIException
from util import check_json_precision, \
    initialize_chain, initialize_chain_clean, \
    start_nodes, stop_nodes, \
    sync_blocks, sync_mempools, wait_bitcoinds
from SidechainTestFramework.scutil import initialize_sc_chain, initialize_sc_chain_clean, \
    start_sc_nodes, stop_sc_nodes, \
    sync_sc_blocks, sync_sc_mempools, wait_sidechainclients, generateGenesisData, TimeoutException
import tempfile
import os
import json
import traceback
import sys
import shutil

'''
If you want to keep default behavior:
For MC Test Only: Override sc_setup_chain, sc_setup_network, sc_add_options
For SC Test Only: Override setup_chain, setup_network, add_options and sc_generate_genesis_data
For MC&SC Tests: Don't override anything
'''

#Default config, for the moment, just setup 1 MC node and 1 SC node
class SidechainTestFramework(BitcoinTestFramework):
    
    def add_options(self, parser):
        pass
    
    def setup_chain(self):
        initialize_chain_clean(self.options.tmpdir, 1)
        
    def setup_network(self, split = False):
        self.nodes = self.setup_nodes() 
    
    def setup_nodes(self):
        return start_nodes(1, self.options.tmpdir)
    
    def split_network(self):
        pass

    def sync_all(self):
        sync_blocks(self.nodes, p = True)
        sync_mempools(self.nodes)

    def join_network(self):
        pass
    
    def sc_add_options(self, parser):
        pass
    
    def sc_generate_genesis_data(self):
        return generateGenesisData(self.nodes[0]) #Maybe other parameters in future
    
    def sc_setup_chain(self):
        genesisData = self.sc_generate_genesis_data() 
        initialize_sc_chain_clean(self.options.tmpdir, 1, genesisData)
    
    def sc_setup_network(self, split = False):
        self.sc_nodes = self.sc_setup_nodes()
    
    def sc_setup_nodes(self):
        return start_sc_nodes(1, self.options.tmpdir)
        
    def sc_split_network(self):
        pass

    def sc_sync_all(self):
        sync_sc_blocks(self.sc_nodes, p = True)
        sync_sc_mempools(self.sc_nodes)

    def sc_join_network(self):
        pass
        
    def run_test(self):
        pass
        
    def main(self):
        import optparse

        parser = optparse.OptionParser(usage="%prog [options]")
        parser.add_option("--nocleanup", dest="nocleanup", default=False, action="store_true",
                          help="Leave bitcoinds and test.* datadir on exit or error")
        parser.add_option("--noshutdown", dest="noshutdown", default=False, action="store_true",
                          help="Don't stop bitcoinds after the test execution")
        parser.add_option("--zendir", dest="zendir", default="ZenCore/src",
                          help="Source directory containing zend/zen-cli (default: %default)")
        parser.add_option("--scjarpath", dest="scjarpath", default="resources/twinsChain.jar examples.hybrid.HybridApp", #New option
                          help="Directory containing .jar file for SC (default: %default)")
        parser.add_option("--tmpdir", dest="tmpdir", default=tempfile.mkdtemp(prefix="test"),
                          help="Root directory for datadirs")
        parser.add_option("--tracerpc", dest="trace_rpc", default=False, action="store_true",
                          help="Print out all RPC calls as they are made")
       
        self.add_options(parser)
        self.sc_add_options(parser)
        (self.options, self.args) = parser.parse_args()

        if self.options.trace_rpc:
            import logging
            logging.basicConfig(level=logging.DEBUG)

        os.environ['PATH'] = self.options.zendir+":"+os.environ['PATH']
        
        check_json_precision()

        success = False
        try:
            if not os.path.isdir(self.options.tmpdir):
                os.makedirs(self.options.tmpdir)
            
            print("Initializing test directory "+self.options.tmpdir)
            
            genesisData = self.setup_chain()

            self.setup_network()
            
            self.sc_setup_chain()
            
            self.sc_setup_network()

            self.run_test()

            success = True

        except JSONRPCException as e:
            print("JSONRPC error: "+e.error['message'])
            traceback.print_tb(sys.exc_info()[2])
        except SCAPIException as e:
            print("SCAPI error: "+e.error)
            traceback.print_tb(sys.exc_info()[2])
        except TimeoutException as e:
            print("Timeout while: " + e.operation)
            traceback.print_tb(sys.exc_info()[2])
        except AssertionError as e:
            print("Assertion failed: "+e.message)
            traceback.print_tb(sys.exc_info()[2])
        except Exception as e:
            print("Unexpected exception caught during testing: "+str(e))
            traceback.print_tb(sys.exc_info()[2])
        
        if not self.options.noshutdown:
            if hasattr(self, "nodes"):
                print("Stopping MC nodes")
                stop_nodes(self.nodes)
                wait_bitcoinds()
            if hasattr(self,"sc_nodes"):
                print("Stopping SC nodes")
                stop_sc_nodes(self.sc_nodes)
        else:
            print("Note: client processes were not stopped and may still be running")

        if not self.options.nocleanup and not self.options.noshutdown:
            print("Cleaning up")
            shutil.rmtree(self.options.tmpdir)

        if success:
            print("Test successful")
            sys.exit(0)
        else:
            print("Failed")
            sys.exit(1)

#Build simple test to see if it works
'''Support for running MC & SC Nodes with different binaries. 
For MC the implementation follows the one of BTF, for SC it is possible to specify multiple jars'''
class SidechainComparisonTestFramework(SidechainTestFramework):
    
    def add_options(self, parser):
        parser.add_option("--testbinary", dest="testbinary",
                          default=os.getenv("BITCOIND", "zend"),
                          help="zend binary to test")
        parser.add_option("--refbinary", dest="refbinary",
                          default=os.getenv("BITCOIND", "zend"),
                          help="zend binary to use for reference nodes (if any)")

    def setup_chain(self):
        self.num_nodes = 2
        initialize_chain_clean(self.options.tmpdir, self.num_nodes)

    def setup_network(self):
        self.nodes = start_nodes(self.num_nodes, self.options.tmpdir,
                                    extra_args=[['-debug', '-whitelist=127.0.0.1']] * self.num_nodes,
                                    binary=[self.options.testbinary] +
                                           [self.options.refbinary]*(self.num_nodes-1))
    
    def sc_add_options(self, parser):
        parser.add_option("--jarspathlist", dest="jarspathlist", type = "string", 
                          action = "callback", callback = self._get_comma_separated_args,
                          default=["resources/twinsChain.jar examples.hybrid.HybridApp", "resources/twinsChainOld.jar examples.hybrid.HybridApp"],
                          help="node jars to test, separated by comma")
    
    def _get_comma_separated_args(self, option, opt, value, parser):
        setattr(parser.values, option.dest, value.split(','))
        
    def sc_setup_chain(self):
        genesisData = self.sc_generate_genesis_data()
        self.num_sc_nodes = len(self.options.jarspathlist)
        initialize_sc_chain_clean(self.options.tmpdir, self.num_sc_nodes, genesisData)
        
    def sc_setup_network(self):
        self.sc_nodes = start_sc_nodes(self.num_sc_nodes, self.options.tmpdir, binary = self.options.jarspathlist)    