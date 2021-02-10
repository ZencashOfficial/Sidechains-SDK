<!-- wp:paragraph -->
<p>Dans le troisième épisode de ce dossier de présentation des sidechains Horizen, je vais m'intéresser aux transferts de la mainchain Horizen à la sidechain (Forward transfer), au sein de la sidechain et enfin, de la sidechain à la mainchain Horizen (Backward transfer).</p>
<!-- /wp:paragraph -->

<!-- wp:heading -->
<h2>Le plan du dossier : Les Sidechains Horizen</h2>
<!-- /wp:heading -->

<!-- wp:list {"ordered":true} -->
<ol><li><a href="https://mescryptos.fr/les-sidechains-horizen-le-noeud-zend-oo/">Zend_oo, le node de la MainChain Horizen</a></li><li><a href="https://mescryptos.fr/les-sidechains-horizen-ma-premiere-sidechain/">Création de ma première sidechain</a></li><li>Sidechains : Transférer du ZEN : Tu es ici :)</li><li>Bonus : <a href="https://mescryptos.fr/gagner-des-zen-ca-detend/">Gagner des ZEN : Le Faucet en détail</a></li><li>Bonus : <a href="https://mescryptos.fr/ton-node-zen-en-20-minutes-chrono/">Monter un secure node ZEN en 20 minutes</a></li></ol>
<!-- /wp:list -->

<!-- wp:heading -->
<h2>Forward transfer : de la mainchain à la sidechain</h2>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>Je m'assure que ma mainchain est bien lancée (zend), ainsi que la sidechain (SimpleApp).</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>De combien je dispose sur la mainchain ? J'utilise l'appel <code>getbalance</code> via zen-cli pour interroger zendoo.</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ zen-cli -regtest getbalance
936.49992873</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Je vois que je dispose de 936.49992873ZEN sur la mainchain.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>De combien je dispose sur la sidechain ? J'utilise l'appel <code>/wallet/balance</code> de l'API. Ce dernier ne prend pas de paramètre. Les headers sont certes inutiles ici mais autant les mettre à chaque fois.</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ curl -X POST "http://127.0.0.1:9085/wallet/balance" -H "accept: application/json" -H "Content-Type: application/json"
{
  "result" : {
    "balance" : 40000000000
  }
}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Je vois que je dispose de 400ZEN sur la sidechain. Les montants sur la sidechain sont exprimés en satoshis (ou en zennies si on se réfère au vocabulaire du faucet que je trouve personnellement plus sympa). Donc, la réponse renvoyée est 40000000000 zennies (1ZEN vaut 100millions de zennies).</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Je décide donc de m'envoyer 120ZEN de plus de la mainchain à la sidechain. Ici, je vais utiliser <code>sc_send</code> qui prend en paramètres une adresse de la sidechain (ma clé publique utilisée pour générer la sidechain), un montant en ZEN et enfin l'identifiant <code>scid</code> de ma sidechain (je l'ai généré dans l'article précédent, les valeurs sont bien évidemment à adapter à ton cas).. </p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ zen-cli -regtest sc_send "a03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f8" 120 "d40188abb42d6ba6302464a847ace23e52b0c7edcf433cd17c10ca008b5e8783"
c8ad35d8309dd53e621987f659771624c8ddd9911f06c0de72b068646b75de54</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Je reçois en retour l'identifiant de transaction de la mainchain.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Je vérifie le contenu de cette transaction :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ zen-cli -regtest gettransaction "c8ad35d8309dd53e621987f659771624c8ddd9911f06c0de72b068646b75de54"
{
  "amount": -120.00000000,
  "fee": -0.00001776,
  "confirmations": 0,
  "txid": "c8ad35d8309dd53e621987f659771624c8ddd9911f06c0de72b068646b75de54",
  "walletconflicts": &#91;
  ],
  "time": 1595631280,
  "timereceived": 1595631280,
  "vsc_ccout": &#91;
  ],
  "vft_ccout": &#91;
    {
      "scid": "d40188abb42d6ba6302464a847ace23e52b0c7edcf433cd17c10ca008b5e8783",
      "value": 120.00000000,
      "n": 0,
      "address": "a03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f8"
    }
  ],
  "vjoinsplit": &#91;
  ],
  "details": &#91;
    {
      "sc address": "a03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f8",
      "category": "crosschain",
      "amount": -120.00000000,
      "fee": -0.00001776,
      "size": 1775
    }
  ],
  "hex": "fcffffff0b13872704dd23473066a19c78048b3c37dba09fcb436b8d0df9adf9d958cb6283000000006b483045022100a5a8a842975473def66a0fa192240373a66e33b2e2da3edfe86ba27baa01d87e02201306acde4868503925336738fde3e43c7d14ba2f4cd4598397cc51fa61e9b23b012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff417e5c87ef54b029b559775b736d1057360a066d39dcf46d99e9071da780708e000000006a473044022070a7b6ca8874e9b6ebeb1b73911bdec61a783810ec857bf5f571d0d07fbcad47022052a2e78a58030a0f5a41e52e365b3f7aa627fa69712af4646849e9c8048959dd012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff6b1cd4b29330b2b1810a28d743c119c799b5eb47321628123cddc5b1ad6cce15000000006b4830450221009f595295d0de99523a57759838505948c967a449efe21035111b3f0f22acba5e022015fb91589d07e8af23ac56f9725616eab0478925f8a36b29c4c5339758fcd849012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff82c9d7e6bc52909c285ea49e3ca18130a1607b3dd262835b62b64b917bdac1f800000000694630430220304095af95cff7f50e30e18c180a2c72bd62739f222b87b3c7194cc55683a385021f46158dd07fc0243fefb068b652b55a7d5e95883de90fb035c9b2c763be5e67012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff8d541be5234e9cd65b098e9685e3c77d83bcd6f72393bcc96e4d225bcbf848e9000000006a47304402204d5fca29d3f894d1c29a4faaec9f5faa045e07018504a42c71b7cb6e1d90e554022064812ea834d70ae6584fc11270df1c0ce595a3ccb287e0993f25f0a5c4a45a43012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff90bf63244b198c95bc1e623847d9a9659419a3f105882b30a895c29511d1502e000000006b483045022100d7827d2e55c83fedaf36a46d0bd131c80dae6d90887fbb1248abdde0df05cb110220497f2e2d17e74b876b90fe96023e1b6ab11de679993258a012eb2e08512cfa5d012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffffbaced09b6e4f111663973a169eaf2cb82aea2ce82d7a650db62e15e7ff5fd0b9000000006a47304402205027064038ad3ae82ce83ff0c3f06b99b615b5330fef2c7dfaf596d484478c1802202bfdfee6aff9853e9ec8f23c1ff7be5ef9836e52a3a092436bd0ccc010221bad012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffffefa6bf4ceba0b9ce9627332617a0be8a8951fb27d733943f569a1536288f0d5e000000006b483045022100bbe769e530804738ac7dacb0b15957b3fc088860fab8a36464199e2ba84c8661022004ed2fcc01d429a8c6768a38572beb41fc6f051302a4e7809988268e6ef54d87012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffefffffff213e6aa61669b103d02d655208260f01a4efa0da6cde48d5b92680c98ce0d7c000000006b483045022100dabb5c1c5c0390ea1eb395211bd5c18e2bb498f7554eaa362a6b4b39cd1cbec40220302a421c1cd42ce1eca11f5e6e17a3c6251a5b1f90033ee543762bb341879da8012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffefffffffb9c8f800ac845117ad63b4f1ffa7833fd3b734b06403e7f8cb558c62e1ef478000000006a47304402201986137cd180cc3ea6a50a952f06e8690d72acd6e6d3e1d76a58f9d2a2d65034022045deae88f6e7bb073bd8695cacb5a88860db435e172ee5aec592df692760d9c7012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffefffffffe16573603229c0a7d7c16956fd6cdfce887b3e5393dacde8e0b79c4eb839c98000000006b483045022100d3757a56d32d6653b4508e576228099d56649777520d6d295357da80b6663c4c022035219e5c868e7e5f77fadaebb211467707ad3f9b14aab2b6081548a14117d7cb012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff013058a012000000003c76a9142954edbc7caadad684590bebfe6045ff2b39c8fd88ac20bb1acf2c1fc1228967a611c7db30632098f0c641855180b5fe23793b72eea50d00b40001007841cb02000000f8d6a2f8db1e7f3cbba1f94c96da8dc3873da20b9b46260a063fb01291ee3ba083875e8b00ca107cd13c43cfedc7b0523ee2ac47a8642430a66b2db4ab8801d4d3000000"
}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>On retrouve bien le montant, l'adresse destinataire et l'identifiant de la sidechain. </p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Je valide donc en ajoutant la transaction au prochain bloc de la sidechain que je mine de ce pas :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ zen-cli -regtest generate 1
&#91;
  "027024f228b813d175aec0ea22412a065ff65d197611703a1499a6ec11a903ea"
]
$ zen-cli -regtest getblock "027024f228b813d175aec0ea22412a065ff65d197611703a1499a6ec11a903ea"
{
  "hash": "027024f228b813d175aec0ea22412a065ff65d197611703a1499a6ec11a903ea",
  "confirmations": 1,
  "size": 2140,
  "height": 222,
  "version": 3,
  "merkleroot": "5d4518fb1a4972d513f125657fae52e62ce841e5f0ed3bfbd78d08d16d35f103",
  "scTxsCommitment": "f610e3f1b60e5769534b7a49f3101dd29b7d7389775d64560b617e2cca0b9ffe",
  "tx": &#91;
    "7205cb1b8ae8bedd6298889d1873d23ee76b4e4dc3758deae6fb221057a2594c",
    "c8ad35d8309dd53e621987f659771624c8ddd9911f06c0de72b068646b75de54"
  ],
  "cert": &#91;
  ],
  "time": 1595462378,
  "nonce": "0000f9f1905d2926a6aeec0e2bd234f67c8445e2b6be76c7fc615a6fceea0034",
  "solution": "0e354cac11009db9f8180fded782af2bade325c4a4b9e2a6062b9454459c55675eb6d1de",
  "bits": "200f0f02",
  "difficulty": 1.000013172800801,
  "chainwork": "0000000000000000000000000000000000000000000000000000000000000ecf",
  "anchor": "59d2cde5e65c1414c32ba54f0fe4bdb3d67618125286e6a191317917c812c6d7",
  "valuePools": &#91;
    {
      "id": "sprout",
      "monitored": true,
      "chainValue": 0.00000000,
      "chainValueZat": 0,
      "valueDelta": 0.00000000,
      "valueDeltaZat": 0
    }
  ],
  "previousblockhash": "04852dabaace286308c2ff9a8ba4968d2e3cf17b7079594b8dff756eb0e0f7f6"
}
$ zen-cli -regtest getbalance
825.24991099</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Le bloc est miné, j'en vérifie le contenu et je vérifie ma balance. Elle  bien été amputée de 120ZEN mais j'ai gagné une récompense de minage pour mon bloc de 7.5 auquels s'ajoutent les fees de 0.0001776 qui s'annulent car je les paye puis les reçois en récompense de minage. Bref, je retombe sur mes pattes.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Les ZEN ont bien quitté la mainchain. Je file donc vérifier ma balance sur la sidechain :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ curl -X POST "http://127.0.0.1:9085/wallet/balance" -H "accept: application/json" -H "Content-Type: application/json"
{
  "result" : {
    "balance" : 40000000000
  }
}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Celle-ci n'a pas bougé encore. En toute logique, il faut comme pour la main chain générer un bloc supplémentaire pour que la modification soit prise en compte.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Je vérifie les paramètres de génération des blocs et l'état actuel grâce à l'appel <code>/block/forginginfo</code> de l'API.</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ curl -X POST "http://127.0.0.1:9085/block/forgingInfo" -H "accept: application/json" -H "Content-Type: application/json" 
{
  "result" : {
    "consensusSecondsInSlot" : 120,
    "consensusSlotsInEpoch" : 720,
    "bestEpochNumber" : 1,
    "bestSlotNumber" : 720
  }</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>On peut placer 720 blocs dans un epoch. On en est actuellement au slot 720 de l'epoch 1. Donc, le prochain bloc sera le bloc 1 de l'epoch 2.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Je le génère grâce à l'API, en appelant <code>/block/generate</code> et les paramètres au format JSON <code>{\"epochNumber\":2,\"slotNumber\":1}</code> en protégeant les guillemets.</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ curl -X POST "http://127.0.0.1:9085/block/generate" -H "accept: application/json"  -H "Content-Type: application/json" -d "{\"epochNumber\":2,\"slotNumber\":1}"
{
  "result": {
    "blockId": "c0537b32906ada53582ba2f65c58fd9067d12024d4216a6c11cede30490dfd5f"
  }
}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>J'obtiens en retour l'id du bloc miné que je peux explorer en appelant <code>/block/findById</code> en lui passant le blockId en paramètre (toujours au format JSON).</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ curl -X POST "http://127.0.0.1:9085/block/findById" -H "accept: application/json" -H "Content-Type: application/json" -d "{\"blockId\":\"c0537b32906ada53582ba2f65c58fd9067d12024d4216a6c11cede30490dfd5f\"}"
{
  "result" : {
    "blockHex" : "018bbc8219cab8b9b1f68513f89d3489bf950635a2ee90192237c9da6ca5fb9c8fbca3c1f10ba204e933f41dc86ca654a03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f800000009502f9000a03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f80ed74aa4afda5ffb358ba4906d0a5429f9840805e1fcc9ea71fca62afb5e97d779d0d2d3dd1bd9a3563166eff9d12b9ab9b6f11baf5639606689c72e16e2577ee338229e015697c8d174782b4caa04af9902eca2f3c384c6f558cf685b11010084a8436bbb18f7ccd039c249afa69f868c79939923ee3fc3ec437b998246db5b4907881702641ec6b41fdc6de644087713115fa2548121280fe8ac85fe31387aee6465767d8ba2189fab2ee8bf702beda6af018b4d68f890176bf2ab599e0000000800000000c3acce7d6d69a5434c6d4934ba19bfcaa07e9002638341d51b4cca039c077f7b03c0ea810446c20887d0b2e5cbaff30254b79e8102d1fb5f27471eedc897a53fa220d865d8a0b76a5f1acf582747953523c1fcb10dd44859fb1c422d31b2000064d16b94ddc62142d53e5634afc04633a8ffd5950b2596f8887219a0af4610819b452d4b8af5d9b3efaf65a8b9ce2a89599f3ff96e6383122d22974f7dec78964a72b37136fba57ef3ddc049f7702cf2dda921037bfa1bb8329f3fe87e020100007dc6c4e190e185761195b14e7ffbc94184357adfccfed1bd6279dbd32dc3e969440cbd2feffe87d7e88455e6ce157c32eda6cfde45c5f8901869cf9640a3643890100f5157452a4428b34464d6b3f3bb70cb56cc2822bedf97ac41a26cca0000dfb3656e9aee97158d5a48558edb4b01e8278442f83a5ef28f5d1983ecdffb0a93146dbc3604c79a988c58dfbb0758027007899b919cababcde21cd0c9e6ee366a65b22ba12466039e86d4ca7f84cf0ac61bbb46905e728004466421fd6e00000000000000000000000000000000000000000000000000000000000000000000eed2f0bae7e5055070673932300c4804041ea95ba78b76b0681ff4f7cdb45675000000000000000000000000000000000000000000000000000000000000000000800163cb74562c998def3a503c9490540a54269932e439b8e98ca9353d6d3eaf6ab972a289cdeb47d08296f1b092d6c760fc61fc833980c4501311f36411f5bb16030002cc02027024f228b813d175aec0ea22412a065ff65d197611703a1499a6ec11a903eaf801000000005f18d2ea0000007002da0101007841cb02000000f8d6a2f8db1e7f3cbba1f94c96da8dc3873da20b9b46260a063fb01291ee3ba083875e8b00ca107cd13c43cfedc7b0523ee2ac47a8642430a66b2db4ab8801d4c8ad35d8309dd53e621987f659771624c8ddd9911f06c0de72b068646b75de5400000000080000000000000002e20203000000f6f7e0b06e75ff8d4b5979707bf13c2e8d96a48b9affc2086328ceaaab2d850403f1356dd1088dd7fb3bedf0e541e82ce652ae7f6525f113d572491afb18455dfe9f0bca2c7e610b56645d7789737d9bd21d10f3497a4b5369570eb6f1e310f6ead2185f020f0f203400eace6f5a61fcc776beb6e245847cf634d22b0eecaea626295d90f1f90000240e354cac11009db9f8180fded782af2bade325c4a4b9e2a6062b9454459c55675eb6d1de00",
    "block" : {
      "header" : {
        "version" : 1,
        "parentId" : "8bbc8219cab8b9b1f68513f89d3489bf950635a2ee90192237c9da6ca5fb9c8f",
        "timestamp" : 1595418846,
        "forgerBox" : {
          "nonce" : -1642701030306306476,
          "id" : "7006b227eaa723e4afffb404383fd07166959275ec9c8f7e6665314c85ff1b03",
          "vrfPubKey" : {
            "valid" : true,
            "publicKey" : "0ed74aa4afda5ffb358ba4906d0a5429f9840805e1fcc9ea71fca62afb5e97d779d0d2d3dd1bd9a3563166eff9d12b9ab9b6f11baf5639606689c72e16e2577ee338229e015697c8d174782b4caa04af9902eca2f3c384c6f558cf685b11010084a8436bbb18f7ccd039c249afa69f868c79939923ee3fc3ec437b998246db5b4907881702641ec6b41fdc6de644087713115fa2548121280fe8ac85fe31387aee6465767d8ba2189fab2ee8bf702beda6af018b4d68f890176bf2ab599e000000"
          },
          "typeId" : 3,
          "blockSignProposition" : {
            "publicKey" : "a03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f8"
          },
          "proposition" : {
            "publicKey" : "a03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f8"
          },
          "value" : 40000000000
        },
        "forgerBoxMerklePath" : "00000000",
        "vrfProof" : {
          "vrfProof" : "c3acce7d6d69a5434c6d4934ba19bfcaa07e9002638341d51b4cca039c077f7b03c0ea810446c20887d0b2e5cbaff30254b79e8102d1fb5f27471eedc897a53fa220d865d8a0b76a5f1acf582747953523c1fcb10dd44859fb1c422d31b2000064d16b94ddc62142d53e5634afc04633a8ffd5950b2596f8887219a0af4610819b452d4b8af5d9b3efaf65a8b9ce2a89599f3ff96e6383122d22974f7dec78964a72b37136fba57ef3ddc049f7702cf2dda921037bfa1bb8329f3fe87e020100007dc6c4e190e185761195b14e7ffbc94184357adfccfed1bd6279dbd32dc3e969440cbd2feffe87d7e88455e6ce157c32eda6cfde45c5f8901869cf9640a3643890100f5157452a4428b34464d6b3f3bb70cb56cc2822bedf97ac41a26cca0000dfb3656e9aee97158d5a48558edb4b01e8278442f83a5ef28f5d1983ecdffb0a93146dbc3604c79a988c58dfbb0758027007899b919cababcde21cd0c9e6ee366a65b22ba12466039e86d4ca7f84cf0ac61bbb46905e728004466421fd6e0000"
        },
        "sidechainTransactionsMerkleRootHash" : "0000000000000000000000000000000000000000000000000000000000000000",
        "mainchainMerkleRootHash" : "eed2f0bae7e5055070673932300c4804041ea95ba78b76b0681ff4f7cdb45675",
        "ommersMerkleRootHash" : "0000000000000000000000000000000000000000000000000000000000000000",
        "ommersCumulativeScore" : 0,
        "signature" : {
          "signature" : "63cb74562c998def3a503c9490540a54269932e439b8e98ca9353d6d3eaf6ab972a289cdeb47d08296f1b092d6c760fc61fc833980c4501311f36411f5bb1603",
          "typeId" : 1
        },
        "id" : "c0537b32906ada53582ba2f65c58fd9067d12024d4216a6c11cede30490dfd5f"
      },
      "sidechainTransactions" : &#91; ],
      "mainchainBlockReferencesData" : &#91; {
        "headerHash" : "027024f228b813d175aec0ea22412a065ff65d197611703a1499a6ec11a903ea",
        "sidechainRelatedAggregatedTransaction" : {
          "modifierTypeId" : 2,
          "id" : "09b52d358c46a68cd43013591d39e34ecf606736f28d3a30441e3dd948a862c7",
          "timestamp" : 1595462378,
          "newBoxes" : &#91; {
            "nonce" : 5518227575416507090,
            "id" : "5d7e803f4dbdcb24d0f4ebb1913f6e47b1d9ae00320509d470df7b90206ef764",
            "typeId" : 1,
            "proposition" : {
              "publicKey" : "a03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f8"
            },
            "value" : 12000000000
          } ],
          "mc2scTransactionsMerkleRootHash" : "09b52d358c46a68cd43013591d39e34ecf606736f28d3a30441e3dd948a862c7",
          "fee" : 0,
          "typeId" : 2,
          "unlockers" : &#91; ]
        },
        "mProof" : &#91; ],
        "proofOfNoData" : &#91; null, null ]
      } ],
      "mainchainHeaders" : &#91; {
        "version" : 3,
        "hashPrevBlock" : "04852dabaace286308c2ff9a8ba4968d2e3cf17b7079594b8dff756eb0e0f7f6",
        "hashMerkleRoot" : "5d4518fb1a4972d513f125657fae52e62ce841e5f0ed3bfbd78d08d16d35f103",
        "hashScTxsCommitment" : "f610e3f1b60e5769534b7a49f3101dd29b7d7389775d64560b617e2cca0b9ffe",
        "time" : 1595462378,
        "bits" : 537857794,
        "nonce" : "0000f9f1905d2926a6aeec0e2bd234f67c8445e2b6be76c7fc615a6fceea0034",
        "solution" : "0e354cac11009db9f8180fded782af2bade325c4a4b9e2a6062b9454459c55675eb6d1de",
        "hash" : "027024f228b813d175aec0ea22412a065ff65d197611703a1499a6ec11a903ea"
      } ],
      "ommers" : &#91; ],
      "timestamp" : 1595418846,
      "parentId" : "8bbc8219cab8b9b1f68513f89d3489bf950635a2ee90192237c9da6ca5fb9c8f",
      "id" : "c0537b32906ada53582ba2f65c58fd9067d12024d4216a6c11cede30490dfd5f"
    }
  }
}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>On retrouve bien les 12000000000 à destination de mon adresse.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Restons simples. Est-ce que j'ai bien reçu les ZENs ? Oui ! J'en ai bien 520 à présent.</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ curl -X POST "http://127.0.0.1:9085/wallet/balance" -H "accept: application/json"
{
  "result" : {
    "balance" : 52000000000
  }
}</code></pre>
<!-- /wp:code -->

<!-- wp:heading -->
<h2>Les transferts au sein de la sidechain</h2>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>J'ai fait un transfert de la mainchain à la sidechain. </p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Maintenant, je te montre rapidement comment faire un transfert à l'intérieur de la sidechain. </p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>D'abord, j'ai besoin d'une adresse destinataire. Je pourrais le faire sur un autre node de la sidechain, mais pour faire simple, je vais juste me contenter de créer une nouvelle adresse sur celui-ci. Le processus reste le même.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Pour créer une nouvelle adresse sur la sidechain, j'utilise l'API et son appel <code>/wallet/createPrivateKey25519</code> qui nous retourne une clé publique sur laquelle on peut transférer des tokens :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ curl -X POST "http://127.0.0.1:9085/wallet/createPrivateKey25519" -H "accept: application/json"
{
  "result" : {
    "proposition" : {
      "publicKey" : "ccc4f785ba27abd41fb4fc8c378f25024dce4007ad2ecc8e974f6e8aaf7a6021"
    }
  }
}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Je dispose d'une nouvelle adresse : <code>ccc4f785ba27abd41fb4fc8c378f25024dce4007ad2ecc8e974f6e8aaf7a6021</code> sur laquelle je vais envoyer 100ZEN.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>L'envoi se fait grâce à l'appel de <code>/transaction/sendCoinsToAddress</code>dans l'API. Les paramètres sont un tableau <code>outputs</code> contenant l'adresse destinataire <code>publicKey</code> et le montant <strong>en zennies</strong> : <code>value</code> et les fees <code>fee</code>. Attention <code>fee</code>est bien en dehors du tableau <code>outputs</code>. Pour l'exemple, on ne paye pas de fees.</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>{
  "outputs": &#91;
    {
      "publicKey": "ccc4f785ba27abd41fb4fc8c378f25024dce4007ad2ecc8e974f6e8aaf7a6021",
      "value": 10000000000
    }
  ],
  "fee": 0
}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Voici l'appel complet, qui retourne un identifiant de transaction :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ curl -X POST "http://127.0.0.1:9085/transaction/sendCoinsToAddress" -H "accept: application/json" -H "Content-Type: application/json" -d "{\"outputs\":&#91;{\"publicKey\":\"ccc4f785ba27abd41fb4fc8c378f25024dce4007ad2ecc8e974f6e8aaf7a6021\",\"value\":10000000000}],\"fee\":0}"
{
  "result": {
    "transactionId": "56d0c09c0f1a0977ee44a27a1902d749c6e889e40b1e3547625652ed07509711"
  }
}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Je vérifie les transactions en attente grâce à l'API, en appelant  <code>/transaction/allTransactions</code>. Cette méthode prend un paramètre <code>format</code>. S'il vaut <code>true</code>, le contenu des transactions est affiché. S'il vaut <code>false</code>, la commande retourne un tableau d'identifiants des transactions en attente :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ curl -X POST "http://127.0.0.1:9085/transaction/allTransactions" -H "accept: application/json" -H "Content-Type: application/json" -d "{\"format\":true}"
{
  "result" : {
    "transactions" : &#91; {
      "modifierTypeId" : 2,
      "id" : "56d0c09c0f1a0977ee44a27a1902d749c6e889e40b1e3547625652ed07509711",
      "fee" : 0,
      "timestamp" : 1595635064137,
      "newBoxes" : &#91; {
        "nonce" : 9217280667005346328,
        "id" : "a6ef1bf988117546920125e57c6e62d7d6e1d587e072cf7c3941a991dd5e391b",
        "typeId" : 1,
        "proposition" : {
          "publicKey" : "ccc4f785ba27abd41fb4fc8c378f25024dce4007ad2ecc8e974f6e8aaf7a6021"
        },
        "value" : 10000000000
      }, {
        "nonce" : 9099696201179751615,
        "id" : "86af78e454b3e2725f3aec0f1036dd7368c81d8744bd4c5d521f8110b34fda0d",
        "typeId" : 1,
        "proposition" : {
          "publicKey" : "a03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f8"
        },
        "value" : 2000000000
      } ],
      "unlockers" : &#91; {
        "boxKey" : {
          "signature" : "7bf9b0b240f6073c144db424094a1eefd77344913c8b7dd6bb035600e094477225608adf570b6722077448ad6956e94bbfad7a8ca7644b5316f9d20b187e250b",
          "typeId" : 1
        },
        "closedBoxId" : "5d7e803f4dbdcb24d0f4ebb1913f6e47b1d9ae00320509d470df7b90206ef764"
      } ],
      "typeId" : 3
    } ]
  }
}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Je génère le bloc suivant (<code>/block/generate</code>), je vérifie qu'il n'y a plus de transactions en attente (<code>transaction/allTransactions</code>) et enfin, je vérifie mes "boxes" (<code>/wallet/allBoxes</code>). </p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p><strong>Nota bene : </strong>Avec 2 nodes différents, il suffirait de vérifier la balance sur chacun.</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ curl -X POST "http://127.0.0.1:9085/block/generate" -H "accept: application/json" -H "Content-Type: application/json" -d "{\"epochNumber\":2,\"slotNumber\":2}"
{
  "result": {
    "blockId": "8d2b3c5ce124296a001cf413e4ef4249fb57ca24ec721531aa2a13201f92d321"
  }
}
$ curl -X POST "http://127.0.0.1:9085/transaction/allTransactions" -H "accept: application/json" -H "Content-Type: application/json" -d "{\"format\":true}"
{
  "result" : {
    "transactions" : &#91; ]
  }
}
$ curl -X POST "http://127.0.0.1:9085/wallet/allBoxes" -H "accept: application/json" -H "Content-Type: application/json"
{
  "result" : {
    "boxes" : &#91; {
      "nonce" : -1642701030306306476,
      "id" : "7006b227eaa723e4afffb404383fd07166959275ec9c8f7e6665314c85ff1b03",
      "vrfPubKey" : {
        "valid" : true,
        "publicKey" : "0ed74aa4afda5ffb358ba4906d0a5429f9840805e1fcc9ea71fca62afb5e97d779d0d2d3dd1bd9a3563166eff9d12b9ab9b6f11baf5639606689c72e16e2577ee338229e015697c8d174782b4caa04af9902eca2f3c384c6f558cf685b11010084a8436bbb18f7ccd039c249afa69f868c79939923ee3fc3ec437b998246db5b4907881702641ec6b41fdc6de644087713115fa2548121280fe8ac85fe31387aee6465767d8ba2189fab2ee8bf702beda6af018b4d68f890176bf2ab599e000000"
      },
      "typeId" : 3,
      "blockSignProposition" : {
        "publicKey" : "a03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f8"
      },
      "proposition" : {
        "publicKey" : "a03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f8"
      },
      "value" : 40000000000
    }, {
      "nonce" : 9217280667005346328,
      "id" : "a6ef1bf988117546920125e57c6e62d7d6e1d587e072cf7c3941a991dd5e391b",
      "typeId" : 1,
      "proposition" : {
        "publicKey" : "ccc4f785ba27abd41fb4fc8c378f25024dce4007ad2ecc8e974f6e8aaf7a6021"
      },
      "value" : 10000000000
    }, {
      "nonce" : 9099696201179751615,
      "id" : "86af78e454b3e2725f3aec0f1036dd7368c81d8744bd4c5d521f8110b34fda0d",
      "typeId" : 1,
      "proposition" : {
        "publicKey" : "a03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f8"
      },
      "value" : 2000000000
    } ]
  }
}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Comme prévu, la liste de transactions en attente est vide après la génération du bloc et je retrouve bien mes 400 ZEN de la création de la chaîne, les 100ZEN sur l'adresse <code>ccc4f785ba27abd41fb4fc8c378f25024dce4007ad2ecc8e974f6e8aaf7a6021</code> et les 20 ZEN restants sur <code>a03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f8</code>.</p>
<!-- /wp:paragraph -->

<!-- wp:heading -->
<h2>Backward transfer : de la sidechain à la mainchain</h2>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>Tous ces ZEN sur la sidechain ne me sont pas forcément tous utiles. Je vais en rapatrier 84 sur la mainchain pour me faire 2 secure nodes à l'avenir.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Il y a une méthode dans l'API qui se charge de tout ça pour moi : <code>/transaction/withdrawCoins</code>. Les paramètres sont les mêmes que dans le cas de <code>/transaction/sendCoinsToAddress</code>, à ceci près que l'adresse destinataire doit être une adresse sur la mainchain Horizen.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>D'ailleurs, je vais de ce pas me créer une adresse dédiée où recevoir ces ZEN avec la méthode <code>getnewaddress</code> appelée grâce à <strong>zen-cli</strong>.</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ zen-cli -regtest getnewaddress
ztgGE5xq1cx71HnQpTgnoz8Jg3DoZMZnMjf</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Je fais ma demande de retrait sur cette nouvelle adresse. Pour rappel, le montant dans les appels à la sidechain sont à saisir en zennies, pas en ZEN : </p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ curl -X POST "http://127.0.0.1:9085/transaction/withdrawCoins" -H "accept: application/json" -H "Content-Type: application/json" -d "{\"outputs\":&#91;{\"publicKey\":\"ztgGE5xq1cx71HnQpTgnoz8Jg3DoZMZnMjf\",\"value\":8400000000}],\"fee\":0}"
{
  "result" : {
    "transactionId" : "98645fdf38e98e200a58306866c83ad9ff0742c6fc37114d27d42c097b13f0ff"
  }</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>J'obtiens mon identifiant de transaction, je vérifie qu'il est bien en attente :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ curl -X POST "http://127.0.0.1:9085/transaction/allTransactions" -H "accept: application/json" -H "Content-Type: application/json" -d "{\"format\":false}"
{
  "result" : {
    "transactionIds" : &#91; "98645fdf38e98e200a58306866c83ad9ff0742c6fc37114d27d42c097b13f0ff" ]
  }
}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Je génère un bloc dans la sidechain et un dans la mainchain et je vérifie mes soldes :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ curl -X POST "http://127.0.0.1:9085/block/generate" -H "accept: application/json"  -d "{\"epochNumber\":2,\"slotNumber\":5}"
{
  "result" : {
    "blockId" : "2b68e8297213eab66048e6ae9576cead42081b4b24ed5e01bb369df54340678e"
  }
}
$ zen-cli -regtest generate 1
&#91;
  "06ae9a87a00a780c7dd105d0cb89bbbce65961ad65a9148c31f84324839605e6"
]
$ zen-cli -regtest listaddressgroupings
&#91;
  &#91;
    &#91;
      "ztWvWCsBC18VY7RDWL1fMYnkgTm4vw1eMM8",
      3.12498224
    ],
    &#91;
      "zte2BTFk6GQLy8Lwb4HdnB8P5HWVuWPwpf1",
      0.31242237
    ],
    &#91;
      "ztf4LHNHXeXfDVVaBwhJd8WwKweHt2U8HTm",
      839.31250000
    ]
  ],
  &#91;
    &#91;
      "ztgGE5xq1cx71HnQpTgnoz8Jg3DoZMZnMjf",
      0.00000000,
      ""
    ]
  ]
]
$ curl -X POST "http://127.0.0.1:9085/wallet/balance" -H "accept: application/json"
{
  "result" : {
    "balance" : 43600000000
  }
}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Zut ... Mon adresse <code>ztejm2TvhUY4HvgRLZZUTwnsuDZAsnv8Fqq</code> est toujours à 0 alors que la somme a été prélevée sur mon compte de la sidechain.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>En effet, le processus de <strong>backward transfer</strong> est beaucoup plus complexe que le <strong>forward transfer</strong>. On doit respecter des <strong>withdrawals epoch</strong>. Un nombre de blocs séparant la prise en compte des retraits. Rappelle-toi, c'était le paramètre <code>"withdrawalEpochLength": 10</code> que j'ai utilisé lors de la création de la sidechain.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>En surveillant les messages de debug, lors de la génération des blocs, tu as dû remarquer ces messages par exemple :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>&#91;2-Hop-akka.actor.default-dispatcher-5] INFO com.horizen.certificatesubmitter.CertificateSubmitter - Can't submit certificate, withdrawal epoch info = WithdrawalEpochInfo(0,5)</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Les chiffres entre parenthèses indique l'epoch courante ainsi que le numéro de bloc dans l'epoch. Le transfert reste en attente jusqu'au bloc 1 de l'epoch qui suit la demande.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p><strong>Astuce :</strong> A partir d'ici pour synchroniser les créations de blocs entre la mainchain et la sidechain de tests, j'utilise une variable <code>nbloc</code> initialisée à 5 puis utilisée dans cette commande qui génère un bloc de mainchain et un bloc de sidechain, tout en incrémentant le compteur de blocs. (Tant qu'on reste en dessous de 720 blocs, ça fonctionne).</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>zen-cli -regtest generate 1 ; nbloc=$((${nbloc}+1)) ; curl -X POST "http://127.0.0.1:9085/block/generate" -H "accept: application/json"  -H "Content-Type: application/json" -d "{\"epochNumber\":2,\"slotNumber\":${nbloc}}"</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p><strong>ATTENTION :</strong> l'<code>epochNumber</code> de la sidechain utilisé dans la génération de blocs n'a rien à voir avec l'epoch de la <code>WithdrawalEpochInfo</code>.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Après plusieurs générations de blocs arrive le message suivant qui valide la tentative de transfert de la sidechain à la mainchain (bloc n°1 de l'epoch suivant) :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>&#91;2-Hop-akka.actor.default-dispatcher-4] INFO com.horizen.certificatesubmitter.CertificateSubmitter - Can submit certificate, withdrawal epoch info = WithdrawalEpochInfo(1,1)
2-Hop-scorex.executionContext-582] INFO com.horizen.certificatesubmitter.CertificateSubmitter - Start generating proof for 1 withdrawal epoch number, with parameters: withdrawalRequests=(), endWithdrawalEpochBlockHash=099e56f2bc2800dfd49cf52b25e6b303e568ed1fe531387d74a188d54ecc0c26, prevEndWithdrawalEpochBlockHash=01f4b681be7f7af5fef263e20ab59e44d2e570b47be71e940a57ef96267f1533, signersThreshold=5. It can a while</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Et ça dure effectivement longtemps. Ne perds pas patience, ça se compte en minutes sur un vps de base. A l'issue de la génération du certificat contenant de transfert, il est transféré à la mainchain.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>On obtient un message de débugage ressemblant à ceci dans la console de SimpleApp, avec notamment, en deuxième ligne le montant, en ZEN.</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>INFO com.horizen.certificatesubmitter.CertificateSubmitter - Backward transfer certificate request was successfully created for epoch number 1, with proof &#91;...] with quality 7 try to send it to mainchain
&#91;zen-cli, -regtest, send_certificate, &#91;...] &#91;{"pubkeyhash":"fb24e8fffcb006b507f5ba5b83b5ff6eaaecc88f","amount":"84"}], 0.00001]
&#91;2-Hop-scorex.executionContext-422] INFO com.horizen.certificatesubmitter.CertificateSubmitter - Backward transfer certificate response had been received. Cert hash = 8ab21a52e428510a1fee07b8161ac4d04c6c199a60713e8abc6730edcf2fe7b7</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>On obtient la réponse de la mainchain dans le bloc suivant (le bloc 2, de l'epoch suivant l'initiation du transfert). C'est validé dans la console par ce message :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>
&#91;2-Hop-akka.actor.default-dispatcher-273] INFO com.horizen.SidechainState - Block contains successfully verified backward transfer certificate for epoch %d
</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Mais les fonds ne sont toujours pas sur mon adresse. En effet, le transfert doit encore être validé. Il faut pour ça attendre le début de l'époch suivant et la validation en retour de la mainchain dans le bloc 2 de l'epoch suivant.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Pour résumer, dans le cas de blocs synchronisés entre la mainchain et la sidechain  :</p>
<!-- /wp:paragraph -->

<!-- wp:list -->
<ul><li>demande de transfert en epoch 0, bloc 3</li><li>la sidechain en informe la mainchain en epoch 1, bloc 1</li><li>le montant arrive sur la main chain en epoch 2, bloc 2</li></ul>
<!-- /wp:list -->

<!-- wp:paragraph -->
<p>Et bien voilà, c'est tout pour la présentation des sidechains Horizen. Si tu as suivi le dossier, tu sais à présent, à partir d'un VPS debian ou ubuntu vierge :</p>
<!-- /wp:paragraph -->

<!-- wp:list -->
<ul><li>préparer ton environnement et installer les prérequis</li><li>compiler les nodes Mainchain et Sidechain</li><li>déclarer une sidechain sur la mainchain</li><li>faire un transfert de la mainchain à la sidechain</li><li>faire des transferts entre adresses de la sidechain</li><li>faire un transfert d'une sidechain vers une mainchain</li></ul>
<!-- /wp:list -->

<!-- wp:paragraph -->
<p>Il reste beaucoup à dire et à faire mais j'espère que ce tour d'horizon t'aidera à mettre le pied à l'étrier des Sidechains Horizen.</p>
<!-- /wp:paragraph -->

<!-- wp:heading -->
<h2>Le plan du dossier : Les Sidechains Horizen</h2>
<!-- /wp:heading -->

<!-- wp:list {"ordered":true} -->
<ol><li><a href="https://mescryptos.fr/les-sidechains-horizen-le-noeud-zend-oo/">Zend_oo, le node de la MainChain Horizen</a></li><li><a href="https://mescryptos.fr/les-sidechains-horizen-ma-premiere-sidechain/">Création de ma première sidechain</a></li><li>Sidechains : Transférer du ZEN : Tu es ici :)</li><li>Bonus : <a href="https://mescryptos.fr/gagner-des-zen-ca-detend/">Gagner des ZEN : Le Faucet en détail</a></li><li>Bonus : <a href="https://mescryptos.fr/ton-node-zen-en-20-minutes-chrono/">Monter un secure node ZEN en 20 minutes</a></li></ol>
<!-- /wp:list -->

<!-- wp:heading -->
<h2>Liens</h2>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>Le README en anglais sur GiHub : <a href="https://github.com/ZencashOfficial/zend_oo/blob/sidechains_testnet/README.md">https://github.com/ZencashOfficial/zend_oo/blob/sidechains_testnet/README.md</a></p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Présentation des sidechains dans l'Academy Horizen Academy (VF) : <a href="https://academy.horizen.global/fr/horizen/expert/sidechains/">https://academy.horizen.global/fr/horizen/expert/sidechains/</a></p>
<!-- /wp:paragraph -->
