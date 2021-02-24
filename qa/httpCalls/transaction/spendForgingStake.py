import json
#execute a transaction/spendForgingStake call
def spendForgingStake(sidechainNode, inputBoxId, valueRegular, valueForging, publicKey, blockSignPublicKey, vrfPubKey):
      j = {\
          "transactionInputs": [ \
            { \
              "boxId": inputBoxId \
            } \
          ],\
          "regularOutputs": [\
            {\
              "publicKey": publicKey,\
              "value": valueRegular\
            }\
          ],\
          "forgerOutputs": [\
            {\
              "publicKey": publicKey,\
              "blockSignPublicKey": blockSignPublicKey,\
              "vrfPubKey": vrfPubKey,\
              "value": valueForging\
            }\
          ],\
      }
      request = json.dumps(j)
      sidechainNode.transaction_spendForgingStake(request)
