import json

#executes a  block/findById call
def http_block_findById(sidechainNode, blockId):
      j = {\
            "blockId": blockId
      }
      request = json.dumps(j)
      response = sidechainNode.block_findById(request)
      return response["result"]



