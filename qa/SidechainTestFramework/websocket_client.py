import websocket
from websocket import create_connection
from test_framework.util import assert_equal, assert_true
import time

class WebsocketClient():
    # Websocket message types codes
    EVENT_MSG_TYPE = 0
    REQUEST_MSG_TYPE = 1
    RESPONSE_MSG_TYPE = 2
    ERROR_MSG_TYPE = 3

    # Websocket request codes
    SEND_RAW_MEMPOOL_REQUEST = 5
    GET_MEMPOOL_TXS_REQUEST = 4
    GET_NEW_BLOCK_HASHES_REQUEST = 2
    GET_SINGLE_BLOCK_REQUEST = 0

    # Websocket events codes
    MEMPOOL_CHANGED_EVENT = 2
    UPDATE_TIP_EVENT = 0

    def create_connection(self, url):
        return create_connection(url)

    def sendMessage(self, ws, msgType, requestId, requestType, requestPayload):
        ws.send('{"msgType":'+str(msgType)+', "requestId":'+str(requestId)+', "requestType":'+str(requestType)+', "requestPayload":'+requestPayload+' }')
        timeout = 500
        for x in xrange(1, timeout):
            results = ws.recv()
            if len(results)==0:
                time.sleep(1)
            else:
                break
        return results

    def checkMessageStaticFields(self, response, msgType, requestId, answerType):
        assert_equal(response['msgType'], msgType)
        assert_equal(response['answerType'], answerType)
        if (msgType == 0):
            assert_true('requestId' not in response)
        else:
            assert_equal(response['requestId'], requestId)
