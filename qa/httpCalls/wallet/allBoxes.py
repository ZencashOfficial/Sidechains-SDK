#executes a  wallet/allBoxes call
def http_wallet_allBoxes(sidechainNode):
      response = sidechainNode.wallet_allBoxes()
      return response['result']['boxes']



