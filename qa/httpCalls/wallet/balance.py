#executes a wallet/balance call
def http_wallet_balance(sidechainNode):
      response = sidechainNode.wallet_balance()
      return response['result']['balance']



