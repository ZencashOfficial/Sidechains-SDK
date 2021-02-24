import json
#execute a transaction/sendCoinsToAddress call
def sendCoinsToAddress(sidechainNode, address, amount, fee):
      j = {\
            "outputs": [ \
              { \
                "publicKey": address, \
                "value": amount \
              } \
            ], \
            "fee": fee \
      }
      request = json.dumps(j)
      response = sidechainNode.transaction_sendCoinsToAddress(request)
      return response["result"]["transactionId"]


