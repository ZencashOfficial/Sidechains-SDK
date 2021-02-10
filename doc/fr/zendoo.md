<!-- wp:paragraph -->
<p>Une introduction pratique aux sidechains Horizen.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Les sidechains sont LA nouvelle fonctionnalité de l'écosystème Horizen.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Les sidechains sont indépendantes mais intéragissent avec la mainchain Horizen. Elles permettent à tout un chacun de créer une blockchain à partir d'un modèle, en ne modifiant que ce qui est nécessaire à l'application originale.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Créer une sidechain à partir de la mainchain, c'est un peu comme l'héritage de classe en programmation orientée objet. Tu vas dériver une sidechain à partir d'un modèle fonctionnel, mais tu peux y apporter les modifications qui te sont nécessaires.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>La première étape consiste à disposer d'une version de noeud Horizen disposant de la prise en charge des sidechains.</p>
<!-- /wp:paragraph -->

<!-- wp:heading -->
<h2>Le plan du dossier : Les Sidechains Horizen</h2>
<!-- /wp:heading -->

<!-- wp:list {"ordered":true} -->
<ol><li>Zend_oo, le node de la MainChain Horizen : Tu es ici :)</li><li><a href="https://mescryptos.fr/les-sidechains-horizen-ma-premiere-sidechain/">Création de ma première sidechain</a></li><li><a href="https://mescryptos.fr/les-sidechains-horizen-transfert-de-zen/">Transferts entre chaînes</a></li><li>Bonus : <a href="https://mescryptos.fr/gagner-des-zen-ca-detend/">Gagner des ZEN : Le Faucet en détail</a></li><li>Bonus : <a href="https://mescryptos.fr/ton-node-zen-en-20-minutes-chrono/">Monter un secure node ZEN en 20 minutes</a></li></ol>
<!-- /wp:list -->

<!-- wp:heading -->
<h2 id="zend_oo">Zend_oo</h2>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>Zend_oo est la version beta du prochain processus Zend. Cette version introduit les sidechains et la capacité de la mainchain de les gérer et de communiquer avec elles.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>L'objetif de ce premier tutoriel consiste à installer Zend_oo sur un VPS.</p>
<!-- /wp:paragraph -->

<!-- wp:heading -->
<h2 id="mise-en-place-dun-noeud-zend_oo">Mise en place d'un noeud zend_oo</h2>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>On va commencer par créer un noeud zend_oo, pour avoir un noeud de la future génération à disposition.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>J'ai utilisé à cette fin un VPS Digital Ocean, mais n'importe quel VPS ferait l'affaire. J'ai choisi le plus petit ($5/mois) en Debian 10. Les temps de compilation sont plus longs que sur un monstre, mais ça réduit les coûts du test.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Une fois connecté au VPS, je crée un utilisateur <strong>zendoo</strong> que j'ajoute au groupe <strong>sudo</strong> et je lui affecte un mot de passe :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code># groupadd zendoo
# useradd -g zendoo -m -s /bin/bash zendoo
# adduser zendoo sudo
# passwd zendoo</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>J'ajoute également un peu de swap à ce vps, sinon, la compilation n'arrivera pas au bout par manque de mémoire (et c'est loooong de tout recommencer) :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code># fallocate -l 4G /swapfile
# chmod 600 /swapfile
# mkswap /swapfile
# swapon /swapfile
# echo '/swapfile none swap sw 0 0' >>/etc/fstab</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>C'est prêt ! Je bascule sur l'user zendoo pour la suite et je me place dans son répertoire :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ su zendoo
$ cd </code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Je fais les mises à jour du système et télécharge les prérequis :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ sudo apt-get update
$ sudo apt-get -y upgrade
$ sudo apt-get -y install build-essential pkg-config libc6-dev m4 g++-multilib autoconf libtool ncurses-dev unzip git python zlib1g-dev bsdmainutils automake curl wget</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Je clone le dépôt des sources et compile <strong>zend_oo </strong>:</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ git clone https://github.com/ZencashOfficial/zend_oo.git
$ cd zend_oo
$ ./zcutil/build.sh -j$(nproc)</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Je télécharge les fichiers nécessaires. Je n'ai pas approfondi ce que faisait cette étape pour être honnête :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ ./zcutil/fetch-params.sh</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>En lançant zend une première fois, il va créer le fichier de configuration :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ ./src/zend</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Je l'édite pour spécifier que je souhaite utiliser le testnet et je relance :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ sed -i -e "s/#testnet=0/testnet=1/" ~/.zen/zen.conf
$ ./src/zend</code></pre>
<!-- /wp:code -->

<!-- wp:image {"align":"center","id":338,"sizeSlug":"full"} -->
<div class="wp-block-image"><figure class="aligncenter size-full"><img src="https://mescryptos.fr/wp-content/uploads/2020/07/unknown.png" alt="" class="wp-image-338" title=""/></figure></div>
<!-- /wp:image -->

<!-- wp:paragraph -->
<p>Voilà, je dispose de mon propre node zend_oo ! Je peux le quitter avec la combinaison de touches <strong>Ctrl+C</strong>, comme indiqué.</p>
<!-- /wp:paragraph -->

<!-- wp:heading -->
<h2>Libérer de l'espace disque</h2>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>Le répertoire des sources et les binaires intermédiaires prennent énormément de place sur le disque. Si comme moi, tu utilises un petit vps pour les tests, je te recommande de supprimer tout cela.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Je crée un répertoire zen dans le répertoire personnel de l'utilisateur, j'y déplace les binaires et je supprime le répertoire <strong>zend_oo</strong> :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ mkdir zen
$ mv zend_oo/src/zen-* ./zen/
$ mv zend_oo/src/zend ./zen/
$ rm zend_oo/ -rf</code></pre>
<!-- /wp:code -->

<!-- wp:heading -->
<h2>Ajout au PATH</h2>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>Pour une utilisation plus facile des binaires ZEN, j'ajoute le nouveau répertoire au PATH en ajoutant le contenu suivant au fichier <code>~/.bashrc</code> :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code># ZEN binaries Path
export PATH=~/zen:$PATH</code></pre>
<!-- /wp:code -->

<!-- wp:heading -->
<h2 id="bootstrap-et-mode-non-interactif">Mode non interactif</h2>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>Le principe du node, c'est aussi de ne pas avoir à rester connecté à le regarder tourner. On peut donc le lancer en mode non interactif, en daemon (ou démon par abus de langage).</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Cela se fait en lançant <code>zend -daemon</code> au lieu de <code>zend</code> tout court ou, de façon plus pérenne, en ajoutant une ligne au fichier de configuration (<code>~/.zen/zen.conf</code>) :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>daemon=1</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>En procédant ainsi, il n'est plus possible d'arrêter zend par un <code>Ctrl+C</code>. Il faut utiliser zen-cli poru le stopper (<code>zen-cli stop</code>). Et/ou, récupérer le PID de zend dans la liste des processus et lui envoyer un kill :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ zend
Zen server starting
$ zen-cli stop
Zen server stopping</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>ou :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ ps x
  PID TTY      STAT   TIME COMMAND
 1492 ?        SLsl   4:29 zend
15373 pts/0    S      0:00 bash
15376 pts/0    R+     0:00 ps x
$ kill 1492</code></pre>
<!-- /wp:code -->

<!-- wp:heading -->
<h2>Bootstrap</h2>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>Pour accélérer la synchronisation du node, on va utiliser un bootstrap. <strong>Précision utile toutefois</strong> : pour l'instant la synchronisation au testnet n'est pas nécessaire pour les sidechains. On utilisera poru l'instant <strong>Zend_oo </strong>en mode  tests de régression (argument <code>-regtest</code> ou <code>regtest=1</code> dans le fichier de configuration)... Je laisse toutefois la méthode ici, elle servira plus tard.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Pour ma part, j'ai stoppé zend et supprimé tout le contenu du répertoire <code>~/.zen/testnet3</code> .  Les bootstraps sont à récupérer ici : <a href="https://bootstraps.ultimatenodes.io/">https://bootstraps.ultimatenodes.io/</a> et plus précisément dans le cas qui m'intéresse : <a href="https://bootstraps.ultimatenodes.io/sc/">https://bootstraps.ultimatenodes.io/sc/</a>. On récupère le lien vers le bootstrap du jour. On le décompresse et on supprime l'archive. Par exemple :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ cd ~/.zen/testnet3
$ wget https://bootstraps.ultimatenodes.io/horizen/masternode-sc/horizen_masternode_sidechains_blockchain_2020-07-07_06-00-01_UTC.tar.gz
$ tar xvzf horizen_masternode_sidechains_blockchain_2020-07-07_06-00-01_UTC.tar.gz
$ rm horizen_masternode_sidechains_blockchain_2020-07-07_06-00-01_UTC.tar.gz
</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Comme recommandé, on ajoute <code>txindex=1</code> au fichier de configuration.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>On peut alors lancer <code>zend</code>.</p>
<!-- /wp:paragraph -->

<!-- wp:heading -->
<h2>Tests</h2>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>Si la ligne <code>daemon=1</code> a été ajoutée au fichier de configuration, le node se lance tout bêtement en tapant <code>zend</code>.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>On peut surveiller ce que fait le daemon dans les fichiers <code>debug.log</code> du sous répertoire <code>zen/testnet</code> et interagir avec lui grâce au client <strong>zen-cli</strong> ou via des appels RPC. Par exemple :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>$ zen-cli getinfo
$ zen-cli getblockchaininfo
...</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Au lancement, le node peut ne pas répondre correctement :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>error code: -28
error message:
Loading block index...</code></pre>
<!-- /wp:code -->

<!-- wp:paragraph -->
<p>Il faut attendre que cette première étape soit terminée avant d'avoir un node réactif. Une fois terminé, <code>zen-cli getinfo</code> retourne des infos de ce type :</p>
<!-- /wp:paragraph -->

<!-- wp:code -->
<pre class="wp-block-code"><code>{
  "version": 2010002,
  "protocolversion": 170003,
  "walletversion": 60000,
  "balance": 0.00000000,
  "blocks": 669612,
  "timeoffset": 0,
  "connections": 2,
  "proxy": "",
  "difficulty": 14.49267470145953,
  "testnet": true,
  "keypoololdest": 1594157359,
  "keypoolsize": 101,
  "paytxfee": 0.00000000,
  "relayfee": 0.00000100,
  "errors": ""
}
</code></pre>
<!-- /wp:code -->

<!-- wp:heading -->
<h2>Le plan du dossier : Les Sidechains Horizen</h2>
<!-- /wp:heading -->

<!-- wp:list {"ordered":true} -->
<ol><li>Zend_oo, le node de la MainChain Horizen : Tu es ici :)</li><li><a href="https://mescryptos.fr/les-sidechains-horizen-ma-premiere-sidechain/">Création de ma première sidechain</a></li><li><a href="https://mescryptos.fr/les-sidechains-horizen-transfert-de-zen/">Transferts entre chaînes</a></li><li>Bonus : <a href="https://mescryptos.fr/gagner-des-zen-ca-detend/">Gagner des ZEN : Le Faucet en détail</a></li><li>Bonus : <a href="https://mescryptos.fr/ton-node-zen-en-20-minutes-chrono/">Monter un secure node ZEN en 20 minutes</a></li></ol>
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
