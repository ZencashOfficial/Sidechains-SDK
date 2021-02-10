<!-- wp:paragraph -->
<p>On reprend la présentation des sidechains Horizen. Dans cet épisode on s'intéresse à la création de notre première sidechain, grâce aux outils du Sidechains-SDK Horizen.</p>
<!-- /wp:paragraph -->

<!-- wp:heading -->
<h2>Le plan du dossier : Les Sidechains Horizen</h2>
<!-- /wp:heading -->

<!-- wp:list {"ordered":true} -->
<ol><li><a href="https://mescryptos.fr/les-sidechains-horizen-le-noeud-zend-oo/">Zend_oo, le node de la MainChain Horizen</a></li><li>Création de ma première sidechain : Tu es ici :)</li><li><a href="https://mescryptos.fr/les-sidechains-horizen-transfert-de-zen/">Transferts de ZEN entre chaînes</a></li><li>Bonus : <a href="https://mescryptos.fr/gagner-des-zen-ca-detend/">Gagner des ZEN : Le Faucet en détail</a></li><li>Bonus : <a href="https://mescryptos.fr/ton-node-zen-en-20-minutes-chrono/">Monter un secure node ZEN en 20 minutes</a></li></ol>
<!-- /wp:list -->

<!-- wp:heading -->
<h2>Installation Java8, Maven &amp; Scala</h2>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>Je commence par me reconnecter à l'environnement mis en place lors du premier épisode. Pour rappel, on doit se connecter en tant qu'utilisateur <strong>zendoo</strong>.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Il y a tout d'abord quelques prérequis à installer.</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ sudo apt-get update
$ sudo apt-get install -y software-properties-common
$ wget -qO - https://adoptopenjdk.jfrog.io/adoptopenjdk/api/gpg/key/public | sudo apt-key add -
$ sudo add-apt-repository --yes https://adoptopenjdk.jfrog.io/adoptopenjdk/deb/
$ sudo apt-get update
$ sudo apt-get install adoptopenjdk-8-hotspot
$ sudo apt-get install maven</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Je vérifie que tout s'est bien déroulé et que je dispose bien de <strong>Java8 </strong>et <strong>Maven </strong>:</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ java -version
openjdk version "1.8.0_252"
OpenJDK Runtime Environment (AdoptOpenJDK)(build 1.8.0_252-b09)
OpenJDK 64-Bit Server VM (AdoptOpenJDK)(build 25.252-b09, mixed mode)

$ mvn --version
Apache Maven 3.6.0
Maven home: /usr/share/maven
Java version: 1.8.0_252, vendor: AdoptOpenJDK, runtime: /usr/lib/jvm/adoptopenjdk-8-hotspot-amd64/jre
Default locale: en_US, platform encoding: UTF-8
OS name: "linux", version: "4.19.0-8-cloud-amd64", arch: "amd64", family: "unix"</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>J'installe <strong>Scala-sbt</strong> en complément :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
$ curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&amp;search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo apt-key add
$ sudo apt-get update
$ sudo apt-get install sbt</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Je teste dans un répertoire temporaire :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ mkdir sbttest
$ cd sbttest
$ sbt</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Une fois dans <strong>sbt </strong>(le premier lancement peut être assez long), je vérifie la version (<code>about</code>) puis je quitte (<code>exit</code>) :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>sbt:sbttest> about
&#91;info] This is sbt 1.3.13
&#91;info] The current project is ProjectRef(uri("file:/home/zendoo/sbttest/"), "sbttest") 0.1.0-SNAPSHOT
&#91;info] The current project is built against Scala 2.12.10
&#91;info] Available Plugins
&#91;info]  - sbt.ScriptedPlugin
&#91;info]  - sbt.plugins.CorePlugin
&#91;info]  - sbt.plugins.Giter8TemplatePlugin
&#91;info]  - sbt.plugins.IvyPlugin
&#91;info]  - sbt.plugins.JUnitXmlReportPlugin
&#91;info]  - sbt.plugins.JvmPlugin
&#91;info]  - sbt.plugins.SbtPlugin
&#91;info]  - sbt.plugins.SemanticdbPlugin
&#91;info] sbt, sbt plugins, and build definitions are using Scala 2.12.10
sbt:sbttest> exit
&#91;info] shutting down sbt server</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Je peux alors quitter et supprimer le répertoire <code>sbttest</code> :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ cd ..
$ rm -rf sbttest/</code></pre>
<!-- /wp:code -->

<!-- wp:heading -->
<h2>Compilation des éléments du Sidechains-SDK</h2>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>Je commence par récupérer le Sidechains-SDK, je me place dans le répertoire ainsi créé et je lance la compilation :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ git clone https://github.com/ZencashOfficial/Sidechains-SDK.git
$ cd Sidechains-SDK
$ mvn package </code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Si Maven rapporte une erreur disant qu'il ne trouve pas <code>/usr/lib/jvm/adoptopenjdk-8-hotspot-amd64/jre/bin/javac</code>, c'est qu'il ne cherche pas au bon endroit (dans le sous répertoire <code>jre</code> au lieu du répertoire du JDK qui est un étage plus haut). Il faut corriger ça en forçant la variable d'environnement <code>JAVA_HOME</code> et relancer :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ export JAVA_HOME=/usr/lib/jvm/adoptopenjdk-8-hotspot-amd64/
$ mvn package</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Cette commande va construire les 4 élements du SDK :</p>
<!-- /wp:paragraph -->

<!-- wp:list -->
<ul><li>Le coeur du <strong>SDK </strong>lui même</li><li>Les <strong>Bootstraping tools</strong>, des outils facilitant la création de la configuration d'une sidechain.</li><li><strong>SimpleApp </strong>un exécutable permettant de faire tourner un node d'une sidechain par défaut.</li><li><strong>Qa </strong>: Le sidechain test framework</li></ul>
<!-- /wp:list -->

<!-- wp:paragraph -->
<p>En théorie, tout doit bien se passer, c'est beaucoup plus rapide que la compilation de <strong>Zend_oo</strong> et on termine sur un résumé :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>&#91;INFO] ------------------------------------------------------------------------
&#91;INFO] Reactor Summary for Sidechains 0.2.1:
&#91;INFO]
&#91;INFO] Sidechains-SDK ..................................... SUCCESS &#91;06:15 min]
&#91;INFO] Sidechains-SDK-simpleapp ........................... SUCCESS &#91;  9.294 s]
&#91;INFO] Sidechains-SDK-ScBootstrappingTools ................ SUCCESS &#91; 18.948 s]
&#91;INFO] Sidechains ......................................... SUCCESS &#91;  0.011 s]
&#91;INFO] ------------------------------------------------------------------------
&#91;INFO] BUILD SUCCESS
&#91;INFO] ------------------------------------------------------------------------
&#91;INFO] Total time:  06:44 min
&#91;INFO] Finished at: 2020-07-15T22:49:29Z
&#91;INFO] ------------------------------------------------------------------------</code></pre>
<!-- /wp:code -->

<!-- wp:image {"align":"center","id":373,"sizeSlug":"full"} -->
<div class="wp-block-image"><figure class="aligncenter size-full"><img src="https://mescryptos.fr/wp-content/uploads/2020/07/ZEN-281ccbe9cfe0fdbcaa48c2c1788cd3a3.png" alt="281ccbe9cfe0fdbcaa48c2c1788cd3a3.png" class="wp-image-373"/></figure></div>
<!-- /wp:image -->

<!-- wp:heading -->
<h2>Obtention des données préliminaires !</h2>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>Allez, c'est parti, je vais pouvoir m'attaquer à la configuration et au lancement de ma sidechain !</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Je vais pour cela utiliser l'outil de Bootstrapping, en étant toujours dans le répertoire <code>Sidechains-SDK</code> :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ java -jar tools/sctool/target/Sidechains-SDK-ScBootstrappingTools-0.2.1.jar
Tool successfully started...
Please, enter the command:</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Cet outil permet de saisir des commandes et leurs arguments au format JSON. Les réponses sont également au format JSON.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Il me faut 2 paires de clés pour initialiser ma sidechain. Et des chaînes aléatoires pour initialiser le générateur de nombres aléatoires lui même lors de chaque appel.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Générer une valeur d'initialisation de générateur de nombres aléatoires est un challenge à lui tout seul. Pour l'exemple, on se contentera de chaînes de 32 caractères créées à l'aide d'un générateur de mot de passe : <code>"ijsj3J00NyPzcOjnkoNYDsgzuHAfiIrI"</code> pour la première paire, <code>"47w0u4exA70266o80w54zv537gps5O4F"</code> pour la seconde et <code>"72S8h385u0327kuO7h0392LL1i0Geo2v"</code> pour la dernière.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Attention à bien conserver toutes les réponses de l'outil dans un fichier texte pour la suite des opérations.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Je commence par ces deux paires de clés (ed25519 puis Vrf) :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>generatekey {"seed":"ijsj3J00NyPzcOjnkoNYDsgzuHAfiIrI"}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Réponse obtenue (une fois remise en forme). A noter, la <code>publicKey</code> sera la première adresse de mon wallet sur la sidechain :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>{
    "secret":"00531e40d4036d808d2101756debc7fd2365cd76da1176f5bf2ed09603d01f711ca03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f8",
    "publicKey":"a03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f8"
}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Je procède de même pour la clé Vrf (attention, la commande n'est pas la même)</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>generateVrfKey {"seed":"47w0u4exA70266o80w54zv537gps5O4F"}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Réponse obtenue (ça doit être plus long que la précédente paire) :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>{
    "vrfSecret":"0300000060b218f4c543504110af4dd02a72a764d5c1c59beb1e6dc3ea0dd85f86685eade90f567f9937b3a4fc36bb5adfe480ac381ccdbb91f11c269e2a41d71d829fc312cdef48bf75818447405bb86202a1d39f0503491207eab329bb77047805f800000ed74aa4afda5ffb358ba4906d0a5429f9840805e1fcc9ea71fca62afb5e97d779d0d2d3dd1bd9a3563166eff9d12b9ab9b6f11baf5639606689c72e16e2577ee338229e015697c8d174782b4caa04af9902eca2f3c384c6f558cf685b11010084a8436bbb18f7ccd039c249afa69f868c79939923ee3fc3ec437b998246db5b4907881702641ec6b41fdc6de644087713115fa2548121280fe8ac85fe31387aee6465767d8ba2189fab2ee8bf702beda6af018b4d68f890176bf2ab599e000000",
    "vrfPublicKey":"0ed74aa4afda5ffb358ba4906d0a5429f9840805e1fcc9ea71fca62afb5e97d779d0d2d3dd1bd9a3563166eff9d12b9ab9b6f11baf5639606689c72e16e2577ee338229e015697c8d174782b4caa04af9902eca2f3c384c6f558cf685b11010084a8436bbb18f7ccd039c249afa69f868c79939923ee3fc3ec437b998246db5b4907881702641ec6b41fdc6de644087713115fa2548121280fe8ac85fe31387aee6465767d8ba2189fab2ee8bf702beda6af018b4d68f890176bf2ab599e000000"
}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Enfin je dois générer des données pour la validation des transactions entre la sidechain et la mainchain. Je les obtiens grâce à l'appel <code>generateProofInfo</code>. La valeur du paramètre <code>keyCount</code> est fixée à 7 pour l'instant. Pour ce qui est de <code>threshold</code>, il doit être inférieur ou égal à 7. La documentation de SimpleApp utilise 5. Donc, j'ai fait de même. Enfin, j'utilise la dernière des trois valeurs d'initialisations de seed précédemment générée.</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>generateProofInfo {"seed":"72S8h385u0327kuO7h0392LL1i0Geo2v", "keyCount":7, "threshold":5}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>La réponse obtenue :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>{
  "threshold":5,
  "genSysConstant":"b1acb36cdc03885d0f8ebeff5f1b7c21e3719f875a733ed58170621852d45dcd51b96d38e2dbb2002b43c44742d02d1c348280112fa2fe282f01a6a2615d0d33d19b03a0f5337c8ef41c2d84e2db25f56eb88bcf2ed76a094801d2fc83160100",
  "verificationKey":"5e7b462cc84ae0faaa5884bd5c4a5a5edf13db210599aeeb4d273c0f5f32967b7071ce2b4d490b9f08f6ce66a8405735c79197cd6773d1c5aeb2a38da1c102df07b05879c77198e5aafa7feed25d4137e86b3d98d9edd9547a460f1615b10000ee9570fbffedd44170477b37500a0a1cb3f94b6361f10f8a68c4075fbc17542d7174b3d95e12ddb8aea5d6b6c53c1df6c8f60010cd2e69902ba5e89e86747569463a23254730fc8d2aabf39648a505df9dcce461443b181ef3eda46074070000550836db2c97820971db6b1421e348d946ed4d3f255295abea46556615e3123de33ec56f784f70302901a4bc10c79c6a8b1e32477aeff9fba75876592981b678fc5a2703ac0b3055e567a6cb1ebab578fc4f9121fd968680250696cb85790000078fcfb60bdfc79aa1e377cb120480538e0236156f23129a88824ca5a1d77e371e5e98a16e6f32087c91aa02a4f5e00e412e515c3b678f6535141203c6886c637b626a2ada4062d037503359a680979091c68941a307db6e4ed8bc49d21b00002f0e6f88fb69309873fdefb015569e5511fb5399295204876543d065d177bf36ab79183a7c5e504b50691bc5b4ed0293324cfe2555d3fc8e39485822a90a91afcd4ef79ec3aefbd4cbe25cbccd802d8334ce1dce238c3f7505330a14615500001f89fbe1922ab3aa31a28fd29e19673714a7e48050dee59859d68345bb7bee7d5e888d8b798a58d7c650f9138304c05a92b668294c6114185ccb2c67ce0bbbb7e1dcbb6d76f5cacd7c9732a33b21d69bd7a28c9cca68b5735d50413862bc0100308bb0dd0bd53f3d1134966702dd3c7cc8b58b270a6996a646493250b0d5f3978d0c971f8fa7a0c958f3efe2fa5269244973fafb701c2eb66dd25901f93d677ab6c538c1ed11f115e52d3f2c7087ea40c3e8cd089376baa38842e9429b5f0000d19a8d874d791f952f13d3c8ecd92e44009c09815e5ae6a8e5def7ea52fe3de4accfb5ba2aa401fbcec14b069cd0dc0f66ab025b45ef9831a26acf58673db7487043654e7980fcb2b6c1bd7593a4dfff810436f653e309121c7ccf2df70b010000732254ec6df184be360cd9ed383ed7c8c236d7761cfc0ce4e7f0cac5a06f4edab9cfc75a7dc1449c0e18ed9564c974c2e1b6847c637f74e5d391cbc80fc6e672ffd66b5ce4fb73bda8359ab8a0ea1e855df1e07d82f93c935c7e1a9a55c5000065efdbb7c3e82291a482b2f24cbd46f4dd02c370cf6dcfe8fb3c00b8b004b5ad51369b1f1b134a824d1f16d72ca6a27ba2d6190150329139cf2c6d9e5a14722f8d39b96b882c1f60a7b230e929819e2abe1cd9d7f3e8c726b1a94d20c8010100732c396eca6ffa1bf851cef449f2f087edd93e4f641b4bd93a482d9f129e675aedb688993d4e2cee824d2803301364ba10fbb66895927adb53bad8aefe8a1caab6f4ccb45883e414a1223ac7f90a89087cd752dfa0c7b3e19bbae000edd5000028d1d23c627d1252d2a2a20a246af2280f50e3fde667873aadd9893ba6833118358398e7428e717128f764714a8d52b090c1f554f58e25ea815338d7bc7326c949567e74f2f2ab3c88f5075fea75594608b8937c9059a42d712ffbd1bd980100000000000250c1a474689e375a309446e5cdd3a0c26cecdcff5c7b8cdc0728868983f1a35a49e3a1bae6f969c3d47356c08d3d169d2c0a2be908d82cd35f41a23d8c2924a9f790ab3a00d53061d440a176670d6a32de2ecd19cf8a9774729c09a6ea4d0100d8838bf55d95521291da12294b302c66042eda0dc2acc79360a1fdd8c9a366fa790c52bf926c2d96b5ba88a3a443487c5235f7c476f350c2101cfbe3bd0361dd291ebc5e42c097a158704b71006886a3662ca6db7d816b4ad12444835d89000000795ce2b34aef921ccb3d9b9695f5d3fe0a03743c955cfcf01f8a1815a7c8b03de85fe15201d4b4b6f401cb334a6988ea5bde8986a468c47c3c6a5ae96a3160ff15e06699ea82bd40c0d5547fe1be77af7817861bbfcca3f4232f05a9cec800006c216565cee4d57b32d2d70bb3cb8d4a967c0eb5d7137b2ec58466f3d4d3b5375e4baa823bcc29c6ad877d9708cd5dc1c31fa3883a80710431110c4aa22e97b67fa639f54e86cfab87187011270139df7873bed12f6fb8cd9ab48f389338010000",
  "schnorrKeys":&#91;
      {
              "schnorrSecret":"04000000600bbacac1017bb3a249a07409a83829df212bc3774069f9f3d758496bf8048d356799c7bd9fdf76369616ffc405c5844ee464ad628345a753a35bc6dfd3839ea7519896e45b2f1b610cc34aea072b5d523fd0e1a95f357d90261783c073f40000328f42588d9df851c2c2a2b11336a5737ba913c0f92177c9d0a854c7564b48da761e53205d30d7f60e21ac382b452cabb7f9274e199a59976b0bd6aaf8f59ac171f6444b9bc594473a20c5a3c89e0d1335ffc8d0ee821c60ebcb1239601d0100dae9144ae4bd8d56638befec496d97eadc590acc2a04b9a210ec475b2505cd6774b1be10353b060c31ae1142b90f436fb5e4e1905814eec8a3e86e0cc5fcff36b686bb92d2f6200ebfc714cfd48db5c1ab3fa77b4747fb60a4f7ae6bd56e000000",
              "schnorrPublicKey":"328f42588d9df851c2c2a2b11336a5737ba913c0f92177c9d0a854c7564b48da761e53205d30d7f60e21ac382b452cabb7f9274e199a59976b0bd6aaf8f59ac171f6444b9bc594473a20c5a3c89e0d1335ffc8d0ee821c60ebcb1239601d0100dae9144ae4bd8d56638befec496d97eadc590acc2a04b9a210ec475b2505cd6774b1be10353b060c31ae1142b90f436fb5e4e1905814eec8a3e86e0cc5fcff36b686bb92d2f6200ebfc714cfd48db5c1ab3fa77b4747fb60a4f7ae6bd56e000000"
        },
        {
            "schnorrSecret":"0400000060798dd23ca90d7ea6d85353c2a7d094cf75146ffa099a5c6b6de9a8bdcc0e4fd4edb271f6000a26cfb80b9cffe990d1dd6193e1031fe1b419e47653596aa7289a005eb19d0407783b0d70a3035c3de537c95be19b7d4b5b6b63cdd2b369d90000a3c8f596cec2583278fcb7b460d0ae3b99283d26a22b599b1bfc41a5bc1a1328540eb64362f688437c553c114291853b2ab752fb6baf172f9a7cbdf7fbdb382445cd4672f313426fa92b071ce840103c6ee1463ff0c94a467bec754829a30000815e97336edbccb9523e80699f5c9ef893b5d51e41a50e757f86fc8aa13751b0f13041814140a98a4768f31917d59dd6af87e66a92a57beb25109bed3a12a6b4a9286746d778da486bec3e5bde2381187f243005011b6ccf05fd7568bf94000000",
            "schnorrPublicKey":"a3c8f596cec2583278fcb7b460d0ae3b99283d26a22b599b1bfc41a5bc1a1328540eb64362f688437c553c114291853b2ab752fb6baf172f9a7cbdf7fbdb382445cd4672f313426fa92b071ce840103c6ee1463ff0c94a467bec754829a30000815e97336edbccb9523e80699f5c9ef893b5d51e41a50e757f86fc8aa13751b0f13041814140a98a4768f31917d59dd6af87e66a92a57beb25109bed3a12a6b4a9286746d778da486bec3e5bde2381187f243005011b6ccf05fd7568bf94000000"
        },
        {
            "schnorrSecret":"04000000602d2b8008350f48fa073f4ac9129b8f4a7a7036b30f482cc5b3681cef02dfd3b713c2b08b0d989a5bd48656fe6995439cc6fdf99235b092343b13e1a7f43b937a9d488cfefe4d8f68afda8ebedec5a78cd5e7d77816223a7e88ac575d5c900100c76abd8288ec3b05e947adcf66a4fd91b8bbeae23d07cfe0196abffdc5d187f7c7cda485c00789f10d8c1650569a6fd4485cfa36e6247268e2aba1563beb3e2c8be87eae0df4a85f5057fddfd1f0bceef7998d20a9efcabc9e0a06e2fd7e010042c0738a904595ed5e3d7f29fb46b1e3918a597361824c7f7c2e9adb6c6ad947db05a36828aa3bb4355942fee130e2f3d5ed3eb35b7093c1cd31c8b8784e7f39c95c9a9b821b7eb867dcd4aa1dbf11410054e3ea1bd2759c479558604cb2000000",
            "schnorrPublicKey":"c76abd8288ec3b05e947adcf66a4fd91b8bbeae23d07cfe0196abffdc5d187f7c7cda485c00789f10d8c1650569a6fd4485cfa36e6247268e2aba1563beb3e2c8be87eae0df4a85f5057fddfd1f0bceef7998d20a9efcabc9e0a06e2fd7e010042c0738a904595ed5e3d7f29fb46b1e3918a597361824c7f7c2e9adb6c6ad947db05a36828aa3bb4355942fee130e2f3d5ed3eb35b7093c1cd31c8b8784e7f39c95c9a9b821b7eb867dcd4aa1dbf11410054e3ea1bd2759c479558604cb2000000"
        },
        { 
            "schnorrSecret":"0400000060e6d06e813f23b4229a1a9723e511ca08bf65219149b7adfa0ba97e3104a40a41a21a40916de846788f596d08b90005db6124b1d872b0a3dc472142046a8e7f4b7784917b4029b85a16c41bf964283d6b92743d240140460c0037783e396c010049e31139f382f266ecca749f54fcdf6bb4c48c2ccab895cb3971d9281ce7b3e3b5993728200867308547b6bc70fa04184e3b2d92732e463f763c0b91d3cc982272c875500d2a6c88b180ee59fad260853e37cbddf1ab9f268c31df00f73f01004a175a663b9a32586f36666b025bc8fa52c275cb8aadcf746825c834c4a5968ec5e083d55a6832bebee8c16adbb564ca3211bdc007e9c0be82b05230f3340525b2459f0acf24ad05eb9c5ab8c20432c289e735db90dcec3ef4b323dc4ef4000000",
            "schnorrPublicKey":"49e31139f382f266ecca749f54fcdf6bb4c48c2ccab895cb3971d9281ce7b3e3b5993728200867308547b6bc70fa04184e3b2d92732e463f763c0b91d3cc982272c875500d2a6c88b180ee59fad260853e37cbddf1ab9f268c31df00f73f01004a175a663b9a32586f36666b025bc8fa52c275cb8aadcf746825c834c4a5968ec5e083d55a6832bebee8c16adbb564ca3211bdc007e9c0be82b05230f3340525b2459f0acf24ad05eb9c5ab8c20432c289e735db90dcec3ef4b323dc4ef4000000"
        },
        {
            "schnorrSecret":"0400000060d563c1a1cec8e162db0df8d2c0f24adf292431e29fef77fe68977e106216d398f036fc605797583b5f0bcc427230188f32ebc9427b73468cda9385b380b7351bf3434afcda85725beb178efa40718b16ddadfa6bff47ee3edc17455456bd0000bb30f74e03b1448cc555d296849be13095e4ff7752ae5441fc68c61f2488cd36837abb8f95657e55ebbf20d8fccdc32673fd5f7c9db7355285f0d01ed707b273c78e85056e564dc613b7c7980611ec0b17fbe608e5c8135a233ed7e425540100bef6f0bb5e21dc446202a63a4892868ae5ef382d84dee1e2f46f6e33cf9c4bb7bdae5aeeec4e58e32ef113a70ff545dcfbaf80eb39907b11d76571dc1c913d07937297e20da6042e1d95b5e8642f10ab8dddc484c48722d140cd237392a5000000",
            "schnorrPublicKey":"bb30f74e03b1448cc555d296849be13095e4ff7752ae5441fc68c61f2488cd36837abb8f95657e55ebbf20d8fccdc32673fd5f7c9db7355285f0d01ed707b273c78e85056e564dc613b7c7980611ec0b17fbe608e5c8135a233ed7e425540100bef6f0bb5e21dc446202a63a4892868ae5ef382d84dee1e2f46f6e33cf9c4bb7bdae5aeeec4e58e32ef113a70ff545dcfbaf80eb39907b11d76571dc1c913d07937297e20da6042e1d95b5e8642f10ab8dddc484c48722d140cd237392a5000000"
        },
        {
            "schnorrSecret":"0400000060d4616f5ccab62713ff2ffb02353e14f13fc34a9ce0b3608a3d3833e8ca7abc54ebe6e981f5c80a2c758f5107d254b14fbff37cb51ad69fcfb5dcfa5bda71d048fe4e439b42a360894736f295681526800ce753800d778e733a20766270a100004f85443c89b5550569451310b320ce03d6ea117aebe492aac4510945f67e20ffdd308e52415f8b89508144fa1d66997d82090fcc55e1969e055ac8ca26e6fefcdd0c8930e76f94df70b0701769e6664adda248298002068c4188357f952001001ceb9119562ff4904eb01a807439a4cd5f8e226f10ade178ce081d6345f12576d988a6e80f0bf54ec9ca8548c264485deb522f8e57cf8f7184d9fb01f805f430df7150595a75e6553c1ff868ff992584f3a52327b3d822236c9214ed53c9000000",
            "schnorrPublicKey":"4f85443c89b5550569451310b320ce03d6ea117aebe492aac4510945f67e20ffdd308e52415f8b89508144fa1d66997d82090fcc55e1969e055ac8ca26e6fefcdd0c8930e76f94df70b0701769e6664adda248298002068c4188357f952001001ceb9119562ff4904eb01a807439a4cd5f8e226f10ade178ce081d6345f12576d988a6e80f0bf54ec9ca8548c264485deb522f8e57cf8f7184d9fb01f805f430df7150595a75e6553c1ff868ff992584f3a52327b3d822236c9214ed53c9000000"
        },
        {
            "schnorrSecret":"04000000603bdb5bb43e1adffb93265eccb0526f5ad1f7e671f30721da46abef1a3322ab5ab1a06b9bc89a2a7b9afd19295ad55fd9c2d775997883f51dc14b23e6a42721a710e3f7bdc7fce07ad74fd3fcb403c2eb61169bf8299eb8b43d726800492d010030949730d160baec1bf4fcc743f2a533dbd9f6e853960bad9adf1390294f58387c0af553117368aa8c5d7769a0b39279b0fc92001d36323878c9e7ed2bfa1048496049f1d6d147ab949f1f76b0719bd4fc542a8840469291069116e06f0701000c227ab8d2b235d5eab4866451ad850ab2634d20bafd3da8c0b697d919fab2937e9bec3d39e41c7bf028575bcf4acb5c9f9977cd97b190e110f7cd0deae72adeae2e33974e56a0929a36db8245d9784ba44f0f071aa3937ad0257c4c5248010000",
            "schnorrPublicKey":"30949730d160baec1bf4fcc743f2a533dbd9f6e853960bad9adf1390294f58387c0af553117368aa8c5d7769a0b39279b0fc92001d36323878c9e7ed2bfa1048496049f1d6d147ab949f1f76b0719bd4fc542a8840469291069116e06f0701000c227ab8d2b235d5eab4866451ad850ab2634d20bafd3da8c0b697d919fab2937e9bec3d39e41c7bf028575bcf4acb5c9f9977cd97b190e110f7cd0deae72adeae2e33974e56a0929a36db8245d9784ba44f0f071aa3937ad0257c4c5248010000"
        }
    ]
}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Ok, j'ai tout ce dont j'ai besoin. Je peux quitter l'outil de Bootstrapping à l'aide de la commande <code>exit</code> (qui n'a pas besoin de "seed", elle. Ouf !).</p>
<!-- /wp:paragraph -->

<!-- wp:heading -->
<h2>Déclaration de la sidechain</h2>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>Pour les tests, je vais utiliser ma mainchain ZEN à moi. Il faut lancer <strong>zend_oo</strong> avec 2 arguments supplémentaires : <code>regtest</code> pour avoir activer la prise en charge des sidechains (elles n'étaient pas encore sur le testnet lorsque j'ai rédigé cet article) et <code>-websocket</code> pour activer les websockets utilisées pour la communication entre mainchain et sidechains.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p><strong>ATTENTION :</strong> Pour utiliser <code>-regtest</code>, on doit désactiver le testnet dans le fichier de configuration de Zen. Donc, on remplace le cas échéant testnet=1 par testnet=0 dans <code>~/.zen/zen.conf</code> avant de lancer zend.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Une fois zend lancé , je vérifie égalment que je suis bien sur la mainchain vide. A noter qu'il faut utiliser le paramètre <code>regtest</code> pour zen-cli également ! On peut s'en passer en spécifiant <code>regtest=1</code> dans le fichier de configuration mais je trouve ça plus clair de le taper explicitement.</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ zend -regtest -websocket
Zen server starting
$ zen-cli -regtest getblockcount
0</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>D'après la doc du SDK, je dois générer 220 blocs pour activer les sidechains. La commande retourne un tableau des hashs des blocs créés. Comme je suis d'un naturel méfiant, je vérifie la présence de 220 blocs après !</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ zen-cli -regtest generate 220
&#91;
  "0ddfa91171f9f839e4db40c802e428bfda341382e1d65d9ea6b327830a33fcbb",
  ...
  "0a6b29f68598275b8aad9f8c56c82141aca7a0ff563786220938873500fd1baf"
]
$ zen-cli -regtest getblockcount
220</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Je peux finalement déclarer ma sidechain dans la mainchain ainsi peuplée de ces quelques blocs. Il faut pour cela utiliser l'appel <code>sc_create</code> qui prend les paramètres suivants :</p>
<!-- /wp:paragraph -->

<!-- wp:list -->
<ul><li>La fréquence (exprimée en blocs) entre deux possibles transferts de la sidechain vers la mainchain (Backward Transfer). On choisira 10 pour avoir des comptes ronds.</li><li>La <code>publicKey</code> obtenue en réponse à l'appel <code>generateKey</code> lors du bootstrap, mon adresse sur la sidechain.</li><li>Le nombre de tokens initiaux à envoyer à cette adresse. Je choisis 400, de façon empirique.</li><li>La clé de vérification des transferts. C'est la partie <code>verificationKey</code> de la réponse à l'appel <code>generateProofInfo</code> lors du bootstrap</li><li>La <code>publicKey</code> obtenue en réponse à l'appel <code>generateVrfKey</code> lors du bootstrap.</li><li>La partie <code>genSysConstant</code> de la réponse à l'appel <code>generateProofInfo</code> lors du bootstrap effectué précédemment.</li></ul>
<!-- /wp:list -->

<!-- wp:paragraph -->
<p>Une fois toutes ces données compilées, j'obtiens la commande peu digeste suivante. Si l'exécution se déroule sans problème, je dois obtenir en réponse la transaction de création de la sidechain (<code>txid</code> pour <strong>Transaction id</strong>), ainsi que l'identifiant de la sidechain créée (<code>scid</code> pour <strong>SideChain ID</strong>)</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ zen-cli -regtest sc_create 10 "a03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f8" 400 "5e7b462cc84ae0faaa5884bd5c4a5a5edf13db210599aeeb4d273c0f5f32967b7071ce2b4d490b9f08f6ce66a8405735c79197cd6773d1c5aeb2a38da1c102df07b05879c77198e5aafa7feed25d4137e86b3d98d9edd9547a460f1615b10000ee9570fbffedd44170477b37500a0a1cb3f94b6361f10f8a68c4075fbc17542d7174b3d95e12ddb8aea5d6b6c53c1df6c8f60010cd2e69902ba5e89e86747569463a23254730fc8d2aabf39648a505df9dcce461443b181ef3eda46074070000550836db2c97820971db6b1421e348d946ed4d3f255295abea46556615e3123de33ec56f784f70302901a4bc10c79c6a8b1e32477aeff9fba75876592981b678fc5a2703ac0b3055e567a6cb1ebab578fc4f9121fd968680250696cb85790000078fcfb60bdfc79aa1e377cb120480538e0236156f23129a88824ca5a1d77e371e5e98a16e6f32087c91aa02a4f5e00e412e515c3b678f6535141203c6886c637b626a2ada4062d037503359a680979091c68941a307db6e4ed8bc49d21b00002f0e6f88fb69309873fdefb015569e5511fb5399295204876543d065d177bf36ab79183a7c5e504b50691bc5b4ed0293324cfe2555d3fc8e39485822a90a91afcd4ef79ec3aefbd4cbe25cbccd802d8334ce1dce238c3f7505330a14615500001f89fbe1922ab3aa31a28fd29e19673714a7e48050dee59859d68345bb7bee7d5e888d8b798a58d7c650f9138304c05a92b668294c6114185ccb2c67ce0bbbb7e1dcbb6d76f5cacd7c9732a33b21d69bd7a28c9cca68b5735d50413862bc0100308bb0dd0bd53f3d1134966702dd3c7cc8b58b270a6996a646493250b0d5f3978d0c971f8fa7a0c958f3efe2fa5269244973fafb701c2eb66dd25901f93d677ab6c538c1ed11f115e52d3f2c7087ea40c3e8cd089376baa38842e9429b5f0000d19a8d874d791f952f13d3c8ecd92e44009c09815e5ae6a8e5def7ea52fe3de4accfb5ba2aa401fbcec14b069cd0dc0f66ab025b45ef9831a26acf58673db7487043654e7980fcb2b6c1bd7593a4dfff810436f653e309121c7ccf2df70b010000732254ec6df184be360cd9ed383ed7c8c236d7761cfc0ce4e7f0cac5a06f4edab9cfc75a7dc1449c0e18ed9564c974c2e1b6847c637f74e5d391cbc80fc6e672ffd66b5ce4fb73bda8359ab8a0ea1e855df1e07d82f93c935c7e1a9a55c5000065efdbb7c3e82291a482b2f24cbd46f4dd02c370cf6dcfe8fb3c00b8b004b5ad51369b1f1b134a824d1f16d72ca6a27ba2d6190150329139cf2c6d9e5a14722f8d39b96b882c1f60a7b230e929819e2abe1cd9d7f3e8c726b1a94d20c8010100732c396eca6ffa1bf851cef449f2f087edd93e4f641b4bd93a482d9f129e675aedb688993d4e2cee824d2803301364ba10fbb66895927adb53bad8aefe8a1caab6f4ccb45883e414a1223ac7f90a89087cd752dfa0c7b3e19bbae000edd5000028d1d23c627d1252d2a2a20a246af2280f50e3fde667873aadd9893ba6833118358398e7428e717128f764714a8d52b090c1f554f58e25ea815338d7bc7326c949567e74f2f2ab3c88f5075fea75594608b8937c9059a42d712ffbd1bd980100000000000250c1a474689e375a309446e5cdd3a0c26cecdcff5c7b8cdc0728868983f1a35a49e3a1bae6f969c3d47356c08d3d169d2c0a2be908d82cd35f41a23d8c2924a9f790ab3a00d53061d440a176670d6a32de2ecd19cf8a9774729c09a6ea4d0100d8838bf55d95521291da12294b302c66042eda0dc2acc79360a1fdd8c9a366fa790c52bf926c2d96b5ba88a3a443487c5235f7c476f350c2101cfbe3bd0361dd291ebc5e42c097a158704b71006886a3662ca6db7d816b4ad12444835d89000000795ce2b34aef921ccb3d9b9695f5d3fe0a03743c955cfcf01f8a1815a7c8b03de85fe15201d4b4b6f401cb334a6988ea5bde8986a468c47c3c6a5ae96a3160ff15e06699ea82bd40c0d5547fe1be77af7817861bbfcca3f4232f05a9cec800006c216565cee4d57b32d2d70bb3cb8d4a967c0eb5d7137b2ec58466f3d4d3b5375e4baa823bcc29c6ad877d9708cd5dc1c31fa3883a80710431110c4aa22e97b67fa639f54e86cfab87187011270139df7873bed12f6fb8cd9ab48f389338010000" "0ed74aa4afda5ffb358ba4906d0a5429f9840805e1fcc9ea71fca62afb5e97d779d0d2d3dd1bd9a3563166eff9d12b9ab9b6f11baf5639606689c72e16e2577ee338229e015697c8d174782b4caa04af9902eca2f3c384c6f558cf685b11010084a8436bbb18f7ccd039c249afa69f868c79939923ee3fc3ec437b998246db5b4907881702641ec6b41fdc6de644087713115fa2548121280fe8ac85fe31387aee6465767d8ba2189fab2ee8bf702beda6af018b4d68f890176bf2ab599e000000" "b1acb36cdc03885d0f8ebeff5f1b7c21e3719f875a733ed58170621852d45dcd51b96d38e2dbb2002b43c44742d02d1c348280112fa2fe282f01a6a2615d0d33d19b03a0f5337c8ef41c2d84e2db25f56eb88bcf2ed76a094801d2fc83160100"
{
  "txid": "8ae0b7db6c1ff877ce8f89482f70e64851a04fccb1312eee94fbba8140e29a82",
  "scid": "d40188abb42d6ba6302464a847ace23e52b0c7edcf433cd17c10ca008b5e8783"
}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Dans mon cas, la sidechain a été créée dans la transaction <code>8ae0b7db6c1ff877ce8f89482f70e64851a04fccb1312eee94fbba8140e29a82</code> et son identifiant est <code>d40188abb42d6ba6302464a847ace23e52b0c7edcf433cd17c10ca008b5e8783</code></p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Je peux consulter les détails de la transaction faisant apparaître ces informations, ainsi que les données de création et le montant initial envoyé (400) et l'adresse publique sur la sidechain ("sc address": "a03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f8") .</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ zen-cli -regtest gettransaction "8ae0b7db6c1ff877ce8f89482f70e612eee94fbba8140e29a82"
{
  "amount": -400.00000000,
  "fee": -0.00007127,
  "confirmations": 0,
  "txid": "8ae0b7db6c1ff877ce8f89482f70e64851a04fccb1312eee94fbba8140e29a82",
  "walletconflicts": &#91;
  ],
  "time": 1595456982,
  "timereceived": 1595456982,
  "vsc_ccout": &#91;
    {
      "scid": "d40188abb42d6ba6302464a847ace23e52b0c7edcf433cd17c10ca008b5e8783"
      "n": 0,
      "withdrawal epoch length": 10,
      "value": 400.00000000,
      "address": "a03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6
      "wCertVk": "5e7b462cc84ae0faaa5884bd5c4a5a5edf13db210599aeeb4d273c0f5f329690b9f08f6ce66a8405735c79197cd6773d1c5aeb2a38da1c102df07b05879c77198e5aafa7feed25d9edd9547a460f1615b10000ee9570fbffedd44170477b37500a0a1cb3f94b6361f10f8a68c4075f3d95e12ddb8aea5d6b6c53c1df6c8f60010cd2e69902ba5e89e86747569463a23254730fc8d2aabfcce461443b181ef3eda46074070000550836db2c97820971db6b1421e348d946ed4d3f255295abeade33ec56f784f70302901a4bc10c79c6a8b1e32477aeff9fba75876592981b678fc5a2703ac0b305b578fc4f9121fd968680250696cb85790000078fcfb60bdfc79aa1e377cb120480538e0236156f231d77e371e5e98a16e6f32087c91aa02a4f5e00e412e515c3b678f6535141203c6886c637b626a2ad59a680979091c68941a307db6e4ed8bc49d21b00002f0e6f88fb69309873fdefb015569e5511fb533d065d177bf36ab79183a7c5e504b50691bc5b4ed0293324cfe2555d3fc8e39485822a90a91afcd4cbe25cbccd802d8334ce1dce238c3f7505330a14615500001f89fbe1922ab3aa31a28fd29e19673759859d68345bb7bee7d5e888d8b798a58d7c650f9138304c05a92b668294c6114185ccb2c67ce0bbf5cacd7c9732a33b21d69bd7a28c9cca68b5735d50413862bc0100308bb0dd0bd53f3d113496670270a6996a646493250b0d5f3978d0c971f8fa7a0c958f3efe2fa5269244973fafb701c2eb66dd259038c1ed11f115e52d3f2c7087ea40c3e8cd089376baa38842e9429b5f0000d19a8d874d791f952f1309c09815e5ae6a8e5def7ea52fe3de4accfb5ba2aa401fbcec14b069cd0dc0f66ab025b45ef9831a487043654e7980fcb2b6c1bd7593a4dfff810436f653e309121c7ccf2df70b010000732254ec6df183ed7c8c236d7761cfc0ce4e7f0cac5a06f4edab9cfc75a7dc1449c0e18ed9564c974c2e1b6847c6c80fc6e672ffd66b5ce4fb73bda8359ab8a0ea1e855df1e07d82f93c935c7e1a9a55c5000065efdb2b2f24cbd46f4dd02c370cf6dcfe8fb3c00b8b004b5ad51369b1f1b134a824d1f16d72ca6a27ba2dcf2c6d9e5a14722f8d39b96b882c1f60a7b230e929819e2abe1cd9d7f3e8c726b1a94d20c8010100a1bf851cef449f2f087edd93e4f641b4bd93a482d9f129e675aedb688993d4e2cee824d280330136927adb53bad8aefe8a1caab6f4ccb45883e414a1223ac7f90a89087cd752dfa0c7b3e19bbae000edc627d1252d2a2a20a246af2280f50e3fde667873aadd9893ba6833118358398e7428e717128f7647f554f58e25ea815338d7bc7326c949567e74f2f2ab3c88f5075fea75594608b8937c9059a42d712f00000000250c1a474689e375a309446e5cdd3a0c26cecdcff5c7b8cdc0728868983f1a35a49e3a1b56c08d3d169d2c0a2be908d82cd35f41a23d8c2924a9f790ab3a00d53061d440a176670d6a32de2e29c09a6ea4d0100d8838bf55d95521291da12294b302c66042eda0dc2acc79360a1fdd8c9a366fa796b5ba88a3a443487c5235f7c476f350c2101cfbe3bd0361dd291ebc5e42c097a158704b7100688616b4ad12444835d89000000795ce2b34aef921ccb3d9b9695f5d3fe0a03743c955cfcf01f8a1815a5201d4b4b6f401cb334a6988ea5bde8986a468c47c3c6a5ae96a3160ff15e06699ea82bd40c0d5547861bbfcca3f4232f05a9cec800006c216565cee4d57b32d2d70bb3cb8d4a967c0eb5d7137b2ec585e4baa823bcc29c6ad877d9708cd5dc1c31fa3883a80710431110c4aa22e97b67fa639f54e86cfab9df7873bed12f6fb8cd9ab48f389338010000",
      "customData": "0ed74aa4afda5ffb358ba4906d0a5429f9840805e1fcc9ea71fca62afb5dd1bd9a3563166eff9d12b9ab9b6f11baf5639606689c72e16e2577ee338229e015697c8d174782bca2f3c384c6f558cf685b11010084a8436bbb18f7ccd039c249afa69f868c79939923ee3fc3ec43707881702641ec6b41fdc6de644087713115fa2548121280fe8ac85fe31387aee6465767d8ba2189fda6af018b4d68f890176bf2ab599e000000",
      "constant": "b1acb36cdc03885d0f8ebeff5f1b7c21e3719f875a733ed58170621852d45dbb2002b43c44742d02d1c348280112fa2fe282f01a6a2615d0d33d19b03a0f5337c8ef41c2d84e2f2ed76a094801d2fc83160100"
    }
  ],
  "vft_ccout": &#91;
  ],
  "vjoinsplit": &#91;
  ],
  "details": &#91;
    {
      "sc address": "a03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a
      "category": "crosschain",
      "amount": -400.00000000,
      "fee": -0.00007127,
      "size": 7119
    }
  ],
  "hex": "fcffffff23e4b505e44c9e165b19f24ba0bea2b45223bb12a1bebd0ea26fe14537e488b48304502210087323da18cca896766523e2ea81df3431cf0f64cb1cb8587643e073bfdfaccb8022d64e98b9293f060f140495fa01d79a7a6646bfc7143cf2a4be3d012103cbe54819c6acf93d0fe91700529aa132edef81a51a1322e3e7ffeffffff2c91d0d2bc668f1152b9ecc2fc959bb3c0606ee60a506c06c1f000000006a47304402205cfca6dd09911465c83b9830a5bab662ae6257b4fa1273accc4822036c5e07f94551d38a21040eb7353a9eb64630843a8ac95179bdba4c89de24146012103cbe54811719b1b510922a000529aa132edef81a51a1322e3e7ffeffffffad0381e205ee62bb7e0629d0cb1469c3df9b52cd0ae4081af34000000006a473044022012d97bc5f20fa86b1eaae3337d9dba989a44fec00f331571017022030051cc662dac5d9f5db80957a9e34351b3d6729db0514ef6714e21d74d945819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff7b2525c7f6f9bd66e8b9a4526e8028da33e3187e43c05ad5167000000006a473044022031a9aedaba61580a23ead3280440a65eb9d14be11a79c3fbf0b022000a6fdcb628eddecf23a874784dfd87d606af78d21fb2e5df7b012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffefffff1cbfaff7dac504747c703971576e3113abaeb02b2cb407fab5fe000000006b483045022100943af86f94be243415d7ae7b66a4ae29736dd233e1e5ed4e36d0220578dc1bdeabaf2e3e739dfe0c189366f9eabee606ace407c0a4012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a5ffffff584f7a61c0c934e0bceb113eae1542435315d321d9f84765c6b05f0cec265ba2000000006aa44d80d6987a92ee25079cc307a19392f12b479ac7e4ce76539b112b687c702203de745c7fa3d4eb5ee921c9bd578a3ca5d1387f50e583f99008012103cbe54819c6acf93d0fe91719b1b510922a0005a51a1322e3e7ffefffffffb0277bfedf3fc59301862ba85577ff37eb81383be0b2ac7e8613deb3066a473044022003074e8767e92745925a3a69a3380862b53691c57369c0f6e6578d8e7bfc1e5902201fb4772356880486f3384f8a2e3c55aeb6b748f38f948d617ac012103cbe54819c6acf93d0fe91710529aa132edef81a51a1322e3e7ffeffffff484bf9b606d7495790b68ae1ac5434362160dfb9c1b0d2a7e0a000000006b483045022100e2ca3664593ff35fe18bf69754a67a8889c8c0cec7362735a160220072e2dedc53bfbf7c670eb5ed26233c8dc3304e90d160b3203aff6e9148a9dfc012103cbe54891719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff5ee08e20a49f5aff8fbb52767ba7154b1bf8c5cd87fea4586f3000000006a473044022058478766245fc41efb02a683026c94d6a1989d7e85977d2299d02200383c0223718dca0a65957b1eeb3639307b1891aef0a45f9736b33c7d062c4819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff6d9d5ca8afe4f0e10d66665738172b9c5330c2a6d7236ed627f000000006b483045022100f4b1fb23a149772a818be63ca77265a739efe00e9df6d3b3ec8022033d512392d2aade591730d60e82707fe34886da1159efd595a1012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffefb1919efbdb36bbad2a99764a02960fca9b7dc294ee132dd7005a56ec000000006a47304402205c64e8976e47a1762d0701a3ff8e85c35e4f191eee5b06b4d9b022011a54d504e3f219dfdf73f1de7b91b73a667d97fb81c9477824012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81aeffffff0a59dff0484d5ba8aae87d0f86c6e96f58d6457d0d8f7ac7c6b5b72021dc9841000000006a7a50f069ac6563fd94c570d9b89e6e1d0bef4aef6e6a74a4c2d831940b42544022032576272576e1b0dcf3e0b64916cab3c87870539087305d7302012103cbe54819c6acf93d0fe91719b1b510922a0f81a51a1322e3e7ffeffffffea8ebbc93abbce09c2b4df4bda9545b4ea63f9d43168534b859017d50006b4830450221009cc1cd6fbd39854c1ce14c6d8f1e49ee404f0ffe9e5ff19580ddd454ca3b92b04ba369823c42606d5c2cfcf478eae1eb3b85812f88c8144f69f6309012103cbe54819c6acf93d0f22a000529aa132edef81a51a1322e3e7ffeffffffb6c346a10dc78ec468744019947793b16f9dc0af646171db728000000006b483045022100fd18a9c2c4b829cf507918c0ffdffaf62e488d3657d342e7d2b02206d69470f063dd3dda1ee28327326ee9cd94044542b3663f711db300c87962f5c012103c3d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffffbc98761352464c9d74f6302702ffe3ccf24324a2a08042123e9000000006a473044022035ae8a975df97dca58fe157d613a277677f776889b347448b7a02205d32b2d6ef40c34a6ae8033dbea91512834a3c9b9fa03de88ce1d0703cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffffdf63ecb0ae062790b543c913b7e4b72c81d0e00332e5110996b000000006a47304402202062b0b941c5039eddc888ef17fcf09e691aa6eae03ef7b9aff0220027e045a53ff81a411ee7647d014995cb3d04f52693d42d68a1012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7f4e190a3d65ad63b2de0aa3cfd81c164fa7510fd76d539ce8e191ed36481000000006a473044022056a9411044bab081e1286833f2145e5d6ce4dbeb7830646af00022024e1e7e049fea1aba8d2bb56006b72ee8f8d032cb13fb7dcc3d012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef7ffeffffffdd22086ed25014be004fe0b4156c4d30df3e9be3a3297fd524a9b194b8307f9e000000100ee95a6c13dfb77260c176fa9f67e52cc039a5858f33de334fac0afa3cacda5c0022021edbb5fde20cf7386f821ce74e412b9298ff71c936389690f7012103cbe54819c6acf93d0fe91719b1b51092edef81a51a1322e3e7ffeffffff950e17966063ad4db305b0d24e2a1f5057448d7f98ce9d1a210e10000006a473044022040a4976a61e6887201a3d0e760a4ff5dcbe29fe5e6ac12e09c2e12046bd092470aee6999cadb8d83c1496fe30e5fa636eda04b302e439f6f291fbad012103cbe54819c6acf93d0922a000529aa132edef81a51a1322e3e7ffeffffff4eb88bf49334ff8a7fa86cbdad77a58cc7904e91da78edbb963000000006a473044022018929f0bb75922b9d28ded2db9a8210092e6bdf3624b4199bfd022055816afbe3a31710fd829e1faa3892a5c9d30688cd6fe58ea52a8be0bd088e97012103cbd0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff0a51f14d33b0d62a7a0d6f1e1e08dbc7f29a3d7479c8b9de2b1000000006a473044022055b1245435e7d6f971947b1d881c51d678c4664393ca0de24460220048c7532da898a38c4bdb054abfee0065c26460192d7eb21c78801504cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffefffffffd6d7d8c3a3e8f3da22cf673842425c3ea64d365503805ec0e0000000006a473044022020d47c6070b3ccb55a7dba48bdf118ba146001abbab773152090220447b0443fb08939cb528dbf5d336407c8b5153673359ac7c9fa012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffb286b3fd831cd289ef8617d8ce30e1405321164f08ffc2fd828cd5231c000000006b483045022100d557288fbe6d6e6def9fc414cb6beedb1155fc5611d2a2c15d602206836a3de3696a4aa5ff5657aa78aea2f6a860a9ea9a77e5ddbf012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edee7ffeffffff8a02b85b964ed9559fa66c85a7a57304f3dc8bd1c068017ac2f33d5e408f4fca00000200eba15d43c29ce35c1844b919516ef60967c2e55d32561249ddcb5a0c7393e4c02206ed051275b6e52f7e96ed8a195d0c3999825c98db8e029618b1012103cbe54819c6acf93d0fe91719b1b510922def81a51a1322e3e7ffeffffffbba90f2a4dc7a56c943958aaaea20dcbbaef0466ec8ea43958114e000006a47304402206670717c4fc41e76f294ed101a1aa21e623e9f019ee5a88a509f12289a6aa76ac3373fe88b7321d2a18813478bbadae934f6b4132da15d907d56d67012103cbe54819c6acf93d0f22a000529aa132edef81a51a1322e3e7ffeffffffadb7d71bd4e1c603212086492ce504063d45fa67edeefd4526b000000006a473044022011a753521a0ebd65879eb15f011043bfd47afd9b3c944c4b16202203da397e749a71917e0d8150ad424c593783bd1a58fafa8570de3885ca75f1e66012103cbe0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffffd40224ee0e1d08618350d5d05bfe261137f5b7b25e6f567f395000000006b483045022100b156d95f064e51fc62e475230cce54670c1904c37c0681fd7200220765fac6554ec2e8c27c04c1769fbd762bf1be966fa3b95cb9daca0d33cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff35d57f8a43998b1f93fc841f3b5b667bfc1740df804c393d4b2000000006a473044022059855a3f0da72db44e4a020ad93ec7abcefc1f0592c1c87b5d3022052b31b3dca159c4866a9d7050d520721ba722c2b90927fa9720012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7fbbd618b93e537860d1147ebeafbfcb6dda64f439767f54cc4ae3ea62002000000006b48304502210599b1ef52199e76f935a65f0c13acfdfd4854c1f755a44d3bd8f02200c229cd700ffa83ef4a2e6dab05076795a19669f22e6a3228bf012103cbe54819c6acf93d0fe91719b1b510922a000529aa132ed3e7ffeffffffec1f7379dfb659bd88d30b4ec38689f80ec96958cee3739fb6cc8698c8bfb3a10000220519f2809b45139d9dd352f2cfc162eed77ad82fc1eb32737acf8fc683bbf642602204b7b065e7d72679ee9a7df9531d29f53ebcc7921cb2aacbf2c3012103cbe54819c6acf93d0fe91719b1b51092edef81a51a1322e3e7ffeffffff773eb88ef08c444da4cad09eebb067ac1cbaeb58d409daed582780000006b483045022100934ea1c94e4c4d828671eeacea35f7ff1777df312797b5f3628dfb6cd53ae4ae193dd7d356fb3ce109a5d11cfce3ec551d585e3adcaf74fe29ae7e3012103cbe54819c6acf9310922a000529aa132edef81a51a1322e3e7ffefffffffbb3cb9aa0d3ccf8459b7ed52541a2b79b683135643e9c5e17c000000006b483045022100adbb54a8d02e6460a97da087ba7edd93668ed386eaeedf6176702202c69cfbb8243ce308aba359b061fd76d359632c4d08d90b76a9a490ef1c18a6a0121cf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff22f0cd121f8311a6cd4a153c43b4ca0ec776d2806f9f621d9cc000000006b483045022100d53b234828f9923ce03bdc6abb3fd54e533a9490567ca888f1d022076e3e3d854a7105c53bb3a51ccbf3fbcbd8abdf76b80a15060b012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffffcf746cae66b4b90dbbd4f1c3ed87607706df4d6f2bb3868bee3000000006a473044022051cc2c63862630204696106727d3343bd3ac505dd6649bfa9ed0220417d8f46c2dec0b6f90a79348f3b4fe290785ad5addc3f29a4c012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322599e4a03a6f6425c23afb5a31f530c9ea13d654dab45783a92bef04a707e6ba9000000006a4730446fde81f08b24749799ebb2c87f7d9cf3aa58f257d38212e6df4fbbb02203423866b582404c66afcaa9143940f55459d7bfe3e35fdb18e4012103cbe54819c6acf93d0fe91719b1b510922a000529aa1322e3e7ffeffffff0179badc01000000003c76a9148290fad678a7ee43e7b7782eae23c4c8ebc64b12c1fc1228967a611c7db30632098f0c641855180b5fe23793b72eea50d00b4010a00000000902f502f8db1e7f3cbba1f94c96da8dc3873da20b9b46260a063fb01291ee3ba0c10ed74aa4afda5ffb358f9840805e1fcc9ea71fca62afb5e97d779d0d2d3dd1bd9a3563166eff9d12b9ab9b6f11baf56396077ee338229e015697c8d174782b4caa04af9902eca2f3c384c6f558cf685b11010084a8436bbb18fa69f868c79939923ee3fc3ec437b998246db5b4907881702641ec6b41fdc6de644087713115fa2545fe31387aee6465767d8ba2189fab2ee8bf702beda6af018b4d68f890176bf2ab599e00000060b1a0f8ebeff5f1b7c21e3719f875a733ed58170621852d45dcd51b96d38e2dbb2002b43c44742d02d1ce282f01a6a2615d0d33d19b03a0f5337c8ef41c2d84e2db25f56eb88bcf2ed76a094801d2fc831604ae0faaa5884bd5c4a5a5edf13db210599aeeb4d273c0f5f32967b7071ce2b4d490b9f08f6ce66a8d6773d1c5aeb2a38da1c102df07b05879c77198e5aafa7feed25d4137e86b3d98d9edd9547a460f170fbffedd44170477b37500a0a1cb3f94b6361f10f8a68c4075fbc17542d7174b3d95e12ddb8aea58f60010cd2e69902ba5e89e86747569463a23254730fc8d2aabf39648a505df9dcce461443b181ef00550836db2c97820971db6b1421e348d946ed4d3f255295abea46556615e3123de33ec56f784f7079c6a8b1e32477aeff9fba75876592981b678fc5a2703ac0b3055e567a6cb1ebab578fc4f9121fd985790000078fcfb60bdfc79aa1e377cb120480538e0236156f23129a88824ca5a1d77e371e5e98a1a02a4f5e00e412e515c3b678f6535141203c6886c637b626a2ada4062d037503359a680979091c68d8bc49d21b00002f0e6f88fb69309873fdefb015569e5511fb5399295204876543d065d177bf36abb50691bc5b4ed0293324cfe2555d3fc8e39485822a90a91afcd4ef79ec3aefbd4cbe25cbccd802d83f7505330a14615500001f89fbe1922ab3aa31a28fd29e19673714a7e48050dee59859d68345bb7b98a58d7c650f9138304c05a92b668294c6114185ccb2c67ce0bbbb7e1dcbb6d76f5cacd7c9732a339cca68b5735d50413862bc0100308bb0dd0bd53f3d1134966702dd3c7cc8b58b270a6996a6464932c971f8fa7a0c958f3efe2fa5269244973fafb701c2eb66dd25901f93d677ab6c538c1ed11f115e52c3e8cd089376baa38842e9429b5f0000d19a8d874d791f952f13d3c8ecd92e44009c09815e5ae6a8de4accfb5ba2aa401fbcec14b069cd0dc0f66ab025b45ef9831a26acf58673db7487043654e7980fa4dfff810436f653e309121c7ccf2df70b010000732254ec6df184be360cd9ed383ed7c8c236d776ac5a06f4edab9cfc75a7dc1449c0e18ed9564c974c2e1b6847c637f74e5d391cbc80fc6e672ffd66359ab8a0ea1e855df1e07d82f93c935c7e1a9a55c5000065efdbb7c3e82291a482b2f24cbd46f4dd8fb3c00b8b004b5ad51369b1f1b134a824d1f16d72ca6a27ba2d6190150329139cf2c6d9e5a147221f60a7b230e929819e2abe1cd9d7f3e8c726b1a94d20c8010100732c396eca6ffa1bf851cef449f241b4bd93a482d9f129e675aedb688993d4e2cee824d2803301364ba10fbb66895927adb53bad8aefb45883e414a1223ac7f90a89087cd752dfa0c7b3e19bbae000edd5000028d1d23c627d1252d2a2a20e3fde667873aadd9893ba6833118358398e7428e717128f764714a8d52b090c1f554f58e25ea81549567e74f2f2ab3c88f5075fea75594608b8937c9059a42d712ffbd1bd980100000000000250c1a4446e5cdd3a0c26cecdcff5c7b8cdc0728868983f1a35a49e3a1bae6f969c3d47356c08d3d169d2c05f41a23d8c2924a9f790ab3a00d53061d440a176670d6a32de2ecd19cf8a9774729c09a6ea4d010021291da12294b302c66042eda0dc2acc79360a1fdd8c9a366fa790c52bf926c2d96b5ba88a3a4434f350c2101cfbe3bd0361dd291ebc5e42c097a158704b71006886a3662ca6db7d816b4ad12444835d2b34aef921ccb3d9b9695f5d3fe0a03743c955cfcf01f8a1815a7c8b03de85fe15201d4b4b6f401cde8986a468c47c3c6a5ae96a3160ff15e06699ea82bd40c0d5547fe1be77af7817861bbfcca3f42306c216565cee4d57b32d2d70bb3cb8d4a967c0eb5d7137b2ec58466f3d4d3b5375e4baa823bcc29c5dc1c31fa3883a80710431110c4aa22e97b67fa639f54e86cfab87187011270139df7873bed12f6f33801000000d2000000"
}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Bien, la transaction de création de sidechain est créée. Elle est pour l'instant dans le pool de transactions. Elle sera donc confirmée et ajoutée dans le prochain bloc.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>"Minons-le" donc !</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ zen-cli -regtest generate 1
&#91;
  "04852dabaace286308c2ff9a8ba4968d2e3cf17b7079594b8dff756eb0e0f7f6"
]
$ zen-cli -regtest getblockcount
221</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>J'obtiens en retour l'id du bloc qui vient d'être ajouté à la mainchain (il y avait 220 blocs, J'en ai ajouté 1). Ici, c'est <code>"04852dabaace286308c2ff9a8ba4968d2e3cf17b7079594b8dff756eb0e0f7f6"</code>.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Ce bloc doit donc contenir ma transaction de création de sidechain :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ zen-cli -regtest getblock "04852dabaace286308c2ff9a8ba4968d2e3cf17b7079594b8dff756eb0e0f7f6"
{
  "hash": "04852dabaace286308c2ff9a8ba4968d2e3cf17b7079594b8dff756eb0e0f7f6",
  "confirmations": 1,
  "size": 7484,
  "height": 221,
  "version": 3,
  "merkleroot": "744840339e167a453ab519a447d775e9ba6720c8289aff48b08448e02e662eab",
  "scTxsCommitment": "15d76826004b8d64607d1559d01c7b7efdd5c5e9872f6a39ec32f17cdd1162b2",
  "tx": &#91;
    "f218efd6527e2defbdfccbcb0903713ec62c0addeebf538a2a9c8341b2a20d0b",
    "8ae0b7db6c1ff877ce8f89482f70e64851a04fccb1312eee94fbba8140e29a82"
  ],
  "cert": &#91;
  ],
  "time": 1595457527,
  "nonce": "00004800fb7dc576c526fad14549e7dc074015424f1ab426840ffe84b4610043",
  "solution": "0896e4f621d95725df0e2057d007ac4b49d60b24bcdee4c7bf3db910ebaebb22b1852318",
  "bits": "200f0f03",
  "difficulty": 1.00001215949611,
  "chainwork": "0000000000000000000000000000000000000000000000000000000000000ebe",
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
  "previousblockhash": "05cca9315c93542fc809fd9e2a22d22f744151406c5cf01a5c97c4b2a10a7579"
}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Ok, tout est en place, je retrouve bien ma transaction <code>8ae0b7db6c1ff877ce8f89482f70e64851a04fccb1312eee94fbba8140e29a82</code> dans le tableau des transactions <code>tx</code> du bloc miné.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Je peux vérifier que la transaction a gagné une confirmation. Bref, la création semble ok.</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ zen-cli -regtest gettransaction "8ae0b7db6c1ff877ce8f89482f70e64851a04fccb1312eee94fbba8140e29a82" | grep confirmations
  "confirmations": 1,</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>D'une façon générale, on peut obtenir les infomations sur les sidechains grâce à la commande <code>zen-cli -regtest getscinfo</code>, à laquelle on peut optionnellement passer en paramètre un identifiant de sidechain. En l'absence de ce paramètre, je vais récupérer l'ensemble des sidechains :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ zen-cli -regtest getscinfo "d40188abb42d6ba6302464a847ace23e52b0c7edcf433cd17c10ca008b5e8783"
{
  "scid": "d40188abb42d6ba6302464a847ace23e52b0c7edcf433cd17c10ca008b5e8783",
  "balance": 0.00000000,
  "epoch": 0,
  "end epoch height": 230,
  "state": "ALIVE",
  "ceasing height": 233,
  "creating tx hash": "8ae0b7db6c1ff877ce8f89482f70e64851a04fccb1312eee94fbba8140e29a82",
  "created in block": "04852dabaace286308c2ff9a8ba4968d2e3cf17b7079594b8dff756eb0e0f7f6",
  "created at block height": 221,
  "last certificate epoch": -1,
  "last certificate hash": "0000000000000000000000000000000000000000000000000000000000000000",
  "withdrawalEpochLength": 10,
  "wCertVk": "5e7b462cc84ae0faaa5884bd5c4a5a5edf13db210599aeeb4d273c0f5f32967b7071ce2b4d490b9f08f6ce66a8405735c79197cd6773d1c5aeb2a38da1c102df07b05879c77198e5aafa7feed25d4137e86b3d98d9edd9547a460f1615b10000ee9570fbffedd44170477b37500a0a1cb3f94b6361f10f8a68c4075fbc17542d7174b3d95e12ddb8aea5d6b6c53c1df6c8f60010cd2e69902ba5e89e86747569463a23254730fc8d2aabf39648a505df9dcce461443b181ef3eda46074070000550836db2c97820971db6b1421e348d946ed4d3f255295abea46556615e3123de33ec56f784f70302901a4bc10c79c6a8b1e32477aeff9fba75876592981b678fc5a2703ac0b3055e567a6cb1ebab578fc4f9121fd968680250696cb85790000078fcfb60bdfc79aa1e377cb120480538e0236156f23129a88824ca5a1d77e371e5e98a16e6f32087c91aa02a4f5e00e412e515c3b678f6535141203c6886c637b626a2ada4062d037503359a680979091c68941a307db6e4ed8bc49d21b00002f0e6f88fb69309873fdefb015569e5511fb5399295204876543d065d177bf36ab79183a7c5e504b50691bc5b4ed0293324cfe2555d3fc8e39485822a90a91afcd4ef79ec3aefbd4cbe25cbccd802d8334ce1dce238c3f7505330a14615500001f89fbe1922ab3aa31a28fd29e19673714a7e48050dee59859d68345bb7bee7d5e888d8b798a58d7c650f9138304c05a92b668294c6114185ccb2c67ce0bbbb7e1dcbb6d76f5cacd7c9732a33b21d69bd7a28c9cca68b5735d50413862bc0100308bb0dd0bd53f3d1134966702dd3c7cc8b58b270a6996a646493250b0d5f3978d0c971f8fa7a0c958f3efe2fa5269244973fafb701c2eb66dd25901f93d677ab6c538c1ed11f115e52d3f2c7087ea40c3e8cd089376baa38842e9429b5f0000d19a8d874d791f952f13d3c8ecd92e44009c09815e5ae6a8e5def7ea52fe3de4accfb5ba2aa401fbcec14b069cd0dc0f66ab025b45ef9831a26acf58673db7487043654e7980fcb2b6c1bd7593a4dfff810436f653e309121c7ccf2df70b010000732254ec6df184be360cd9ed383ed7c8c236d7761cfc0ce4e7f0cac5a06f4edab9cfc75a7dc1449c0e18ed9564c974c2e1b6847c637f74e5d391cbc80fc6e672ffd66b5ce4fb73bda8359ab8a0ea1e855df1e07d82f93c935c7e1a9a55c5000065efdbb7c3e82291a482b2f24cbd46f4dd02c370cf6dcfe8fb3c00b8b004b5ad51369b1f1b134a824d1f16d72ca6a27ba2d6190150329139cf2c6d9e5a14722f8d39b96b882c1f60a7b230e929819e2abe1cd9d7f3e8c726b1a94d20c8010100732c396eca6ffa1bf851cef449f2f087edd93e4f641b4bd93a482d9f129e675aedb688993d4e2cee824d2803301364ba10fbb66895927adb53bad8aefe8a1caab6f4ccb45883e414a1223ac7f90a89087cd752dfa0c7b3e19bbae000edd5000028d1d23c627d1252d2a2a20a246af2280f50e3fde667873aadd9893ba6833118358398e7428e717128f764714a8d52b090c1f554f58e25ea815338d7bc7326c949567e74f2f2ab3c88f5075fea75594608b8937c9059a42d712ffbd1bd980100000000000250c1a474689e375a309446e5cdd3a0c26cecdcff5c7b8cdc0728868983f1a35a49e3a1bae6f969c3d47356c08d3d169d2c0a2be908d82cd35f41a23d8c2924a9f790ab3a00d53061d440a176670d6a32de2ecd19cf8a9774729c09a6ea4d0100d8838bf55d95521291da12294b302c66042eda0dc2acc79360a1fdd8c9a366fa790c52bf926c2d96b5ba88a3a443487c5235f7c476f350c2101cfbe3bd0361dd291ebc5e42c097a158704b71006886a3662ca6db7d816b4ad12444835d89000000795ce2b34aef921ccb3d9b9695f5d3fe0a03743c955cfcf01f8a1815a7c8b03de85fe15201d4b4b6f401cb334a6988ea5bde8986a468c47c3c6a5ae96a3160ff15e06699ea82bd40c0d5547fe1be77af7817861bbfcca3f4232f05a9cec800006c216565cee4d57b32d2d70bb3cb8d4a967c0eb5d7137b2ec58466f3d4d3b5375e4baa823bcc29c6ad877d9708cd5dc1c31fa3883a80710431110c4aa22e97b67fa639f54e86cfab87187011270139df7873bed12f6fb8cd9ab48f389338010000",
  "customData": "0ed74aa4afda5ffb358ba4906d0a5429f9840805e1fcc9ea71fca62afb5e97d779d0d2d3dd1bd9a3563166eff9d12b9ab9b6f11baf5639606689c72e16e2577ee338229e015697c8d174782b4caa04af9902eca2f3c384c6f558cf685b11010084a8436bbb18f7ccd039c249afa69f868c79939923ee3fc3ec437b998246db5b4907881702641ec6b41fdc6de644087713115fa2548121280fe8ac85fe31387aee6465767d8ba2189fab2ee8bf702beda6af018b4d68f890176bf2ab599e000000",
  "constant": "b1acb36cdc03885d0f8ebeff5f1b7c21e3719f875a733ed58170621852d45dcd51b96d38e2dbb2002b43c44742d02d1c348280112fa2fe282f01a6a2615d0d33d19b03a0f5337c8ef41c2d84e2db25f56eb88bcf2ed76a094801d2fc83160100",
  "immature amounts": &#91;
    {
      "maturityHeight": 224,
      "amount": 400.00000000
    }
  ]
}</code></pre>
<!-- /wp:code -->

<!-- wp:heading -->
<h2>La Génèse</h2>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>A ce stade, la <strong>mainchain </strong>contient la déclaration de la <strong>sidechain</strong>.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Je dois à présent lui donner vie grâce à l'appli SimpleApp qui sera un node de cette sidechain.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Je vais générer un fichier de configuration avec les données nécessaires pour que SimpleApp sache faire tourner la sidechain.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Pour configurer mon noeud de la sidechain, j'ai besoin de collecter et compiler plusieurs informations.</p>
<!-- /wp:paragraph -->

<!-- wp:list {"ordered":true} -->
<ol><li>La SideChain Genesis Info</li></ol>
<!-- /wp:list -->

<!-- wp:paragraph -->
<p>Je l'obtiens grâce à la commande <code>getscgenesisinfo</code> de <strong>zen-cli</strong> en lui passant en paramètre le <code>scid</code>(SideChain ID) récupéré lors de la création de la sidechain avec <code>sc_create</code>.</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ zen-cli -regtest getscgenesisinfo "d40188abb42d6ba6302464a847ace23e52b0c7edcf433cd17c10ca008b5e8783"</code></pre>
<!-- /wp:code -->

<!-- wp:list {"ordered":true,"start":2} -->
<ol start="2"><li>La partie <code>secret</code> obtenue en réponse à l'appel generateKey lors du bootstrap.</li><li>La partie <code>vrfSecret</code> obtenue en réponse à l'appel generateVrfKey lors du bootstrap.</li></ol>
<!-- /wp:list -->

<!-- wp:paragraph -->
<p>On va utiliser à nouveau l'outil de bootstrap qui fournit la commande <code>genesisinfo</code> qui va nous fournir les informations nécessaires pour la configuration de la sidechain.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>L'utilisation est la suivante :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>genesisinfo  {"info": "Données 1", "secret": "Données 2", "vrfSecret": "Données 3"} </code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Pour une raison inconnue l'outil de Bootstrap ne voulait pas exécuter la commande <code>genesisinfo</code>. J'ai donc utilisé l'autre forme d'appel de l'outil, non-interactive, en tapant tout sur une seule ligne. <strong>Attention !</strong> Dans ce cas, il faut protéger les guillemets avec des antislashs :</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Après s'être placé dans le répertoire Sidechains-SDK, la commande à taper directement dans bash est donc :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ java -jar tools/sctool/target/Sidechains-SDK-ScBootstrappingTools-0.2.1.jar genesisinfo  {\"info\": \"Données 1\", \"secret\": \"Données 2\", \"vrfSecret\": \"Données 3\"}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Une fois les données remplacées, attention à ne pas se tromper :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ java -jar tools/sctool/target/Sidechains-SDK-ScBootstrappingTools-0.2.1.jar genesisinfo  {\"info\":\"0283875e8b00ca107cd13c43cfedc7b0523ee2ac47a8642430a66b2db4ab8801d41cd3bd185f030f0f20d3bd185f030f0f20d3bd185f030f0f20d2bd185f030f0f20d2bd185f030f0f20d2bd185f030f0f20d2bd185f030f0f20d2bd185f030f0f20d2bd185f030f0f20d1bd185f030f0f20d1bd185f030f0f20d1bd185f030f0f20d1bd185f030f0f20d1bd185f030f0f20d1bd185f030f0f20d0bd185f030f0f20d0bd185f040f0f20d0bd185f040f0f20d0bd185f040f0f20d0bd185f040f0f20d0bd185f040f0f20cfbd185f040f0f20cfbd185f040f0f20cfbd185f040f0f20cfbd185f040f0f20cfbd185f040f0f20cfbd185f040f0f20cebd185f040f0f20dd0000000300000079750aa1b2c4975c1af05c6c405141742fd2222a9efd09c82f54935c31a9cc05ab2e662ee04884b048ff9a28c82067bae975d747a419b53a457a169e33404874b26211dd7cf132ec396a2f87e9c5d5fd7e7b1cd059157d60648d4b002668d715f7bf185f030f0f20430061b484fe0f8426b41a4f42154007dce74945d1fa26c576c57dfb00480000240896e4f621d95725df0e2057d007ac4b49d60b24bcdee4c7bf3db910ebaebb22b18523180201000000010000000000000000000000000000000000000000000000000000000000000000ffffffff0502dd000101ffffffff045733b42c000000001976a9142954edbc7caadad684590bebfe6045ff2b39c8fd88ac80b2e60e0000000017a914b6863b182a52745bf6d5fb190139a2aa876c08f587405973070000000017a914b6863b182a52745bf6d5fb190139a2aa876c08f587405973070000000017a914b6863b182a52745bf6d5fb190139a2aa876c08f58700000000fcffffff23e4b505e44c9e165b19f24ba0bea2b45223bb12a1bebd0ea26fe14537e4880054000000006b48304502210087323da18cca896766523e2ea81df3431cf0f64cb1cb8587643e073bfdfaccb802203a446e8105ebd64e98b9293f060f140495fa01d79a7a6646bfc7143cf2a4be3d012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff2c91d0d2bc668f1152b9ecc2fc959bb3c0606ee60a55d280ea6e423706c06c1f000000006a47304402205cfca6dd09911465c83b9830a5bab662ae6257b4fa1273accc489ab1e1c285fa022036c5e07f94551d38a21040eb7353a9eb64630843a8ac95179bdba4c89de24146012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffffad0381e205ee62bb7e0629d0cb14f1183062d742569c3df9b52cd0ae4081af34000000006a473044022012d97bc5f20fa86b1eaae3337d9dba989a44f6e7f711f36540ec00f331571017022030051cc662dac5d9f5db80957a9e34351b3d6729db0514ef6714e21d74d94531012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff7b2525c7f6f9b98469ff26d78ad66e8b9a4526e8028da33e3187e43c05ad5167000000006a473044022031a9aedaba61580a23ead331357cb0efec9280440a65eb9d14be11a79c3fbf0b022000a6fdcb628eddecf23a874784dfd87d606af78d21fb2e5f8340b24e30a1df7b012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff7745c2d3f43f1cbfaff7dac504747c703971576e3113abaeb02b2cb407fab5fe000000006b483045022100943aff312dad43040f86f94be243415d7ae7b66a4ae29736dd233e1e5ed4e36d0220578dc1bdeabaf2e3e739dfe0c18936560947c1b79976f9eabee606ace407c0a4012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff584f7a61c0c934e0bceb113eae1542435315d321d9f84765c6b05f0cec265ba2000000006a4730440220244a44d80d6987a92ee25079cc307a19392f12b479ac7e4ce76539b112b687c702203de745c7fa3d4eb732d18b85fae45ee921c9bd578a3ca5d1387f50e583f99008012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffefffffffb0277bfedf3fc59301862ba85577ff37eb81383be0b2ac7e8613deb306586aa000000006a473044022003074e8767e92745925a3a69a3380862b53691c57369c0f6e6578d8e7bfc1e590220098adad09259f1fb4772356880486f3384f8a2e3c55aeb6b748f38f948d617ac012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff484bf9b606d7495790b68ae1ac5434362160dfb9c1b05d770c87330b7d2a7e0a000000006b483045022100e2ca3664593ff35fe18bf69754a67a8889c8c0cec7362735a1629eca72f8ddae0220072e2dedc53bfbf7c670eb5ed26233c8dc3304e90d160b3203aff6e9148a9dfc012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff5ee08e20a49f5aff8fbb52767bae566901203e647154b1bf8c5cd87fea4586f3000000006a473044022058478766245fc41efb02a683026c94d6a1981fa2c4b686ea79d7e85977d2299d02200383c0223718dca0a65957b1eeb3639307b1891aef0a45f9736b33c7d062cd21012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff6d9d5ca8afe4ca075917b4e4af0e10d66665738172b9c5330c2a6d7236ed627f000000006b483045022100f4b1fb23a149772a818e15ef882f9781be63ca77265a739efe00e9df6d3b3ec8022033d512392d2aade591730d60e82707fe34886da1159ef21d848cb75d8fd595a1012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff1e59c3c9b1919efbdb36bbad2a99764a02960fca9b7dc294ee132dd7005a56ec000000006a47304402205c64972d5bd614eb9e8976e47a1762d0701a3ff8e85c35e4f191eee5b06b4d9b022011a54d504e3f219dfdf73f1de7b91844d8583b1310b73a667d97fb81c9477824012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff0a59dff0484d5ba8aae87d0f86c6e96f58d6457d0d8f7ac7c6b5b72021dc9841000000006b483045022100a7a50f069ac6563fd94c570d9b89e6e1d0bef4aef6e6a74a4c2d831940b42544022032576272576eae8861259b3641b0dcf3e0b64916cab3c87870539087305d7302012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffffea8ebbc93abbce09c2b4df4bda9545b4ea63f9d43168534b859017d5d833f998000000006b4830450221009cc1cd6fbd39854c1ce14c6d8f1e49ee404f0ffe9e5ff19580ddd454ca3b92b9022016428a6b04ba369823c42606d5c2cfcf478eae1eb3b85812f88c8144f69f6309012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffffb6c346a10dc78ec468744019947793b16f9dc0a96ae8c81d9346f646171db728000000006b483045022100fd18a9c2c4b829cf507918c0ffdffaf62e488d3657d34274e20afb5fa36e7d2b02206d69470f063dd3dda1ee28327326ee9cd94044542b3663f711db300c87962f5c012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffffbc98761352464c9d74f630da1117811c6c52702ffe3ccf24324a2a08042123e9000000006a473044022035ae8a975df97dca58fe157d613a277d3289388a5bbf677f776889b347448b7a02205d32b2d6ef40c34a6ae8033dbea91512834a3c9b9fa03de88ce1d0706e763d4b012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffffdf63ecb87f1a527654370ae062790b543c913b7e4b72c81d0e00332e5110996b000000006a47304402202062b0b941c5039e2b09db90a4090ddc888ef17fcf09e691aa6eae03ef7b9aff0220027e045a53ff81a411ee7647d014995cb3d04f526670f99fdd4cca93d42d68a1012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff2915d4e190a3d65ad63b2de0aa3cfd81c164fa7510fd76d539ce8e191ed36481000000006a47304402205eb363a834574c6a9411044bab081e1286833f2145e5d6ce4dbeb7830646af00022024e1e7e049fea1aba8d2bb5600f047b232c2cfb6b72ee8f8d032cb13fb7dcc3d012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffffdd22086ed25014be004fe0b4156c4d30df3e9be3a3297fd524a9b194b8307f9e000000006b483045022100ee95a6c13dfb77260c176fa9f67e52cc039a5858f33de334fac0afa3cacda5c0022021edbb5fd97e0b161d9432e20cf7386f821ce74e412b9298ff71c936389690f7012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff950e17966063ad4db305b0d24e2a1f5057448d7f98ce9d1a210e158da1313894000000006a473044022040a4976a61e6887201a3d0e760a4ff5dcbe29fe5e6ac12e09c2e12046bd09228022035554bb470aee6999cadb8d83c1496fe30e5fa636eda04b302e439f6f291fbad012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff4eb88bf49334ff8a7fa86cbdad77a58cc7904e4b50a27663cf591da78edbb963000000006a473044022018929f0bb75922b9d28ded2db9a8210092e6bdf3624b41927e2945f7052e9bfd022055816afbe3a31710fd829e1faa3892a5c9d30688cd6fe58ea52a8be0bd088e97012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff0a51f14d33b0d62a7a0d6f1b7874b8054b6ae1e08dbc7f29a3d7479c8b9de2b1000000006a473044022055b1245435e7d6f971947b1d881c51d61c16c9908931c78c4664393ca0de24460220048c7532da898a38c4bdb054abfee0065c26460192d7eb21c78801504eb70473012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffefffffffd6d7d8c34023b8147f9e3a3e8f3da22cf673842425c3ea64d365503805ec0e0000000006a473044022020d47c6070b3ccb55e2dc989dd22a5a7dba48bdf118ba146001abbab773152090220447b0443fb08939cb528dbf5d336407c8b515367330ce6946cf453359ac7c9fa012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff7777b0b286b3fd831cd289ef8617d8ce30e1405321164f08ffc2fd828cd5231c000000006b483045022100aa303a127a8efd557288fbe6d6e6def9fc414cb6beedb1155fc5611d2a2c15d602206836a3de3696a4aa5ff5657aa66eb37a7c000778aea2f6a860a9ea9a77e5ddbf012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff8a02b85b964ed9559fa66c85a7a57304f3dc8bd1c068017ac2f33d5e408f4fca000000006a47304402200eba15d43c29ce35c1844b919516ef60967c2e55d32561249ddcb5a0c7393e4c02206ed051275bee0d783ed589b6e52f7e96ed8a195d0c3999825c98db8e029618b1012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffffbba90f2a4dc7a56c943958aaaea20dcbbaef0466ec8ea43958114e7dd6e87ef7000000006a47304402206670717c4fc41e76f294ed101a1aa21e623e9f019ee5a88a509f12289a6aa761022039de0208ac3373fe88b7321d2a18813478bbadae934f6b4132da15d907d56d67012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffffadb7d71bd4e1c603212086492ce504063d45fa6d0c9b62eb55637edeefd4526b000000006a473044022011a753521a0ebd65879eb15f011043bfd47afd9b3c944c4bec1c1d27bfd5416202203da397e749a71917e0d8150ad424c593783bd1a58fafa8570de3885ca75f1e66012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffffd40224ee0e1d08618350d5d01c6ef863e4f975bfe261137f5b7b25e6f567f395000000006b483045022100b156d95f064e51fc62e475230cce546450f09d6a647c70c1904c37c0681fd7200220765fac6554ec2e8c27c04c1769fbd762bf1be966fa3b95cb9daca0d3d4b1b817012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff35d57f8e1f2905d01ad4a43998b1f93fc841f3b5b667bfc1740df804c393d4b2000000006a473044022059855a3f0da72db43f0d8ff1e30324e4a020ad93ec7abcefc1f0592c1c87b5d3022052b31b3dca159c4866a9d7050d520721ba722c2b93701f692b97930927fa9720012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffffdb5f4bbd618b93e537860d1147ebeafbfcb6dda64f439767f54cc4ae3ea62002000000006b483045022100b0df30ecaa78599b1ef52199e76f935a65f0c13acfdfd4854c1f755a44d3bd8f02200c229cd700ffa83ef4a2e6da537ec973862c8b05076795a19669f22e6a3228bf012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffffec1f7379dfb659bd88d30b4ec38689f80ec96958cee3739fb6cc8698c8bfb3a1000000006a4730440220519f2809b45139d9dd352f2cfc162eed77ad82fc1eb32737acf8fc683bbf642602204b7b065e709eb7e917915fd72679ee9a7df9531d29f53ebcc7921cb2aacbf2c3012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff773eb88ef08c444da4cad09eebb067ac1cbaeb58d409daed582786585b288065000000006b483045022100934ea1c94e4c4d828671eeacea35f7ff1777df312797b5f3628dfb6cd53a9c1802200ebe3e4ae193dd7d356fb3ce109a5d11cfce3ec551d585e3adcaf74fe29ae7e3012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffefffffffbb3cb9aa0d3ccf8459b7ed52541a2b79b68f636f7c29e32c3135643e9c5e17c000000006b483045022100adbb54a8d02e6460a97da087ba7edd93668ed386eaeb5c13644a14ddedf6176702202c69cfbb8243ce308aba359b061fd76d359632c4d08d90b76a9a490ef1c18a6a012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff22f0cd121f8311a6cd479818b50f83a7a153c43b4ca0ec776d2806f9f621d9cc000000006b483045022100d53b234828f9923ce03bdc6abb17e576cb43e203fd54e533a9490567ca888f1d022076e3e3d854a7105c53bb3a51ccbf3fbcbd8abdf76b80a15060bb007f156d160c012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffffcf385d69aaa00ff746cae66b4b90dbbd4f1c3ed87607706df4d6f2bb3868bee3000000006a473044022051cc2c638628328b25bf9dd7630204696106727d3343bd3ac505dd6649bfa9ed0220417d8f46c2dec0b6f90a79348f3b4fe290783aa35ee5c8be65ad5addc3f29a4c012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff599e4a03a6f6425c23afb5a31f530c9ea13d654dab45783a92bef04a707e6ba9000000006a47304402204557fa5f46fde81f08b24749799ebb2c87f7d9cf3aa58f257d38212e6df4fbbb02203423866b582404c66afca5e2b7d96aa7a2a9143940f55459d7bfe3e35fdb18e4012103cbe54819c6acf93d0fe91719b1b510922a000529aa132edef81a51a1322e3e7ffeffffff0179badc01000000003c76a9148290fad678a7ee43e7b7782eae23c4c8ebc64b1a88ac20bb1acf2c1fc1228967a611c7db30632098f0c641855180b5fe23793b72eea50d00b4010a00000000902f5009000000f8d6a2f8db1e7f3cbba1f94c96da8dc3873da20b9b46260a063fb01291ee3ba0c10ed74aa4afda5ffb358ba4906d0a5429f9840805e1fcc9ea71fca62afb5e97d779d0d2d3dd1bd9a3563166eff9d12b9ab9b6f11baf5639606689c72e16e2577ee338229e015697c8d174782b4caa04af9902eca2f3c384c6f558cf685b11010084a8436bbb18f7ccd039c249afa69f868c79939923ee3fc3ec437b998246db5b4907881702641ec6b41fdc6de644087713115fa2548121280fe8ac85fe31387aee6465767d8ba2189fab2ee8bf702beda6af018b4d68f890176bf2ab599e00000060b1acb36cdc03885d0f8ebeff5f1b7c21e3719f875a733ed58170621852d45dcd51b96d38e2dbb2002b43c44742d02d1c348280112fa2fe282f01a6a2615d0d33d19b03a0f5337c8ef41c2d84e2db25f56eb88bcf2ed76a094801d2fc831601005e7b462cc84ae0faaa5884bd5c4a5a5edf13db210599aeeb4d273c0f5f32967b7071ce2b4d490b9f08f6ce66a8405735c79197cd6773d1c5aeb2a38da1c102df07b05879c77198e5aafa7feed25d4137e86b3d98d9edd9547a460f1615b10000ee9570fbffedd44170477b37500a0a1cb3f94b6361f10f8a68c4075fbc17542d7174b3d95e12ddb8aea5d6b6c53c1df6c8f60010cd2e69902ba5e89e86747569463a23254730fc8d2aabf39648a505df9dcce461443b181ef3eda46074070000550836db2c97820971db6b1421e348d946ed4d3f255295abea46556615e3123de33ec56f784f70302901a4bc10c79c6a8b1e32477aeff9fba75876592981b678fc5a2703ac0b3055e567a6cb1ebab578fc4f9121fd968680250696cb85790000078fcfb60bdfc79aa1e377cb120480538e0236156f23129a88824ca5a1d77e371e5e98a16e6f32087c91aa02a4f5e00e412e515c3b678f6535141203c6886c637b626a2ada4062d037503359a680979091c68941a307db6e4ed8bc49d21b00002f0e6f88fb69309873fdefb015569e5511fb5399295204876543d065d177bf36ab79183a7c5e504b50691bc5b4ed0293324cfe2555d3fc8e39485822a90a91afcd4ef79ec3aefbd4cbe25cbccd802d8334ce1dce238c3f7505330a14615500001f89fbe1922ab3aa31a28fd29e19673714a7e48050dee59859d68345bb7bee7d5e888d8b798a58d7c650f9138304c05a92b668294c6114185ccb2c67ce0bbbb7e1dcbb6d76f5cacd7c9732a33b21d69bd7a28c9cca68b5735d50413862bc0100308bb0dd0bd53f3d1134966702dd3c7cc8b58b270a6996a646493250b0d5f3978d0c971f8fa7a0c958f3efe2fa5269244973fafb701c2eb66dd25901f93d677ab6c538c1ed11f115e52d3f2c7087ea40c3e8cd089376baa38842e9429b5f0000d19a8d874d791f952f13d3c8ecd92e44009c09815e5ae6a8e5def7ea52fe3de4accfb5ba2aa401fbcec14b069cd0dc0f66ab025b45ef9831a26acf58673db7487043654e7980fcb2b6c1bd7593a4dfff810436f653e309121c7ccf2df70b010000732254ec6df184be360cd9ed383ed7c8c236d7761cfc0ce4e7f0cac5a06f4edab9cfc75a7dc1449c0e18ed9564c974c2e1b6847c637f74e5d391cbc80fc6e672ffd66b5ce4fb73bda8359ab8a0ea1e855df1e07d82f93c935c7e1a9a55c5000065efdbb7c3e82291a482b2f24cbd46f4dd02c370cf6dcfe8fb3c00b8b004b5ad51369b1f1b134a824d1f16d72ca6a27ba2d6190150329139cf2c6d9e5a14722f8d39b96b882c1f60a7b230e929819e2abe1cd9d7f3e8c726b1a94d20c8010100732c396eca6ffa1bf851cef449f2f087edd93e4f641b4bd93a482d9f129e675aedb688993d4e2cee824d2803301364ba10fbb66895927adb53bad8aefe8a1caab6f4ccb45883e414a1223ac7f90a89087cd752dfa0c7b3e19bbae000edd5000028d1d23c627d1252d2a2a20a246af2280f50e3fde667873aadd9893ba6833118358398e7428e717128f764714a8d52b090c1f554f58e25ea815338d7bc7326c949567e74f2f2ab3c88f5075fea75594608b8937c9059a42d712ffbd1bd980100000000000250c1a474689e375a309446e5cdd3a0c26cecdcff5c7b8cdc0728868983f1a35a49e3a1bae6f969c3d47356c08d3d169d2c0a2be908d82cd35f41a23d8c2924a9f790ab3a00d53061d440a176670d6a32de2ecd19cf8a9774729c09a6ea4d0100d8838bf55d95521291da12294b302c66042eda0dc2acc79360a1fdd8c9a366fa790c52bf926c2d96b5ba88a3a443487c5235f7c476f350c2101cfbe3bd0361dd291ebc5e42c097a158704b71006886a3662ca6db7d816b4ad12444835d89000000795ce2b34aef921ccb3d9b9695f5d3fe0a03743c955cfcf01f8a1815a7c8b03de85fe15201d4b4b6f401cb334a6988ea5bde8986a468c47c3c6a5ae96a3160ff15e06699ea82bd40c0d5547fe1be77af7817861bbfcca3f4232f05a9cec800006c216565cee4d57b32d2d70bb3cb8d4a967c0eb5d7137b2ec58466f3d4d3b5375e4baa823bcc29c6ad877d9708cd5dc1c31fa3883a80710431110c4aa22e97b67fa639f54e86cfab87187011270139df7873bed12f6fb8cd9ab48f38933801000000d200000000\", \"secret\": \"00531e40d4036d808d2101756debc7fd2365cd76da1176f5bf2ed09603d01f711ca03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f8\", \"vrfSecret\": \"0300000060b218f4c543504110af4dd02a72a764d5c1c59beb1e6dc3ea0dd85f86685eade90f567f9937b3a4fc36bb5adfe480ac381ccdbb91f11c269e2a41d71d829fc312cdef48bf75818447405bb86202a1d39f0503491207eab329bb77047805f800000ed74aa4afda5ffb358ba4906d0a5429f9840805e1fcc9ea71fca62afb5e97d779d0d2d3dd1bd9a3563166eff9d12b9ab9b6f11baf5639606689c72e16e2577ee338229e015697c8d174782b4caa04af9902eca2f3c384c6f558cf685b11010084a8436bbb18f7ccd039c249afa69f868c79939923ee3fc3ec437b998246db5b4907881702641ec6b41fdc6de644087713115fa2548121280fe8ac85fe31387aee6465767d8ba2189fab2ee8bf702beda6af018b4d68f890176bf2ab599e000000\"}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Cette commande retourne une structure à réutiliser plus tard. Je la mets de côté :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>{
    "scId":"d40188abb42d6ba6302464a847ace23e52b0c7edcf433cd17c10ca008b5e8783",
    "scGenesisBlockHex":"010000000000000000000000000000000000000000000000000000000000000000cca1c1f10ba204e933f41dc86ca654a03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f800000009502f9000a03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f80ed74aa4afda5ffb358ba4906d0a5429f9840805e1fcc9ea71fca62afb5e97d779d0d2d3dd1bd9a3563166eff9d12b9ab9b6f11baf5639606689c72e16e2577ee338229e015697c8d174782b4caa04af9902eca2f3c384c6f558cf685b11010084a8436bbb18f7ccd039c249afa69f868c79939923ee3fc3ec437b998246db5b4907881702641ec6b41fdc6de644087713115fa2548121280fe8ac85fe31387aee6465767d8ba2189fab2ee8bf702beda6af018b4d68f890176bf2ab599e0000000800000000dcc662d4348c35e93abd86d54e73d3f89de9011e217345c1576a535919c7625c216a0d2942dda7d257d38ea93197127756c1630ef63efc6d967ea02e620952d382d2a4c32f323a85c2bae120e2c1e5add29bbb5a1d0b2c9da18bbbd583d50000fc45b48a89b591177675dfedaec458c7a552556e968fccd9012bba7315807435187426124dcaf81a6f3b6503e78c0254328607a2538be94388708527a637234bdc9c4c26350bb3720d0c90dd0ea0cd5349e519b4e6b2e8585256b8688364010000508457c4bdc88baa74cedfe4e35f7937f14d3093f9202ac116f4c7f4fdfff7da657088e70371a6c474ddf3b0bc40475c75e6c4104d0344b32d1bd458a5185b063c6e27de5ef992bfc7f2852b80dc47c0e4200821b0a1ae27ebbff6cf8fc90000ef55c7566071fdb85b08027632f50b6f443fb64f83ce3aa83960770550855ff95e0f98af05ea16532e364c877b616ae5bc4f6fc3b40359fe79e7e20cd6fa701204ddba0ae615887d84cd26264b8824fe27e07e9ea0501493dd9588d19ba3000000000000000000000000000000000000000000000000000000000000000000005f10d71366010e474d9ed8c1f055396495b51d561bac632c4e353871614422ff00000000000000000000000000000000000000000000000000000000000000000080016f16a558e106b6d367cfe82497d7bc95955aa570a2c6811cbf6ae4504b4e9252ef1586ec4e70d63374fbe67d591d1bad3102351a0750492e2f7f7b5d379762010002f21e04852dabaace286308c2ff9a8ba4968d2e3cf17b7079594b8dff756eb0e0f7f69e1e000000005f18bff70000078302801e03000007570a00000000902f5009000000f8d6a2f8db1e7f3cbba1f94c96da8dc3873da20b9b46260a063fb01291ee3ba0c10ed74aa4afda5ffb358ba4906d0a5429f9840805e1fcc9ea71fca62afb5e97d779d0d2d3dd1bd9a3563166eff9d12b9ab9b6f11baf5639606689c72e16e2577ee338229e015697c8d174782b4caa04af9902eca2f3c384c6f558cf685b11010084a8436bbb18f7ccd039c249afa69f868c79939923ee3fc3ec437b998246db5b4907881702641ec6b41fdc6de644087713115fa2548121280fe8ac85fe31387aee6465767d8ba2189fab2ee8bf702beda6af018b4d68f890176bf2ab599e00000060b1acb36cdc03885d0f8ebeff5f1b7c21e3719f875a733ed58170621852d45dcd51b96d38e2dbb2002b43c44742d02d1c348280112fa2fe282f01a6a2615d0d33d19b03a0f5337c8ef41c2d84e2db25f56eb88bcf2ed76a094801d2fc831601005e7b462cc84ae0faaa5884bd5c4a5a5edf13db210599aeeb4d273c0f5f32967b7071ce2b4d490b9f08f6ce66a8405735c79197cd6773d1c5aeb2a38da1c102df07b05879c77198e5aafa7feed25d4137e86b3d98d9edd9547a460f1615b10000ee9570fbffedd44170477b37500a0a1cb3f94b6361f10f8a68c4075fbc17542d7174b3d95e12ddb8aea5d6b6c53c1df6c8f60010cd2e69902ba5e89e86747569463a23254730fc8d2aabf39648a505df9dcce461443b181ef3eda46074070000550836db2c97820971db6b1421e348d946ed4d3f255295abea46556615e3123de33ec56f784f70302901a4bc10c79c6a8b1e32477aeff9fba75876592981b678fc5a2703ac0b3055e567a6cb1ebab578fc4f9121fd968680250696cb85790000078fcfb60bdfc79aa1e377cb120480538e0236156f23129a88824ca5a1d77e371e5e98a16e6f32087c91aa02a4f5e00e412e515c3b678f6535141203c6886c637b626a2ada4062d037503359a680979091c68941a307db6e4ed8bc49d21b00002f0e6f88fb69309873fdefb015569e5511fb5399295204876543d065d177bf36ab79183a7c5e504b50691bc5b4ed0293324cfe2555d3fc8e39485822a90a91afcd4ef79ec3aefbd4cbe25cbccd802d8334ce1dce238c3f7505330a14615500001f89fbe1922ab3aa31a28fd29e19673714a7e48050dee59859d68345bb7bee7d5e888d8b798a58d7c650f9138304c05a92b668294c6114185ccb2c67ce0bbbb7e1dcbb6d76f5cacd7c9732a33b21d69bd7a28c9cca68b5735d50413862bc0100308bb0dd0bd53f3d1134966702dd3c7cc8b58b270a6996a646493250b0d5f3978d0c971f8fa7a0c958f3efe2fa5269244973fafb701c2eb66dd25901f93d677ab6c538c1ed11f115e52d3f2c7087ea40c3e8cd089376baa38842e9429b5f0000d19a8d874d791f952f13d3c8ecd92e44009c09815e5ae6a8e5def7ea52fe3de4accfb5ba2aa401fbcec14b069cd0dc0f66ab025b45ef9831a26acf58673db7487043654e7980fcb2b6c1bd7593a4dfff810436f653e309121c7ccf2df70b010000732254ec6df184be360cd9ed383ed7c8c236d7761cfc0ce4e7f0cac5a06f4edab9cfc75a7dc1449c0e18ed9564c974c2e1b6847c637f74e5d391cbc80fc6e672ffd66b5ce4fb73bda8359ab8a0ea1e855df1e07d82f93c935c7e1a9a55c5000065efdbb7c3e82291a482b2f24cbd46f4dd02c370cf6dcfe8fb3c00b8b004b5ad51369b1f1b134a824d1f16d72ca6a27ba2d6190150329139cf2c6d9e5a14722f8d39b96b882c1f60a7b230e929819e2abe1cd9d7f3e8c726b1a94d20c8010100732c396eca6ffa1bf851cef449f2f087edd93e4f641b4bd93a482d9f129e675aedb688993d4e2cee824d2803301364ba10fbb66895927adb53bad8aefe8a1caab6f4ccb45883e414a1223ac7f90a89087cd752dfa0c7b3e19bbae000edd5000028d1d23c627d1252d2a2a20a246af2280f50e3fde667873aadd9893ba6833118358398e7428e717128f764714a8d52b090c1f554f58e25ea815338d7bc7326c949567e74f2f2ab3c88f5075fea75594608b8937c9059a42d712ffbd1bd980100000000000250c1a474689e375a309446e5cdd3a0c26cecdcff5c7b8cdc0728868983f1a35a49e3a1bae6f969c3d47356c08d3d169d2c0a2be908d82cd35f41a23d8c2924a9f790ab3a00d53061d440a176670d6a32de2ecd19cf8a9774729c09a6ea4d0100d8838bf55d95521291da12294b302c66042eda0dc2acc79360a1fdd8c9a366fa790c52bf926c2d96b5ba88a3a443487c5235f7c476f350c2101cfbe3bd0361dd291ebc5e42c097a158704b71006886a3662ca6db7d816b4ad12444835d89000000795ce2b34aef921ccb3d9b9695f5d3fe0a03743c955cfcf01f8a1815a7c8b03de85fe15201d4b4b6f401cb334a6988ea5bde8986a468c47c3c6a5ae96a3160ff15e06699ea82bd40c0d5547fe1be77af7817861bbfcca3f4232f05a9cec800006c216565cee4d57b32d2d70bb3cb8d4a967c0eb5d7137b2ec58466f3d4d3b5375e4baa823bcc29c6ad877d9708cd5dc1c31fa3883a80710431110c4aa22e97b67fa639f54e86cfab87187011270139df7873bed12f6fb8cd9ab48f3893380100008ae0b7db6c1ff877ce8f89482f70e64851a04fccb1312eee94fbba8140e29a8200000000080000000000000002e2020300000079750aa1b2c4975c1af05c6c405141742fd2222a9efd09c82f54935c31a9cc05ab2e662ee04884b048ff9a28c82067bae975d747a419b53a457a169e33404874b26211dd7cf132ec396a2f87e9c5d5fd7e7b1cd059157d60648d4b002668d715f7bf185f030f0f20430061b484fe0f8426b41a4f42154007dce74945d1fa26c576c57dfb00480000240896e4f621d95725df0e2057d007ac4b49d60b24bcdee4c7bf3db910ebaebb22b185231800",
    "powData":"d3bd185f030f0f20d3bd185f030f0f20d3bd185f030f0f20d2bd185f030f0f20d2bd185f030f0f20d2bd185f030f0f20d2bd185f030f0f20d2bd185f030f0f20d2bd185f030f0f20d1bd185f030f0f20d1bd185f030f0f20d1bd185f030f0f20d1bd185f030f0f20d1bd185f030f0f20d1bd185f030f0f20d0bd185f030f0f20d0bd185f040f0f20d0bd185f040f0f20d0bd185f040f0f20d0bd185f040f0f20d0bd185f040f0f20cfbd185f040f0f20cfbd185f040f0f20cfbd185f040f0f20cfbd185f040f0f20cfbd185f040f0f20cfbd185f040f0f20cebd185f040f0f20",
    "mcBlockHeight":221,
    "mcNetwork":"regtest",
    "withdrawalEpochLength":10
}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>J'ai tout ce qu'il me faut pour rédiger le fichier de configuration qui permettra de lancer un node de gestion de ma sidechain. J'utilise le modèle fourni dans le Sidechains-SDK situé ici à partir de la racine du SDK : <code>examples/simpleapp/src/main/resources/settings_basic.conf</code>.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Voici à quoi il ressemble :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>scorex {
  dataDir = /tmp/scorex/data/blockchain
  logDir = /tmp/scorex/data/log

  restApi {
    bindAddress = "127.0.0.1:9085"
    api-key-hash = ""
  }

  network {
    nodeName = "testNode1"
    bindAddress = "127.0.0.1:9084"
    knownPeers = &#91;]
    agentName = "2-Hop"
  }

  websocket {
    address = "ws://localhost:8888"
    connectionTimeout = 100 milliseconds
    reconnectionDelay = 1 seconds
    reconnectionMaxAttempts = 1
  }

  withdrawalEpochCertificate {
    submitterIsEnabled =
    signersPublicKeys =
    signersThreshold =
    signersSecrets =
    provingKeyFilePath = "../../sdk/src/test/resources/sample_proving_key_7_keys_with_threshold_5"
    verificationKeyFilePath = "../../sdk/src/test/resources/sample_vk_7_keys_with_threshold_5"
  }

  wallet {
    seed = "seed1"
    genesisSecrets =
  }
}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>J'en fais une copie appelée <code>ma-sidechain.conf</code> dans le même répertoire et y ajoute les éléments spécifiques à ma sidechain :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ cp examples/simpleapp/src/main/resources/settings_basic.conf examples/simpleapp/src/main/resources/ma-sidechain.conf</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>J'ajoute 2 lignes de configuration à la structure <code>websocket</code>, le fichier modèle n'était pas à jour au moment de l'écriture de cet article et une tentative de lancement de la sidechain se soldait par l'erreur suivante : <code>"Exception in thread "main" com.typesafe.config.ConfigException$Missing: No configuration setting found for key 'scorex.websocket.zencliCommandLine'"</code></p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Il faut par ailleurs affecter à la variable <code>zencliCommandLine</code> le chemin permettant d'exécuter zen-cli. Si ce dernier est dans le <strong>PATH</strong>, alors, la valeur <code>"zen-cli"</code> suffit. De même, si tu utilises des paramètres particuliers (argument <code>conf</code> spécifique par exemple, tu peux les ajouter au tableau <code>zencliCommandLineArguments</code>.</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>zencliCommandLine = "zen-cli"
zencliCommandLineArguments = &#91;]</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Je dois ensuite adapter la structure <code>withdrawalEpochCertificate</code> :</p>
<!-- /wp:paragraph -->

<!-- wp:list -->
<ul><li><code>submitterIsEnabled</code> pour activer les transferts de la sidechain vers la mainchain.</li><li><code>signersPublicKeys</code> : C'est un tableau contenant la liste des <code>schnorrPublicKey</code> générée par la commande <code>generateProofInfo</code>. <strong>Il est important de respecter l'ordre des clés</strong>.</li><li><code>signersThreshold</code> : le paramètre <code>threshold</code> utilisé lors de la commande <code>generateProofInfo</code></li><li><code>signersSecrets</code> : C'est un tableau contenant la liste des <code>schnorrSecret</code> générée par la commande <code>generateProofInfo</code>. <strong>Il est important de respecter l'ordre des clés</strong>.</li><li><code>provingKeyFilePath</code> et <code>verificationKeyFilePath</code> : les chemins vers les 2 fichiers fournis dans le SDK. Il faut toutefois modifier la valeur par défaut et supprimer la partie <code>../../</code> avant <code>sdk/..</code> pour correspondre au chemin relatif à partir d'où on lance le node.</li></ul>
<!-- /wp:list -->

<!-- wp:paragraph -->
<p>Vient ensuite le paramétrage du <code>wallet</code> dans la structure du même nom. Cette initialisation du wallet est indispensable pour retrouver les fonds envoyés depuis la mainchain lors de la création de la sidechain. Les 400ZEN initialement envoyés seront ainsi disponibles dans ce wallet sur la sidechain.</p>
<!-- /wp:paragraph -->

<!-- wp:list -->
<ul><li><code>seed</code> : une graine au hasard. On utilisera <code>"q7yYYI80978LJF04r501n8Mv58222O2s"</code></li><li><code>genesisSecrets</code> : Un tableau contenant les clés <code>secret</code> et <code>vrfSecret</code> des appels à <code>generateKey</code> et <code>generateVrfKey</code>.</li></ul>
<!-- /wp:list -->

<!-- wp:paragraph -->
<p>Enfin, à l'intérieur de la structure <code>scorex</code>, après <code>wallet</code> mais avant la fermeture de la dernière accolade, je dois ajouter une structure nommée <code>genesis</code> contenant le retour de la commande <code>genesisinfo</code> vue plus haut.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Le reste des valeurs reste à la valeur par défaut et le fichier terminé doit ressembler à cela :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>scorex {
  dataDir = /tmp/scorex/data/blockchain
  logDir = /tmp/scorex/data/log

  restApi {
    bindAddress = "127.0.0.1:9085"
    api-key-hash = ""
  }

  network {
    nodeName = "testNode1"
    bindAddress = "127.0.0.1:9084"
    knownPeers = &#91;]
    agentName = "2-Hop"
  }

  websocket {
          address = "ws://localhost:8888"
          connectionTimeout = 100 milliseconds
          reconnectionDelay = 1 seconds
          reconnectionMaxAttempts = 1
          zencliCommandLine = ""
          zencliCommandLineArguments = &#91;]
  }

  withdrawalEpochCertificate {
    submitterIsEnabled = true
    signersPublicKeys = &#91;"328f42588d9df851c2c2a2b11336a5737ba913c0f92177c9d0a854c7564b48da761e53205d30d7f60e21ac382b452cabb7f9274e199a59976b0bd6aaf8f59ac171f6444b9bc594473a20c5a3c89e0d1335ffc8d0ee821c60ebcb1239601d0100dae9144ae4bd8d56638befec496d97eadc590acc2a04b9a210ec475b2505cd6774b1be10353b060c31ae1142b90f436fb5e4e1905814eec8a3e86e0cc5fcff36b686bb92d2f6200ebfc714cfd48db5c1ab3fa77b4747fb60a4f7ae6bd56e000000"
                         "a3c8f596cec2583278fcb7b460d0ae3b99283d26a22b599b1bfc41a5bc1a1328540eb64362f688437c553c114291853b2ab752fb6baf172f9a7cbdf7fbdb382445cd4672f313426fa92b071ce840103c6ee1463ff0c94a467bec754829a30000815e97336edbccb9523e80699f5c9ef893b5d51e41a50e757f86fc8aa13751b0f13041814140a98a4768f31917d59dd6af87e66a92a57beb25109bed3a12a6b4a9286746d778da486bec3e5bde2381187f243005011b6ccf05fd7568bf94000000"
                         "c76abd8288ec3b05e947adcf66a4fd91b8bbeae23d07cfe0196abffdc5d187f7c7cda485c00789f10d8c1650569a6fd4485cfa36e6247268e2aba1563beb3e2c8be87eae0df4a85f5057fddfd1f0bceef7998d20a9efcabc9e0a06e2fd7e010042c0738a904595ed5e3d7f29fb46b1e3918a597361824c7f7c2e9adb6c6ad947db05a36828aa3bb4355942fee130e2f3d5ed3eb35b7093c1cd31c8b8784e7f39c95c9a9b821b7eb867dcd4aa1dbf11410054e3ea1bd2759c479558604cb2000000"
                         "49e31139f382f266ecca749f54fcdf6bb4c48c2ccab895cb3971d9281ce7b3e3b5993728200867308547b6bc70fa04184e3b2d92732e463f763c0b91d3cc982272c875500d2a6c88b180ee59fad260853e37cbddf1ab9f268c31df00f73f01004a175a663b9a32586f36666b025bc8fa52c275cb8aadcf746825c834c4a5968ec5e083d55a6832bebee8c16adbb564ca3211bdc007e9c0be82b05230f3340525b2459f0acf24ad05eb9c5ab8c20432c289e735db90dcec3ef4b323dc4ef4000000"
                         "bb30f74e03b1448cc555d296849be13095e4ff7752ae5441fc68c61f2488cd36837abb8f95657e55ebbf20d8fccdc32673fd5f7c9db7355285f0d01ed707b273c78e85056e564dc613b7c7980611ec0b17fbe608e5c8135a233ed7e425540100bef6f0bb5e21dc446202a63a4892868ae5ef382d84dee1e2f46f6e33cf9c4bb7bdae5aeeec4e58e32ef113a70ff545dcfbaf80eb39907b11d76571dc1c913d07937297e20da6042e1d95b5e8642f10ab8dddc484c48722d140cd237392a5000000"
                         "4f85443c89b5550569451310b320ce03d6ea117aebe492aac4510945f67e20ffdd308e52415f8b89508144fa1d66997d82090fcc55e1969e055ac8ca26e6fefcdd0c8930e76f94df70b0701769e6664adda248298002068c4188357f952001001ceb9119562ff4904eb01a807439a4cd5f8e226f10ade178ce081d6345f12576d988a6e80f0bf54ec9ca8548c264485deb522f8e57cf8f7184d9fb01f805f430df7150595a75e6553c1ff868ff992584f3a52327b3d822236c9214ed53c9000000"
                         "30949730d160baec1bf4fcc743f2a533dbd9f6e853960bad9adf1390294f58387c0af553117368aa8c5d7769a0b39279b0fc92001d36323878c9e7ed2bfa1048496049f1d6d147ab949f1f76b0719bd4fc542a8840469291069116e06f0701000c227ab8d2b235d5eab4866451ad850ab2634d20bafd3da8c0b697d919fab2937e9bec3d39e41c7bf028575bcf4acb5c9f9977cd97b190e110f7cd0deae72adeae2e33974e56a0929a36db8245d9784ba44f0f071aa3937ad0257c4c5248010000"
                        ]
    signersThreshold = 5
    signersSecrets = &#91;"04000000600bbacac1017bb3a249a07409a83829df212bc3774069f9f3d758496bf8048d356799c7bd9fdf76369616ffc405c5844ee464ad628345a753a35bc6dfd3839ea7519896e45b2f1b610cc34aea072b5d523fd0e1a95f357d90261783c073f40000328f42588d9df851c2c2a2b11336a5737ba913c0f92177c9d0a854c7564b48da761e53205d30d7f60e21ac382b452cabb7f9274e199a59976b0bd6aaf8f59ac171f6444b9bc594473a20c5a3c89e0d1335ffc8d0ee821c60ebcb1239601d0100dae9144ae4bd8d56638befec496d97eadc590acc2a04b9a210ec475b2505cd6774b1be10353b060c31ae1142b90f436fb5e4e1905814eec8a3e86e0cc5fcff36b686bb92d2f6200ebfc714cfd48db5c1ab3fa77b4747fb60a4f7ae6bd56e000000",
                         "0400000060798dd23ca90d7ea6d85353c2a7d094cf75146ffa099a5c6b6de9a8bdcc0e4fd4edb271f6000a26cfb80b9cffe990d1dd6193e1031fe1b419e47653596aa7289a005eb19d0407783b0d70a3035c3de537c95be19b7d4b5b6b63cdd2b369d90000a3c8f596cec2583278fcb7b460d0ae3b99283d26a22b599b1bfc41a5bc1a1328540eb64362f688437c553c114291853b2ab752fb6baf172f9a7cbdf7fbdb382445cd4672f313426fa92b071ce840103c6ee1463ff0c94a467bec754829a30000815e97336edbccb9523e80699f5c9ef893b5d51e41a50e757f86fc8aa13751b0f13041814140a98a4768f31917d59dd6af87e66a92a57beb25109bed3a12a6b4a9286746d778da486bec3e5bde2381187f243005011b6ccf05fd7568bf94000000",
                         "04000000602d2b8008350f48fa073f4ac9129b8f4a7a7036b30f482cc5b3681cef02dfd3b713c2b08b0d989a5bd48656fe6995439cc6fdf99235b092343b13e1a7f43b937a9d488cfefe4d8f68afda8ebedec5a78cd5e7d77816223a7e88ac575d5c900100c76abd8288ec3b05e947adcf66a4fd91b8bbeae23d07cfe0196abffdc5d187f7c7cda485c00789f10d8c1650569a6fd4485cfa36e6247268e2aba1563beb3e2c8be87eae0df4a85f5057fddfd1f0bceef7998d20a9efcabc9e0a06e2fd7e010042c0738a904595ed5e3d7f29fb46b1e3918a597361824c7f7c2e9adb6c6ad947db05a36828aa3bb4355942fee130e2f3d5ed3eb35b7093c1cd31c8b8784e7f39c95c9a9b821b7eb867dcd4aa1dbf11410054e3ea1bd2759c479558604cb2000000",
                         "0400000060e6d06e813f23b4229a1a9723e511ca08bf65219149b7adfa0ba97e3104a40a41a21a40916de846788f596d08b90005db6124b1d872b0a3dc472142046a8e7f4b7784917b4029b85a16c41bf964283d6b92743d240140460c0037783e396c010049e31139f382f266ecca749f54fcdf6bb4c48c2ccab895cb3971d9281ce7b3e3b5993728200867308547b6bc70fa04184e3b2d92732e463f763c0b91d3cc982272c875500d2a6c88b180ee59fad260853e37cbddf1ab9f268c31df00f73f01004a175a663b9a32586f36666b025bc8fa52c275cb8aadcf746825c834c4a5968ec5e083d55a6832bebee8c16adbb564ca3211bdc007e9c0be82b05230f3340525b2459f0acf24ad05eb9c5ab8c20432c289e735db90dcec3ef4b323dc4ef4000000",
                         "0400000060d563c1a1cec8e162db0df8d2c0f24adf292431e29fef77fe68977e106216d398f036fc605797583b5f0bcc427230188f32ebc9427b73468cda9385b380b7351bf3434afcda85725beb178efa40718b16ddadfa6bff47ee3edc17455456bd0000bb30f74e03b1448cc555d296849be13095e4ff7752ae5441fc68c61f2488cd36837abb8f95657e55ebbf20d8fccdc32673fd5f7c9db7355285f0d01ed707b273c78e85056e564dc613b7c7980611ec0b17fbe608e5c8135a233ed7e425540100bef6f0bb5e21dc446202a63a4892868ae5ef382d84dee1e2f46f6e33cf9c4bb7bdae5aeeec4e58e32ef113a70ff545dcfbaf80eb39907b11d76571dc1c913d07937297e20da6042e1d95b5e8642f10ab8dddc484c48722d140cd237392a5000000",
                         "0400000060d4616f5ccab62713ff2ffb02353e14f13fc34a9ce0b3608a3d3833e8ca7abc54ebe6e981f5c80a2c758f5107d254b14fbff37cb51ad69fcfb5dcfa5bda71d048fe4e439b42a360894736f295681526800ce753800d778e733a20766270a100004f85443c89b5550569451310b320ce03d6ea117aebe492aac4510945f67e20ffdd308e52415f8b89508144fa1d66997d82090fcc55e1969e055ac8ca26e6fefcdd0c8930e76f94df70b0701769e6664adda248298002068c4188357f952001001ceb9119562ff4904eb01a807439a4cd5f8e226f10ade178ce081d6345f12576d988a6e80f0bf54ec9ca8548c264485deb522f8e57cf8f7184d9fb01f805f430df7150595a75e6553c1ff868ff992584f3a52327b3d822236c9214ed53c9000000",
                         "04000000603bdb5bb43e1adffb93265eccb0526f5ad1f7e671f30721da46abef1a3322ab5ab1a06b9bc89a2a7b9afd19295ad55fd9c2d775997883f51dc14b23e6a42721a710e3f7bdc7fce07ad74fd3fcb403c2eb61169bf8299eb8b43d726800492d010030949730d160baec1bf4fcc743f2a533dbd9f6e853960bad9adf1390294f58387c0af553117368aa8c5d7769a0b39279b0fc92001d36323878c9e7ed2bfa1048496049f1d6d147ab949f1f76b0719bd4fc542a8840469291069116e06f0701000c227ab8d2b235d5eab4866451ad850ab2634d20bafd3da8c0b697d919fab2937e9bec3d39e41c7bf028575bcf4acb5c9f9977cd97b190e110f7cd0deae72adeae2e33974e56a0929a36db8245d9784ba44f0f071aa3937ad0257c4c5248010000"
                        ]
    provingKeyFilePath = "sdk/src/test/resources/sample_proving_key_7_keys_with_threshold_5"
    verificationKeyFilePath = "sdk/src/test/resources/sample_vk_7_keys_with_threshold_5"
  }

  wallet {
    seed = "q7yYYI80978LJF04r501n8Mv58222O2s"
    genesisSecrets = &#91;"00531e40d4036d808d2101756debc7fd2365cd76da1176f5bf2ed09603d01f711ca03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f8"
                      "0300000060b218f4c543504110af4dd02a72a764d5c1c59beb1e6dc3ea0dd85f86685eade90f567f9937b3a4fc36bb5adfe480ac381ccdbb91f11c269e2a41d71d829fc312cdef48bf75818447405bb86202a1d39f0503491207eab329bb77047805f800000ed74aa4afda5ffb358ba4906d0a5429f9840805e1fcc9ea71fca62afb5e97d779d0d2d3dd1bd9a3563166eff9d12b9ab9b6f11baf5639606689c72e16e2577ee338229e015697c8d174782b4caa04af9902eca2f3c384c6f558cf685b11010084a8436bbb18f7ccd039c249afa69f868c79939923ee3fc3ec437b998246db5b4907881702641ec6b41fdc6de644087713115fa2548121280fe8ac85fe31387aee6465767d8ba2189fab2ee8bf702beda6af018b4d68f890176bf2ab599e000000"
                     ]
  }

  genesis {
    "scId":"d40188abb42d6ba6302464a847ace23e52b0c7edcf433cd17c10ca008b5e8783",
    "scGenesisBlockHex":"010000000000000000000000000000000000000000000000000000000000000000cca1c1f10ba204e933f41dc86ca654a03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f800000009502f9000a03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f80ed74aa4afda5ffb358ba4906d0a5429f9840805e1fcc9ea71fca62afb5e97d779d0d2d3dd1bd9a3563166eff9d12b9ab9b6f11baf5639606689c72e16e2577ee338229e015697c8d174782b4caa04af9902eca2f3c384c6f558cf685b11010084a8436bbb18f7ccd039c249afa69f868c79939923ee3fc3ec437b998246db5b4907881702641ec6b41fdc6de644087713115fa2548121280fe8ac85fe31387aee6465767d8ba2189fab2ee8bf702beda6af018b4d68f890176bf2ab599e0000000800000000dcc662d4348c35e93abd86d54e73d3f89de9011e217345c1576a535919c7625c216a0d2942dda7d257d38ea93197127756c1630ef63efc6d967ea02e620952d382d2a4c32f323a85c2bae120e2c1e5add29bbb5a1d0b2c9da18bbbd583d50000fc45b48a89b591177675dfedaec458c7a552556e968fccd9012bba7315807435187426124dcaf81a6f3b6503e78c0254328607a2538be94388708527a637234bdc9c4c26350bb3720d0c90dd0ea0cd5349e519b4e6b2e8585256b8688364010000508457c4bdc88baa74cedfe4e35f7937f14d3093f9202ac116f4c7f4fdfff7da657088e70371a6c474ddf3b0bc40475c75e6c4104d0344b32d1bd458a5185b063c6e27de5ef992bfc7f2852b80dc47c0e4200821b0a1ae27ebbff6cf8fc90000ef55c7566071fdb85b08027632f50b6f443fb64f83ce3aa83960770550855ff95e0f98af05ea16532e364c877b616ae5bc4f6fc3b40359fe79e7e20cd6fa701204ddba0ae615887d84cd26264b8824fe27e07e9ea0501493dd9588d19ba3000000000000000000000000000000000000000000000000000000000000000000005f10d71366010e474d9ed8c1f055396495b51d561bac632c4e353871614422ff00000000000000000000000000000000000000000000000000000000000000000080016f16a558e106b6d367cfe82497d7bc95955aa570a2c6811cbf6ae4504b4e9252ef1586ec4e70d63374fbe67d591d1bad3102351a0750492e2f7f7b5d379762010002f21e04852dabaace286308c2ff9a8ba4968d2e3cf17b7079594b8dff756eb0e0f7f69e1e000000005f18bff70000078302801e03000007570a00000000902f5009000000f8d6a2f8db1e7f3cbba1f94c96da8dc3873da20b9b46260a063fb01291ee3ba0c10ed74aa4afda5ffb358ba4906d0a5429f9840805e1fcc9ea71fca62afb5e97d779d0d2d3dd1bd9a3563166eff9d12b9ab9b6f11baf5639606689c72e16e2577ee338229e015697c8d174782b4caa04af9902eca2f3c384c6f558cf685b11010084a8436bbb18f7ccd039c249afa69f868c79939923ee3fc3ec437b998246db5b4907881702641ec6b41fdc6de644087713115fa2548121280fe8ac85fe31387aee6465767d8ba2189fab2ee8bf702beda6af018b4d68f890176bf2ab599e00000060b1acb36cdc03885d0f8ebeff5f1b7c21e3719f875a733ed58170621852d45dcd51b96d38e2dbb2002b43c44742d02d1c348280112fa2fe282f01a6a2615d0d33d19b03a0f5337c8ef41c2d84e2db25f56eb88bcf2ed76a094801d2fc831601005e7b462cc84ae0faaa5884bd5c4a5a5edf13db210599aeeb4d273c0f5f32967b7071ce2b4d490b9f08f6ce66a8405735c79197cd6773d1c5aeb2a38da1c102df07b05879c77198e5aafa7feed25d4137e86b3d98d9edd9547a460f1615b10000ee9570fbffedd44170477b37500a0a1cb3f94b6361f10f8a68c4075fbc17542d7174b3d95e12ddb8aea5d6b6c53c1df6c8f60010cd2e69902ba5e89e86747569463a23254730fc8d2aabf39648a505df9dcce461443b181ef3eda46074070000550836db2c97820971db6b1421e348d946ed4d3f255295abea46556615e3123de33ec56f784f70302901a4bc10c79c6a8b1e32477aeff9fba75876592981b678fc5a2703ac0b3055e567a6cb1ebab578fc4f9121fd968680250696cb85790000078fcfb60bdfc79aa1e377cb120480538e0236156f23129a88824ca5a1d77e371e5e98a16e6f32087c91aa02a4f5e00e412e515c3b678f6535141203c6886c637b626a2ada4062d037503359a680979091c68941a307db6e4ed8bc49d21b00002f0e6f88fb69309873fdefb015569e5511fb5399295204876543d065d177bf36ab79183a7c5e504b50691bc5b4ed0293324cfe2555d3fc8e39485822a90a91afcd4ef79ec3aefbd4cbe25cbccd802d8334ce1dce238c3f7505330a14615500001f89fbe1922ab3aa31a28fd29e19673714a7e48050dee59859d68345bb7bee7d5e888d8b798a58d7c650f9138304c05a92b668294c6114185ccb2c67ce0bbbb7e1dcbb6d76f5cacd7c9732a33b21d69bd7a28c9cca68b5735d50413862bc0100308bb0dd0bd53f3d1134966702dd3c7cc8b58b270a6996a646493250b0d5f3978d0c971f8fa7a0c958f3efe2fa5269244973fafb701c2eb66dd25901f93d677ab6c538c1ed11f115e52d3f2c7087ea40c3e8cd089376baa38842e9429b5f0000d19a8d874d791f952f13d3c8ecd92e44009c09815e5ae6a8e5def7ea52fe3de4accfb5ba2aa401fbcec14b069cd0dc0f66ab025b45ef9831a26acf58673db7487043654e7980fcb2b6c1bd7593a4dfff810436f653e309121c7ccf2df70b010000732254ec6df184be360cd9ed383ed7c8c236d7761cfc0ce4e7f0cac5a06f4edab9cfc75a7dc1449c0e18ed9564c974c2e1b6847c637f74e5d391cbc80fc6e672ffd66b5ce4fb73bda8359ab8a0ea1e855df1e07d82f93c935c7e1a9a55c5000065efdbb7c3e82291a482b2f24cbd46f4dd02c370cf6dcfe8fb3c00b8b004b5ad51369b1f1b134a824d1f16d72ca6a27ba2d6190150329139cf2c6d9e5a14722f8d39b96b882c1f60a7b230e929819e2abe1cd9d7f3e8c726b1a94d20c8010100732c396eca6ffa1bf851cef449f2f087edd93e4f641b4bd93a482d9f129e675aedb688993d4e2cee824d2803301364ba10fbb66895927adb53bad8aefe8a1caab6f4ccb45883e414a1223ac7f90a89087cd752dfa0c7b3e19bbae000edd5000028d1d23c627d1252d2a2a20a246af2280f50e3fde667873aadd9893ba6833118358398e7428e717128f764714a8d52b090c1f554f58e25ea815338d7bc7326c949567e74f2f2ab3c88f5075fea75594608b8937c9059a42d712ffbd1bd980100000000000250c1a474689e375a309446e5cdd3a0c26cecdcff5c7b8cdc0728868983f1a35a49e3a1bae6f969c3d47356c08d3d169d2c0a2be908d82cd35f41a23d8c2924a9f790ab3a00d53061d440a176670d6a32de2ecd19cf8a9774729c09a6ea4d0100d8838bf55d95521291da12294b302c66042eda0dc2acc79360a1fdd8c9a366fa790c52bf926c2d96b5ba88a3a443487c5235f7c476f350c2101cfbe3bd0361dd291ebc5e42c097a158704b71006886a3662ca6db7d816b4ad12444835d89000000795ce2b34aef921ccb3d9b9695f5d3fe0a03743c955cfcf01f8a1815a7c8b03de85fe15201d4b4b6f401cb334a6988ea5bde8986a468c47c3c6a5ae96a3160ff15e06699ea82bd40c0d5547fe1be77af7817861bbfcca3f4232f05a9cec800006c216565cee4d57b32d2d70bb3cb8d4a967c0eb5d7137b2ec58466f3d4d3b5375e4baa823bcc29c6ad877d9708cd5dc1c31fa3883a80710431110c4aa22e97b67fa639f54e86cfab87187011270139df7873bed12f6fb8cd9ab48f3893380100008ae0b7db6c1ff877ce8f89482f70e64851a04fccb1312eee94fbba8140e29a8200000000080000000000000002e2020300000079750aa1b2c4975c1af05c6c405141742fd2222a9efd09c82f54935c31a9cc05ab2e662ee04884b048ff9a28c82067bae975d747a419b53a457a169e33404874b26211dd7cf132ec396a2f87e9c5d5fd7e7b1cd059157d60648d4b002668d715f7bf185f030f0f20430061b484fe0f8426b41a4f42154007dce74945d1fa26c576c57dfb00480000240896e4f621d95725df0e2057d007ac4b49d60b24bcdee4c7bf3db910ebaebb22b185231800",
    "powData":"d3bd185f030f0f20d3bd185f030f0f20d3bd185f030f0f20d2bd185f030f0f20d2bd185f030f0f20d2bd185f030f0f20d2bd185f030f0f20d2bd185f030f0f20d2bd185f030f0f20d1bd185f030f0f20d1bd185f030f0f20d1bd185f030f0f20d1bd185f030f0f20d1bd185f030f0f20d1bd185f030f0f20d0bd185f030f0f20d0bd185f040f0f20d0bd185f040f0f20d0bd185f040f0f20d0bd185f040f0f20d0bd185f040f0f20cfbd185f040f0f20cfbd185f040f0f20cfbd185f040f0f20cfbd185f040f0f20cfbd185f040f0f20cfbd185f040f0f20cebd185f040f0f20",
    "mcBlockHeight":221,
    "mcNetwork":"regtest",
    "withdrawalEpochLength":10
  }
}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p><strong>Nota bene :</strong> Concernant <code>dataDir</code> et <code>logDir</code>, il pourrait être judicieux de les stocker dans un sous-répertoire <code>tmp</code> du répertoire utilisateur plutôt que de les laisser dans le répertoire <code>/tmp/</code> global mais pour l'instant, j'ai laissé tel quel.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Je peux alors enfin lancer ma SideChain. Ceci doit être fait dans le répertoire principal du SideChain-SDK :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ pwd
/home/zendoo/Sidechains-SDK
$ java -cp ./examples/simpleapp/target/Sidechains-SDK-simpleapp-0.2.1.jar:./examples/simpleapp/target/lib/* com.horizen.examples.SimpleApp ./examples/simpleapp/src/main/resources/ma-sidechain.conf
&#91;2-Hop-akka.actor.default-dispatcher-3] INFO scorex.core.network.NetworkController - Declared address: None
&#91;main] INFO com.horizen.SidechainApp - Starting application with settings
SidechainSettings(ScorexSettings(/tmp/scorex/data/blockchain,/tmp/scorex/data/log,NetworkSettings(testNode1,None,false,Vector(),/127.0.0.1:9084,20,1 second,false,None,None,None,30 seconds,2 seconds,2,0.0.1,2-Hop,1048576,8096,512,512,5 seconds,2 minutes,20 seconds,4 minutes,Some(5 seconds),Some(5 seconds),1024,&#91;B@2a640157,2 minutes,64,1 hour,5 minutes,100),RESTApiSettings(/127.0.0.1:9085,None,Some(*),5 seconds),NetworkTimeProviderSettings(pool.ntp.org,30 minutes,30 seconds)),GenesisDataSettings(010000000000000000000000000000000000000000000000000000000000000000cca1c1f10ba204e933f41dc86ca654a03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f800000009502f9000a03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f80ed74aa4afda5ffb358ba4906d0a5429f9840805e1fcc9ea71fca62afb5e97d779d0d2d3dd1bd9a3563166eff9d12b9ab9b6f11baf5639606689c72e16e2577ee338229e015697c8d174782b4caa04af9902eca2f3c384c6f558cf685b11010084a8436bbb18f7ccd039c249afa69f868c79939923ee3fc3ec437b998246db5b4907881702641ec6b41fdc6de644087713115fa2548121280fe8ac85fe31387aee6465767d8ba2189fab2ee8bf702beda6af018b4d68f890176bf2ab599e0000000800000000dcc662d4348c35e93abd86d54e73d3f89de9011e217345c1576a535919c7625c216a0d2942dda7d257d38ea93197127756c1630ef63efc6d967ea02e620952d382d2a4c32f323a85c2bae120e2c1e5add29bbb5a1d0b2c9da18bbbd583d50000fc45b48a89b591177675dfedaec458c7a552556e968fccd9012bba7315807435187426124dcaf81a6f3b6503e78c0254328607a2538be94388708527a637234bdc9c4c26350bb3720d0c90dd0ea0cd5349e519b4e6b2e8585256b8688364010000508457c4bdc88baa74cedfe4e35f7937f14d3093f9202ac116f4c7f4fdfff7da657088e70371a6c474ddf3b0bc40475c75e6c4104d0344b32d1bd458a5185b063c6e27de5ef992bfc7f2852b80dc47c0e4200821b0a1ae27ebbff6cf8fc90000ef55c7566071fdb85b08027632f50b6f443fb64f83ce3aa83960770550855ff95e0f98af05ea16532e364c877b616ae5bc4f6fc3b40359fe79e7e20cd6fa701204ddba0ae615887d84cd26264b8824fe27e07e9ea0501493dd9588d19ba3000000000000000000000000000000000000000000000000000000000000000000005f10d71366010e474d9ed8c1f055396495b51d561bac632c4e353871614422ff00000000000000000000000000000000000000000000000000000000000000000080016f16a558e106b6d367cfe82497d7bc95955aa570a2c6811cbf6ae4504b4e9252ef1586ec4e70d63374fbe67d591d1bad3102351a0750492e2f7f7b5d379762010002f21e04852dabaace286308c2ff9a8ba4968d2e3cf17b7079594b8dff756eb0e0f7f69e1e000000005f18bff70000078302801e03000007570a00000000902f5009000000f8d6a2f8db1e7f3cbba1f94c96da8dc3873da20b9b46260a063fb01291ee3ba0c10ed74aa4afda5ffb358ba4906d0a5429f9840805e1fcc9ea71fca62afb5e97d779d0d2d3dd1bd9a3563166eff9d12b9ab9b6f11baf5639606689c72e16e2577ee338229e015697c8d174782b4caa04af9902eca2f3c384c6f558cf685b11010084a8436bbb18f7ccd039c249afa69f868c79939923ee3fc3ec437b998246db5b4907881702641ec6b41fdc6de644087713115fa2548121280fe8ac85fe31387aee6465767d8ba2189fab2ee8bf702beda6af018b4d68f890176bf2ab599e00000060b1acb36cdc03885d0f8ebeff5f1b7c21e3719f875a733ed58170621852d45dcd51b96d38e2dbb2002b43c44742d02d1c348280112fa2fe282f01a6a2615d0d33d19b03a0f5337c8ef41c2d84e2db25f56eb88bcf2ed76a094801d2fc831601005e7b462cc84ae0faaa5884bd5c4a5a5edf13db210599aeeb4d273c0f5f32967b7071ce2b4d490b9f08f6ce66a8405735c79197cd6773d1c5aeb2a38da1c102df07b05879c77198e5aafa7feed25d4137e86b3d98d9edd9547a460f1615b10000ee9570fbffedd44170477b37500a0a1cb3f94b6361f10f8a68c4075fbc17542d7174b3d95e12ddb8aea5d6b6c53c1df6c8f60010cd2e69902ba5e89e86747569463a23254730fc8d2aabf39648a505df9dcce461443b181ef3eda46074070000550836db2c97820971db6b1421e348d946ed4d3f255295abea46556615e3123de33ec56f784f70302901a4bc10c79c6a8b1e32477aeff9fba75876592981b678fc5a2703ac0b3055e567a6cb1ebab578fc4f9121fd968680250696cb85790000078fcfb60bdfc79aa1e377cb120480538e0236156f23129a88824ca5a1d77e371e5e98a16e6f32087c91aa02a4f5e00e412e515c3b678f6535141203c6886c637b626a2ada4062d037503359a680979091c68941a307db6e4ed8bc49d21b00002f0e6f88fb69309873fdefb015569e5511fb5399295204876543d065d177bf36ab79183a7c5e504b50691bc5b4ed0293324cfe2555d3fc8e39485822a90a91afcd4ef79ec3aefbd4cbe25cbccd802d8334ce1dce238c3f7505330a14615500001f89fbe1922ab3aa31a28fd29e19673714a7e48050dee59859d68345bb7bee7d5e888d8b798a58d7c650f9138304c05a92b668294c6114185ccb2c67ce0bbbb7e1dcbb6d76f5cacd7c9732a33b21d69bd7a28c9cca68b5735d50413862bc0100308bb0dd0bd53f3d1134966702dd3c7cc8b58b270a6996a646493250b0d5f3978d0c971f8fa7a0c958f3efe2fa5269244973fafb701c2eb66dd25901f93d677ab6c538c1ed11f115e52d3f2c7087ea40c3e8cd089376baa38842e9429b5f0000d19a8d874d791f952f13d3c8ecd92e44009c09815e5ae6a8e5def7ea52fe3de4accfb5ba2aa401fbcec14b069cd0dc0f66ab025b45ef9831a26acf58673db7487043654e7980fcb2b6c1bd7593a4dfff810436f653e309121c7ccf2df70b010000732254ec6df184be360cd9ed383ed7c8c236d7761cfc0ce4e7f0cac5a06f4edab9cfc75a7dc1449c0e18ed9564c974c2e1b6847c637f74e5d391cbc80fc6e672ffd66b5ce4fb73bda8359ab8a0ea1e855df1e07d82f93c935c7e1a9a55c5000065efdbb7c3e82291a482b2f24cbd46f4dd02c370cf6dcfe8fb3c00b8b004b5ad51369b1f1b134a824d1f16d72ca6a27ba2d6190150329139cf2c6d9e5a14722f8d39b96b882c1f60a7b230e929819e2abe1cd9d7f3e8c726b1a94d20c8010100732c396eca6ffa1bf851cef449f2f087edd93e4f641b4bd93a482d9f129e675aedb688993d4e2cee824d2803301364ba10fbb66895927adb53bad8aefe8a1caab6f4ccb45883e414a1223ac7f90a89087cd752dfa0c7b3e19bbae000edd5000028d1d23c627d1252d2a2a20a246af2280f50e3fde667873aadd9893ba6833118358398e7428e717128f764714a8d52b090c1f554f58e25ea815338d7bc7326c949567e74f2f2ab3c88f5075fea75594608b8937c9059a42d712ffbd1bd980100000000000250c1a474689e375a309446e5cdd3a0c26cecdcff5c7b8cdc0728868983f1a35a49e3a1bae6f969c3d47356c08d3d169d2c0a2be908d82cd35f41a23d8c2924a9f790ab3a00d53061d440a176670d6a32de2ecd19cf8a9774729c09a6ea4d0100d8838bf55d95521291da12294b302c66042eda0dc2acc79360a1fdd8c9a366fa790c52bf926c2d96b5ba88a3a443487c5235f7c476f350c2101cfbe3bd0361dd291ebc5e42c097a158704b71006886a3662ca6db7d816b4ad12444835d89000000795ce2b34aef921ccb3d9b9695f5d3fe0a03743c955cfcf01f8a1815a7c8b03de85fe15201d4b4b6f401cb334a6988ea5bde8986a468c47c3c6a5ae96a3160ff15e06699ea82bd40c0d5547fe1be77af7817861bbfcca3f4232f05a9cec800006c216565cee4d57b32d2d70bb3cb8d4a967c0eb5d7137b2ec58466f3d4d3b5375e4baa823bcc29c6ad877d9708cd5dc1c31fa3883a80710431110c4aa22e97b67fa639f54e86cfab87187011270139df7873bed12f6fb8cd9ab48f3893380100008ae0b7db6c1ff877ce8f89482f70e64851a04fccb1312eee94fbba8140e29a8200000000080000000000000002e2020300000079750aa1b2c4975c1af05c6c405141742fd2222a9efd09c82f54935c31a9cc05ab2e662ee04884b048ff9a28c82067bae975d747a419b53a457a169e33404874b26211dd7cf132ec396a2f87e9c5d5fd7e7b1cd059157d60648d4b002668d715f7bf185f030f0f20430061b484fe0f8426b41a4f42154007dce74945d1fa26c576c57dfb00480000240896e4f621d95725df0e2057d007ac4b49d60b24bcdee4c7bf3db910ebaebb22b185231800,d40188abb42d6ba6302464a847ace23e52b0c7edcf433cd17c10ca008b5e8783,221,d3bd185f030f0f20d3bd185f030f0f20d3bd185f030f0f20d2bd185f030f0f20d2bd185f030f0f20d2bd185f030f0f20d2bd185f030f0f20d2bd185f030f0f20d2bd185f030f0f20d1bd185f030f0f20d1bd185f030f0f20d1bd185f030f0f20d1bd185f030f0f20d1bd185f030f0f20d1bd185f030f0f20d0bd185f030f0f20d0bd185f040f0f20d0bd185f040f0f20d0bd185f040f0f20d0bd185f040f0f20d0bd185f040f0f20cfbd185f040f0f20cfbd185f040f0f20cfbd185f040f0f20cfbd185f040f0f20cfbd185f040f0f20cfbd185f040f0f20cebd185f040f0f20,regtest,10),WebSocketSettings(ws://localhost:8888,100 milliseconds,1 second,1,,Some(Vector()),true),withdrawalEpochCertificateSettings(true,Vector(328f42588d9df851c2c2a2b11336a5737ba913c0f92177c9d0a854c7564b48da761e53205d30d7f60e21ac382b452cabb7f9274e199a59976b0bd6aaf8f59ac171f6444b9bc594473a20c5a3c89e0d1335ffc8d0ee821c60ebcb1239601d0100dae9144ae4bd8d56638befec496d97eadc590acc2a04b9a210ec475b2505cd6774b1be10353b060c31ae1142b90f436fb5e4e1905814eec8a3e86e0cc5fcff36b686bb92d2f6200ebfc714cfd48db5c1ab3fa77b4747fb60a4f7ae6bd56e000000, a3c8f596cec2583278fcb7b460d0ae3b99283d26a22b599b1bfc41a5bc1a1328540eb64362f688437c553c114291853b2ab752fb6baf172f9a7cbdf7fbdb382445cd4672f313426fa92b071ce840103c6ee1463ff0c94a467bec754829a30000815e97336edbccb9523e80699f5c9ef893b5d51e41a50e757f86fc8aa13751b0f13041814140a98a4768f31917d59dd6af87e66a92a57beb25109bed3a12a6b4a9286746d778da486bec3e5bde2381187f243005011b6ccf05fd7568bf94000000, c76abd8288ec3b05e947adcf66a4fd91b8bbeae23d07cfe0196abffdc5d187f7c7cda485c00789f10d8c1650569a6fd4485cfa36e6247268e2aba1563beb3e2c8be87eae0df4a85f5057fddfd1f0bceef7998d20a9efcabc9e0a06e2fd7e010042c0738a904595ed5e3d7f29fb46b1e3918a597361824c7f7c2e9adb6c6ad947db05a36828aa3bb4355942fee130e2f3d5ed3eb35b7093c1cd31c8b8784e7f39c95c9a9b821b7eb867dcd4aa1dbf11410054e3ea1bd2759c479558604cb2000000, 49e31139f382f266ecca749f54fcdf6bb4c48c2ccab895cb3971d9281ce7b3e3b5993728200867308547b6bc70fa04184e3b2d92732e463f763c0b91d3cc982272c875500d2a6c88b180ee59fad260853e37cbddf1ab9f268c31df00f73f01004a175a663b9a32586f36666b025bc8fa52c275cb8aadcf746825c834c4a5968ec5e083d55a6832bebee8c16adbb564ca3211bdc007e9c0be82b05230f3340525b2459f0acf24ad05eb9c5ab8c20432c289e735db90dcec3ef4b323dc4ef4000000, bb30f74e03b1448cc555d296849be13095e4ff7752ae5441fc68c61f2488cd36837abb8f95657e55ebbf20d8fccdc32673fd5f7c9db7355285f0d01ed707b273c78e85056e564dc613b7c7980611ec0b17fbe608e5c8135a233ed7e425540100bef6f0bb5e21dc446202a63a4892868ae5ef382d84dee1e2f46f6e33cf9c4bb7bdae5aeeec4e58e32ef113a70ff545dcfbaf80eb39907b11d76571dc1c913d07937297e20da6042e1d95b5e8642f10ab8dddc484c48722d140cd237392a5000000, 4f85443c89b5550569451310b320ce03d6ea117aebe492aac4510945f67e20ffdd308e52415f8b89508144fa1d66997d82090fcc55e1969e055ac8ca26e6fefcdd0c8930e76f94df70b0701769e6664adda248298002068c4188357f952001001ceb9119562ff4904eb01a807439a4cd5f8e226f10ade178ce081d6345f12576d988a6e80f0bf54ec9ca8548c264485deb522f8e57cf8f7184d9fb01f805f430df7150595a75e6553c1ff868ff992584f3a52327b3d822236c9214ed53c9000000, 30949730d160baec1bf4fcc743f2a533dbd9f6e853960bad9adf1390294f58387c0af553117368aa8c5d7769a0b39279b0fc92001d36323878c9e7ed2bfa1048496049f1d6d147ab949f1f76b0719bd4fc542a8840469291069116e06f0701000c227ab8d2b235d5eab4866451ad850ab2634d20bafd3da8c0b697d919fab2937e9bec3d39e41c7bf028575bcf4acb5c9f9977cd97b190e110f7cd0deae72adeae2e33974e56a0929a36db8245d9784ba44f0f071aa3937ad0257c4c5248010000),5,Vector(04000000600bbacac1017bb3a249a07409a83829df212bc3774069f9f3d758496bf8048d356799c7bd9fdf76369616ffc405c5844ee464ad628345a753a35bc6dfd3839ea7519896e45b2f1b610cc34aea072b5d523fd0e1a95f357d90261783c073f40000328f42588d9df851c2c2a2b11336a5737ba913c0f92177c9d0a854c7564b48da761e53205d30d7f60e21ac382b452cabb7f9274e199a59976b0bd6aaf8f59ac171f6444b9bc594473a20c5a3c89e0d1335ffc8d0ee821c60ebcb1239601d0100dae9144ae4bd8d56638befec496d97eadc590acc2a04b9a210ec475b2505cd6774b1be10353b060c31ae1142b90f436fb5e4e1905814eec8a3e86e0cc5fcff36b686bb92d2f6200ebfc714cfd48db5c1ab3fa77b4747fb60a4f7ae6bd56e000000, 0400000060798dd23ca90d7ea6d85353c2a7d094cf75146ffa099a5c6b6de9a8bdcc0e4fd4edb271f6000a26cfb80b9cffe990d1dd6193e1031fe1b419e47653596aa7289a005eb19d0407783b0d70a3035c3de537c95be19b7d4b5b6b63cdd2b369d90000a3c8f596cec2583278fcb7b460d0ae3b99283d26a22b599b1bfc41a5bc1a1328540eb64362f688437c553c114291853b2ab752fb6baf172f9a7cbdf7fbdb382445cd4672f313426fa92b071ce840103c6ee1463ff0c94a467bec754829a30000815e97336edbccb9523e80699f5c9ef893b5d51e41a50e757f86fc8aa13751b0f13041814140a98a4768f31917d59dd6af87e66a92a57beb25109bed3a12a6b4a9286746d778da486bec3e5bde2381187f243005011b6ccf05fd7568bf94000000, 04000000602d2b8008350f48fa073f4ac9129b8f4a7a7036b30f482cc5b3681cef02dfd3b713c2b08b0d989a5bd48656fe6995439cc6fdf99235b092343b13e1a7f43b937a9d488cfefe4d8f68afda8ebedec5a78cd5e7d77816223a7e88ac575d5c900100c76abd8288ec3b05e947adcf66a4fd91b8bbeae23d07cfe0196abffdc5d187f7c7cda485c00789f10d8c1650569a6fd4485cfa36e6247268e2aba1563beb3e2c8be87eae0df4a85f5057fddfd1f0bceef7998d20a9efcabc9e0a06e2fd7e010042c0738a904595ed5e3d7f29fb46b1e3918a597361824c7f7c2e9adb6c6ad947db05a36828aa3bb4355942fee130e2f3d5ed3eb35b7093c1cd31c8b8784e7f39c95c9a9b821b7eb867dcd4aa1dbf11410054e3ea1bd2759c479558604cb2000000, 0400000060e6d06e813f23b4229a1a9723e511ca08bf65219149b7adfa0ba97e3104a40a41a21a40916de846788f596d08b90005db6124b1d872b0a3dc472142046a8e7f4b7784917b4029b85a16c41bf964283d6b92743d240140460c0037783e396c010049e31139f382f266ecca749f54fcdf6bb4c48c2ccab895cb3971d9281ce7b3e3b5993728200867308547b6bc70fa04184e3b2d92732e463f763c0b91d3cc982272c875500d2a6c88b180ee59fad260853e37cbddf1ab9f268c31df00f73f01004a175a663b9a32586f36666b025bc8fa52c275cb8aadcf746825c834c4a5968ec5e083d55a6832bebee8c16adbb564ca3211bdc007e9c0be82b05230f3340525b2459f0acf24ad05eb9c5ab8c20432c289e735db90dcec3ef4b323dc4ef4000000, 0400000060d563c1a1cec8e162db0df8d2c0f24adf292431e29fef77fe68977e106216d398f036fc605797583b5f0bcc427230188f32ebc9427b73468cda9385b380b7351bf3434afcda85725beb178efa40718b16ddadfa6bff47ee3edc17455456bd0000bb30f74e03b1448cc555d296849be13095e4ff7752ae5441fc68c61f2488cd36837abb8f95657e55ebbf20d8fccdc32673fd5f7c9db7355285f0d01ed707b273c78e85056e564dc613b7c7980611ec0b17fbe608e5c8135a233ed7e425540100bef6f0bb5e21dc446202a63a4892868ae5ef382d84dee1e2f46f6e33cf9c4bb7bdae5aeeec4e58e32ef113a70ff545dcfbaf80eb39907b11d76571dc1c913d07937297e20da6042e1d95b5e8642f10ab8dddc484c48722d140cd237392a5000000, 0400000060d4616f5ccab62713ff2ffb02353e14f13fc34a9ce0b3608a3d3833e8ca7abc54ebe6e981f5c80a2c758f5107d254b14fbff37cb51ad69fcfb5dcfa5bda71d048fe4e439b42a360894736f295681526800ce753800d778e733a20766270a100004f85443c89b5550569451310b320ce03d6ea117aebe492aac4510945f67e20ffdd308e52415f8b89508144fa1d66997d82090fcc55e1969e055ac8ca26e6fefcdd0c8930e76f94df70b0701769e6664adda248298002068c4188357f952001001ceb9119562ff4904eb01a807439a4cd5f8e226f10ade178ce081d6345f12576d988a6e80f0bf54ec9ca8548c264485deb522f8e57cf8f7184d9fb01f805f430df7150595a75e6553c1ff868ff992584f3a52327b3d822236c9214ed53c9000000, 04000000603bdb5bb43e1adffb93265eccb0526f5ad1f7e671f30721da46abef1a3322ab5ab1a06b9bc89a2a7b9afd19295ad55fd9c2d775997883f51dc14b23e6a42721a710e3f7bdc7fce07ad74fd3fcb403c2eb61169bf8299eb8b43d726800492d010030949730d160baec1bf4fcc743f2a533dbd9f6e853960bad9adf1390294f58387c0af553117368aa8c5d7769a0b39279b0fc92001d36323878c9e7ed2bfa1048496049f1d6d147ab949f1f76b0719bd4fc542a8840469291069116e06f0701000c227ab8d2b235d5eab4866451ad850ab2634d20bafd3da8c0b697d919fab2937e9bec3d39e41c7bf028575bcf4acb5c9f9977cd97b190e110f7cd0deae72adeae2e33974e56a0929a36db8245d9784ba44f0f071aa3937ad0257c4c5248010000),sdk/src/test/resources/sample_proving_key_7_keys_with_threshold_5,sdk/src/test/resources/sample_vk_7_keys_with_threshold_5),WalletSettings(q7yYYI80978LJF04r501n8Mv58222O2s,Vector(00531e40d4036d808d2101756debc7fd2365cd76da1176f5bf2ed09603d01f711ca03bee9112b03f060a26469b0ba23d87c38dda964cf9a1bb3c7f1edbf8a2d6f8, 0300000060b218f4c543504110af4dd02a72a764d5c1c59beb1e6dc3ea0dd85f86685eade90f567f9937b3a4fc36bb5adfe480ac381ccdbb91f11c269e2a41d71d829fc312cdef48bf75818447405bb86202a1d39f0503491207eab329bb77047805f800000ed74aa4afda5ffb358ba4906d0a5429f9840805e1fcc9ea71fca62afb5e97d779d0d2d3dd1bd9a3563166eff9d12b9ab9b6f11baf5639606689c72e16e2577ee338229e015697c8d174782b4caa04af9902eca2f3c384c6f558cf685b11010084a8436bbb18f7ccd039c249afa69f868c79939923ee3fc3ec437b998246db5b4907881702641ec6b41fdc6de644087713115fa2548121280fe8ac85fe31387aee6465767d8ba2189fab2ee8bf702beda6af018b4d68f890176bf2ab599e000000)))
&#91;2-Hop-akka.actor.default-dispatcher-3] INFO scorex.core.network.NetworkController - Registering handlers for List((1,GetPeers message), (2,Peers message))
&#91;2-Hop-akka.actor.default-dispatcher-2] INFO scorex.core.network.NetworkController - Successfully bound to the port 9084
&#91;main] INFO com.horizen.SidechainApp - calculated sysDataConstant is: b1acb36cdc03885d0f8ebeff5f1b7c21e3719f875a733ed58170621852d45dcd51b96d38e2dbb2002b43c44742d02d1c348280112fa2fe282f01a6a2615d0d33d19b03a0f5337c8ef41c2d84e2db25f56eb88bcf2ed76a094801d2fc83160100
&#91;2-Hop-akka.actor.default-dispatcher-2] INFO scorex.core.network.NetworkController - Registering handlers for List((55,Inv), (22,RequestModifier), (33,Modifier), (65,Sync))
&#91;2-Hop-akka.actor.default-dispatcher-3] INFO com.horizen.consensus.ConsensusDataStorage - Storage with id:1878272466 -- Add stake to consensus data storage: for epochId 8bbc8219cab8b9b1f68513f89d3489bf950635a2ee90192237c9da6ca5fb9c8f stake info: StakeConsensusEpochInfo(rootHash=7006b227eaa723e4afffb404383fd07166959275ec9c8f7e6665314c85ff1b03, totalStake=40000000000)
&#91;2-Hop-akka.actor.default-dispatcher-3] INFO com.horizen.consensus.ConsensusDataStorage - Storage with id:1878272466 -- Add nonce to consensus data storage: for epochId 8bbc8219cab8b9b1f68513f89d3489bf950635a2ee90192237c9da6ca5fb9c8f nonce info: NonceConsensusEpochInfo(consensusNonce=000000005f182866
&#91;main] INFO com.horizen.websocket.WebSocketConnectorImpl - Starting web socket connector...
&#91;main] INFO com.horizen.websocket.WebSocketConnectorImpl - Web socket connector started.
&#91;2-Hop-akka.actor.default-dispatcher-2] INFO com.horizen.certificatesubmitter.CertificateSubmitter - sysDataConstant in Certificate submitter is: b1acb36cdc03885d0f8ebeff5f1b7c21e3719f875a733ed58170621852d45dcd51b96d38e2dbb2002b43c44742d02d1c348280112fa2fe282f01a6a2615d0d33d19b03a0f5337c8ef41c2d84e2db25f56eb88bcf2ed76a094801d2fc83160100
&#91;2-Hop-akka.actor.default-dispatcher-2] INFO com.horizen.certificatesubmitter.CertificateSubmitter - Found proving key file at location: /home/zendoo/Sidechains-SDK/sdk/src/test/resources/sample_proving_key_7_keys_with_threshold_5
&#91;2-Hop-scorex.executionContext-11] INFO com.horizen.certificatesubmitter.CertificateSubmitter - Backward transfer certificate submitter was successfully started.
Simple Sidechain application successfully started...</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Il est possible de véfifier que le node est bien en écoute sur les ports 9084 (SideChain) et 9085 (API) :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ netstat -plnt
(Not all processes could be identified, non-owned process info
 will not be shown, you would have to be root to see it all.)
Active Internet connections (only servers)
Proto Recv-Q Send-Q Local Address           Foreign Address         State       PID/Program name
&#91;...]
tcp6       0      0 127.0.0.1:9084          :::*                    LISTEN      4834/java
tcp6       0      0 127.0.0.1:9085          :::*                    LISTEN      4834/java</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Il est possible d'interagir avec SimpleApp via une API disponible sur le port <strong>9085</strong>. Je vérifie par exemple si je retrouve bien dans la sidechain les informations de la génèse, le Genesis Block :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ curl -X POST "http://127.0.0.1:9085/mainchain/genesisBlockReferenceInfo" -H "accept: application/json"
{
  "result" : {
    "blockReferenceInfo" : {
      "mainchainHeaderSidechainBlockId" : "8bbc8219cab8b9b1f68513f89d3489bf950635a2ee90192237c9da6ca5fb9c8f",
      "mainchainReferenceDataSidechainBlockId" : "8bbc8219cab8b9b1f68513f89d3489bf950635a2ee90192237c9da6ca5fb9c8f",
      "hash" : "04852dabaace286308c2ff9a8ba4968d2e3cf17b7079594b8dff756eb0e0f7f6",
      "parentHash" : "05cca9315c93542fc809fd9e2a22d22f744151406c5cf01a5c97c4b2a10a7579",
      "height" : 221
    }
  }
}</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Voilà, J'ai bien ma sidechain déclarée dans la mainchain, fonctionnelle et répondant via une interface RPC sur le port 9085.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Il est possible de se connecter à la doc de l'API sur cette url : http://127.0.0.1:9085/swagger</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Pour rendre les connexions distantes possibles, il est possible d'éditer la structure restApi de la configuration de la sidechain comme suit :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>restApi {
    bindAddress = "0.0.0.0:9085"
    api-key-hash = ""
  }</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Il est ensuite possible de filtrer les clients par IP à l'aide du firewall, mais on sort du cadre de cet article déjà bien long.</p>
<!-- /wp:paragraph -->

<!-- wp:image {"align":"center","id":377,"sizeSlug":"full"} -->
<div class="wp-block-image"><figure class="aligncenter size-full"><img src="https://mescryptos.fr/wp-content/uploads/2020/07/ZEN-Annotation-2020-07-23-193254.jpg" alt="17e541ca71ffd37d082106e08d86b9e8.png" class="wp-image-377"/></figure></div>
<!-- /wp:image -->

<!-- wp:paragraph -->
<p>A bientôt pour de nouvelles aventures ! Dans le prochain article on transférera des ZEN dans tous les sens.</p>
<!-- /wp:paragraph -->

<!-- wp:heading -->
<h2>Le plan du dossier : Les Sidechains Horizen</h2>
<!-- /wp:heading -->

<!-- wp:list {"ordered":true} -->
<ol><li><a href="https://mescryptos.fr/les-sidechains-horizen-le-noeud-zend-oo/">Zend_oo, le node de la MainChain Horizen</a></li><li>Création de ma première saidechain : Tu es ici :)</li><li><a href="https://mescryptos.fr/les-sidechains-horizen-transfert-de-zen/">Transferts entre chaînes</a></li><li>Bonus : <a href="https://mescryptos.fr/gagner-des-zen-ca-detend/">Gagner des ZEN : Le Faucet en détail</a></li><li>Bonus : <a href="https://mescryptos.fr/ton-node-zen-en-20-minutes-chrono/">Monter un secure node ZEN en 20 minutes</a></li></ol>
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
