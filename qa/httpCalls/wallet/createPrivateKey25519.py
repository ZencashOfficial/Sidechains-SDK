import json
#create and send a custom transaction block/best
def http_wallet_createPrivateKey25519(sidechainNode):
      response = sidechainNode.wallet_createPrivateKey25519()
      return response['result']['proposition']['publicKey']



