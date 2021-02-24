import json

#executes a  transaction/findById call
def http_transaction_findById(sidechainNode, txid):
      j = {\
            "transactionId": txid, \
            "format": True \
      }
      request = json.dumps(j)
      response = sidechainNode.transaction_findById(request)
      return response["result"]



