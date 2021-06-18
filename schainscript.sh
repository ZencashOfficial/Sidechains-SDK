
#Known Issues
#Datadir is not working but implemented in script
#Not everything in config is working yet but most should work. Everything thats tested not working is marked





#!/bin/bash
set -e

#Mostly tested(ive tested it for the most part but should be checked)
#-------------------------------------------------------------#
configfile=mysetting.conf
submitterisenabled=true

nodename="testNode1"
agentname="2-Hop"

connectiontimeout="100 milliseconds"
reconnectiondelay="1 seconds"
reconnectionmaxattempts=1
#-------------------------------------------------------------#

#IMPLEMENTED BUT NOT TESTED
#-------------------------------------------------------------#
#custom data dir for zend/zen-cli leave empty else
#uses -datdir argument.
zendatadir=
#only used to delete regtest folder
mainzendatafolder=$HOME/.zen

#NOT IMPLEMENTED SETTINGS
#-------------------------------------------------------------#
#restApi
apikeyhash=""


#network

knownpeers=[]

#withdrawalEpochCertificate
signersThreshold=5
provingKeyFilePath=""
verificationKeyFilePath=""
#-------------------------------------------------------------#

#CHANGABLE INSIDE PROGRAM
#-------------------------------------------------------------#

#zen network. leave blank if you want mainnet
network=-regtest

seed="seed1"
initamount=1
withdrawepochlength=10
websocketip="localhost:8888"
rpcip="127.0.0.1:9085"
netbindaddress="127.0.0.1:9084"

#where sidechain data should reside
scdatadir=/tmp/scorex/data/blockchain
sclogdir=/tmp/scorex/data/log
#zend and zen-cli path. /usr/bin/ for precompiled
zenpath=/usr/bin/

#NOT WORKING
#custom data dir for zend/zen-cli leave empty else
#uses -datdir argument
zendatadir=


#folder where your sdk is located
sdkfolder=$HOME/Sidechains-SDK

#folder where your sdkconfig is going to end up
sdkconfigfolder=$sdkfolder/examples/simpleapp/src/main/resources

#folder for scripts and savedata
logs=scriptdata
function usage()
{
  echo "Options: \n
  -h, --help                  Display this help message and exit. \n
  -c, --config configpath     Custom config file path \n
    Where 'configpath' is the path to the script config file. \n
  -l, --logdir logpath        Custom log folder path \n
    Where 'logpath' is the path to logdir. "
}

while [[ -n "$1" ]]; do
  case "$1" in
    -h | --help)
      usage
      exit 0
      ;;
    -c | --config)
      echo "Using provided configfile"
      shift; configpath="$1"
      if [[ ! -e "$configpath" ]];then
          print "Config does not exist. configpath=$configpath"
          exit 1
      fi
      . "$configpath"
      ;;
    -l | --logdir)
      echo "log folder path"
      shift; logs="$1"
      ;;
    -* | --*)
    usage
    echo "Unknown option $1"
    exit 1
      ;;
  esac
  shift

done
#-------------------------------------------------------------#
#non changable inside program
#-------------------------------------------------------------#
jsontemplateurl="https://gist.githubusercontent.com/ponta9/76cda76b4576dc9e0152835afdee3564/raw/4280c4a0e0d1ef73735d81f9581bf532b7d7cfe0/template.conf"



#jar file name before version
boostrapfilename=sidechains-sdk-scbootstrappingtools-
sdkappfilename=sidechains-sdk-simpleapp-



#Path to bootstraptool
bootstraptool=$sdkfolder/tools/sctool/target
#-------------------------------------------------------------#

declare -A KEYS

declare -A log_type=([PRINT]=0 [READ]=1 [COMMAND]=2 [ERROR]=3 [RAWOUTPUT]=4 [JQOUTPUT]=5 )

function main()
{
  
  
  mkdir -p "$logs"
  to_log "mkdir -p $logs" "COMMAND"

  #check if right programs are installed
  #-------------------------------------------------------------#
  for name in curl sleep grep tr sed screen jq mvn java; do
    if ! hash "$name" >/dev/null 2>&1; then
      to_log "ERROR:Please install the program: $name" "ERROR"
      print "ERROR:Please install the program: $name"
      exit 1
    fi
  done
  #-------------------------------------------------------------#

  asciiart

  #Show config and ask user if they want to change something
  #will run until user is satisfied
  #-------------------------------------------------------------#
  configchecked=false
  while [[ "$configchecked" == false ]]; do
    print "Your config looks like this: "
    print_config

    print "---------------------------"
    print "Is this information correct?"

    promt
    #if information is not correct run change values
    #else exit while loop
    if [[ ! $REPLY =~ ^[yY]$ ]]; then
      change_config
    else
      configchecked=true
    fi
  done
  #-------------------------------------------------------------#

  #if not -testnet or -regtest network then warn user that this program will spend zen
  #-------------------------------------------------------------#
  if [[ -z "$network" ]]; then
    print "---------------------------"
    print "Seems like you are running on mainnet"
    print "WARNING this program will spend $initamount zen and send it to sidechain"
    print "Are you sure you want to send $initamount zen to sidechain?"

    promt  
    #if you dont want to send quit
    if [[ ! $REPLY =~ ^[yY]$ ]]; then
      print "Exiting application now"
      exit 1
    fi
    print "---------------------------"
  fi
  #-------------------------------------------------------------#

  checkconfig

  #if sidechain data exists ask if user wants to use it to skip to step 9
  #-------------------------------------------------------------#
  if [[ -e $logs/sidechainkeys.keys ]];then
    print "----------------------------------------------------"
    print "Seems like you got cut off after somewhere after step 8"
    print ""
    print "Do you want to run the saved data from $logs/sidechainkeys.keys?"
    print "This will skip to step 9"
    print "If you respond no the saved data will be overwritten"

    promt
    #if yes then start configure sidechain 
    if [[ $REPLY =~ ^[yY]$ ]]; then
      to_log "source $logs/sidechainkeys.keys" "COMMAND"
      source "$logs"/sidechainkeys.keys
	  configuresidechain
	  exit 0
	fi
  fi

  declaresidechain
  

  #step 9-12
  configuresidechain

  #-------------------------------------------------------------#


}

function islatestzend()
{
	
	localzendv=$("$zenpath"/./zend --version | grep version | cut -d' ' -f4 | cut -f1,2 -d'-' )
	to_log "localzendv= $localzendv" "RAWOUTPUT"
	latestzendv=$(curl -s "https://api.github.com/repos/HorizenOfficial/zend_oo/releases/latest" | jq -r '.tag_name')
	to_log "latestzendv= $latestzendv" "RAWOUTPUT"
	
	if [[ "$localzendv" != "$latestzendv" ]]; then
		to_log "ERROR: Your zend_oo version='$localzendv' does not match latest zend_oo version='$latestzendv'" "ERROR"
	    print "ERROR: Your zend_oo version='$localzendv' does not match latest zend_oo version='$latestzendv'"
		exit 1
	fi
}

function versiontests()
{
	mavenversion=$(mvn -version | grep Apache | sed -n 's/.*\([Mm]aven[[:space:]]*\)\([0-9.]*\).*/\2/p')
	to_log "mavenversion= $mavenversion" "RAWOUTPUT"
    mavenjavaversion=$(mvn -version | sed -n 's/.*\([Jj]ava version[[:space:]:"]*\)\([0-9.]*\).*/\2/p')
	to_log "mavenjavaversion= $mavenjavaversion" "RAWOUTPUT"
	#if maven version less then 3.0.0
	if [[ ! "$mavenversion" =~ ^[3-9].* ]]; then
        to_log "ERROR:Maven version needs to be over 3+ yours was='$mavenversion' " "ERROR"
		print "ERROR:Maven version needs to be over 3+ yours was='$mavenversion'"
	fi
	
	#before java 9 version number is shown as 1.X where x is version
	#if java version in maven is of type 1.X
	if [[ -z "$mavenjavaversion" || "$mavenjavaversion" =~ ^[1][.].* ]]; then
		
		#if java version is less than 8
		if [[ ! "$mavenjavaversion" =~ ^[1][.][8-9].* ]]; then
			to_log "ERROR: Maven java version needs to be 8+ yours is='$mavenjavaversion'" "ERROR"
			print "ERROR: Maven java version needs to be 8+ yours is='$mavenjavaversion'"
			exit 1
		fi
		
	fi
	
	
	#maven downloads stuff sometimes first run and botches output so we run it twice
	localprojectv=$(cd "$sdkfolder" && echo '${project.version}' | mvn help:evaluate | grep -v '^[[]')
	localprojectv=$(cd "$sdkfolder" && echo '${project.version}' | mvn help:evaluate | grep -v '^[[]')
	
	latestprojectv=$(curl -s "https://api.github.com/repos/HorizenOfficial/Sidechains-SDK/releases/latest" | jq -r '.tag_name')

	if [[ "$localprojectv" != "$latestprojectv" ]]; then
		to_log "ERROR: Your sdk project version='$localprojectv' does not match latest version='$latestprojectv'" "ERROR"
		print "ERROR: Your sdk project version='$localprojectv' does not match latest version='$latestprojectv'"
		exit 1
	fi
	
	bootstrapjar="$boostrapfilename""$latestprojectv".jar
	sdkappjar="$sdkappfilename""$localprojectv".jar

	#Check if updated boostrap tool or sdkapp exists
	if [[ ! -e "$bootstraptool/$bootstrapjar" || ! -e "$sdkfolder/examples/simpleapp/target/$sdkappjar" ]]; then
		print "Sdkapp or boostrap is not up to date. running a maven build"
		print "This will take a while depending on your system speed"
			
		#if maven clean package fails
		if ! mvncleanpackage >/dev/null 2> >(to_log); then
			to_log "ERROR: Maven build failed please check logs" "ERROR"
			print "ERROR: Maven build failed please check logs"
			exit 1
		fi
		
	fi
	
	#Check if boostrap tool exists
	if [[ ! -e "$bootstraptool/$bootstrapjar" ]]; then
		to_log "ERROR:Boostrap tool is not in the provided path $bootstraptool/$bootstrapjar" "ERROR"
		print "ERROR:Boostrap tool is not in the provided path $bootstraptool/$bootstrapjar"
		exit 1
	fi
 
	#Check if sdkapp exists
	if [[ ! -e "$sdkfolder/examples/simpleapp/target/$sdkappjar" ]];then
		to_log "ERROR:Sdkapp is not in the provided path $sdkfolder/examples/simpleapp/target/$sdkappjar" "ERROR"
		print "RROR:Sdkapp is not in the provided path $sdkfolder/examples/simpleapp/target/$sdkappjar"
		exit 1
	fi
	
	  #Check if sdkconfigfolder exists
	if [[ ! -d "$sdkconfigfolder" ]]; then
		to_log "ERROR:sdkconfigfolder does not exist= $sdkconfigfolder" "ERROR"
		print "ERROR:sdkconfigfolder does not exist= $sdkconfigfolder"
		exit 1
	fi
	
}


function mvncleanpackage()
{
	OUT=$(cd "$sdkfolder" && mvn clean package)
	local returnvalue="$?"
	
	to_log "$OUT" "RAWOUTPUT"
	return "$returnvalue"
}
function asciiart()
{
  print "---------------------------------------------------------------------------------------------" "\033[32m"
  print "  ____  _     _           _           _                         _          _                 " "\033[32m"
  print " / ___|(_) __| | ___  ___| |__   __ _(_)_ __   __ _ _ __  _ __ | |__   ___| |_ __   ___ _ __ " "\033[32m"
  print " \___ \| |/ _\` |/ _ \/ __| '_ \ / _\` | | '_ \ / _\` | '_ \| '_ \| '_ \ / _ \ | '_ \ / _ \ '__|" "\033[32m"
  print "  ___) | | (_| |  __/ (__| | | | (_| | | | | | (_| | |_) | |_) | | | |  __/ | |_) |  __/ |   " "\033[32m"
  print " |____/|_|\__,_|\___|\___|_| |_|\__,_|_|_| |_|\__,_| .__/| .__/|_| |_|\___|_| .__/ \___|_|   " "\033[32m"
  print "                                                   |_|   |_|                |_|              " "\033[32m"
  print "---------------------------------------------------------------------------------------------" "\033[32m"
}


function checkconfig()
{
  #Check if zend is in provided path
  if ! command -v "$zenpath"/./zend >/dev/null 2>&1; then
    to_log "ERROR:zend cant be found in zenpath=$zenpath" "ERROR"
    print "ERROR:zend cant be found in zenpath=$zenpath"
    exit 1
  fi

  #Check if zen-cli is in provided path
  if ! command -v "$zenpath"/./zen-cli >/dev/null 2>&1; then
    to_log "ERROR:zen-cli cant be found in zenpath=$zenpath" "ERROR"
    print "ERROR:zen-cli cant be found in zenpath=$zenpath"
    exit 1
  fi

   #Check if sdkfolder exists
  if [[ ! -d "$sdkfolder" ]]; then
    to_log "ERROR:sdkfolder does not exist= $sdkfolder" "ERROR"
    print "ERROR:sdkfolder does not exist= $sdkfolder"
    exit 1
  fi
  
  if [[ -n "$network" && "$network" != "-testnet" && "$network" != "-regtest" ]]; then
    to_log "ERROR:you are running a unsupported network. the supported types are '-regtest','-testnet',' ' and your network is=$network" "ERROR"
    print "ERROR:you are running a unsupported network. the supported types are '-regtest','-testnet',' ' and your network is=$network"
    exit 1
  fi

  if [[ ! -z $zendatadir ]]; then
      mainzendatafolder="$zendatadir"
      zendatadir="-dataDir=$zendatadir"
  fi
  
  islatestzend
  
  versiontests
  

}


function declaresidechain()
{
  #force stop zen if its running
  print "----------------------------------------------------"
  print "Force stopping zend"
  print "----------------------------------------------------"
  cli stop >/dev/null 2> >(to_log) | true
  
  #Asks if user really wants to delte sidechain data
  #-------------------------------------------------------------#
  print "Do you want to delete old sidechain data?"
  promt
  if [[ $REPLY =~ ^[yY]$ ]]; then
    print ""
    print "Are you sure you want to delete data in: $scdatadir and $sclogdir and $mainzendatafolder/regtest?"
    promt
    if [[ $REPLY =~ ^[yY]$ ]]; then
      print "Deleting old sidechain data"
      rm -rf "$scdatadir"
      rm -rf "$sclogdir"
      rm -rf "$mainzendatafolder"/regtest
    fi
  fi 
  #-------------------------------------------------------------#
  
  #Running Step 3
  #-------------------------------------------------------------#
  print "----------------------------------------------------"
  print "Running step 3"
  print "Generating publicKey and secret"
  
  bootstrap generatekey "{\"seed\":\"$seed\"}" 2> >(exit_log)

  #save keys and log
  KEYS[PUBKEY]=$(echo "$OUT" | jq '.publicKey')
  to_log "${KEYS[PUBKEY]}" "JQOUTPUT" "KEYS[PUBKEY]"
  KEYS[SECRET]=$(echo "$OUT" | jq '.secret')
  to_log "${KEYS[SECRET]}" "JQOUTPUT" "KEYS[SECRET]"

  print "Generating VRFpublickey and VRFsecret"

  bootstrap generateVrfKey "{\"seed\":\"$seed\"}" 2> >(exit_log)
  
  #save keys and log
  KEYS[VRFPUBKEY]=$(echo "$OUT" | jq '.vrfPublicKey')
  to_log "${KEYS[VRFPUBKEY]}" "JQOUTPUT" "KEYS[VRFPUBKEY]"
  KEYS[VRFSECRET]=$(echo "$OUT" | jq '.vrfSecret')
  to_log "${KEYS[VRFSECRET]}" "JQOUTPUT" "KEYS[VRFSECRET]"
  #-------------------------------------------------------------#

  #Running Step 4
  #-------------------------------------------------------------#
  print "----------------------------------------------------"
  print "Running step 4"
  print "Genereting Proofinfo"

  bootstrap generateProofInfo "{\"seed\":\"$seed\",\"keyCount\":7,\"threshold\":5}" 2> >(exit_log)
  
  #save keys and log
  KEYS[THRESHOLD]=$(echo "$OUT" | jq '.threshold')
  to_log "${KEYS[THRESHOLD]}" "JQOUTPUT" "KEYS[THRESHOLD]"
  KEYS[VERIFYKEY]=$(echo "$OUT" | jq '.verificationKey')
  to_log "${KEYS[VERIFYKEY]}" "JQOUTPUT" "KEYS[VERIFYKEY]"
  KEYS[GENSYSCONST]=$(echo "$OUT" | jq '.genSysConstant')
  to_log "${KEYS[GENSYSCONST]}" "JQOUTPUT" "KEYS[GENSYSCONST]"
  KEYS[SCHPUBKEYS]=$(echo "$OUT" | jq -c '[.schnorrKeys | .[].schnorrPublicKey]')
  to_log "${KEYS[SCHPUBKEYS]}" "JQOUTPUT" "KEYS[SCHPUBKEYS]"
  KEYS[SCHSECRETS]=$(echo "$OUT" | jq -c '[.schnorrKeys | .[].schnorrSecret]')
  to_log "${KEYS[SCHSECRETS]}" "JQOUTPUT" "KEYS[SCHSECRETS]"

  #-------------------------------------------------------------#

  #Running Step 5
  #-------------------------------------------------------------#
  print "----------------------------------------------------"
  print "Running step 5"

  #if zend gives error stop
  if ! zendstart 2> >(exit_log); then
    to_log "ERROR: Failed to start zend. Check the error message above" "ERROR"
    print "ERROR: Failed to start zend. Check the error message above"
    exit 1
  fi

  print "Trying to call zen-cli"
  #wait for zend to talk to cli
  until cli getinfo >/dev/null 2> >(to_log); do
    print "Waiting for Zend to start. Trying again in 10 seconds"
    print "If you get stuck here then zend probably failed somewhere after starting"
    sleep 10
  done
  #-------------------------------------------------------------#

  #Running Step 6. Skip if not on regtesxt
  #-------------------------------------------------------------#
  print "----------------------------------------------------"
  if [[ "$network" == "-regtest" ]]; then
    print "Running step 6"
    print "Genereting 220 blocks"
    cli generate 220
  else 
    print "Skipping Step 6 because network is not -regtest"
  fi
  #-------------------------------------------------------------#

  #Running Step 7
  #-------------------------------------------------------------#
  print "----------------------------------------------------"
  print "Running step 7"
  print "Declaring a Sidechain in Mainchain node using keys" 
  
 
  cli sc_create "$withdrawepochlength" "${KEYS[PUBKEY]}" "$initamount" "${KEYS[VERIFYKEY]}" "${KEYS[VRFPUBKEY]}" "${KEYS[GENSYSCONST]}" 2> >(exit_log)
  
  #save keys
  KEYS[TXID]=$(echo "$OUT" | jq '.txid' )
  to_log "${KEYS[TXID]}" "JQOUTPUT" "KEYS[TXID]"
  KEYS[SCID]=$(echo "$OUT" | jq '.scid' )
  to_log "${KEYS[SCID]}" "JQOUTPUT" "KEYS[SCID]"
  
  #backup keys in a file if program should crash
  saveallkeys
  #-------------------------------------------------------------#


  #Running Step 8
  #-------------------------------------------------------------#
  print "----------------------------------------------------"
  print "Running Step 8"

  #if regtest generate 1 more block
  if [[ "$network" == "-regtest" ]]; then
    print "Generating 1 block"
    cli generate 1
  fi
  #-------------------------------------------------------------#

}

function configuresidechain()
{
  #Running step 9
  #-------------------------------------------------------------#
  print "----------------------------------------------------"
  print "Running step 9"

  #Check if txid exists
  if ! cli gettransaction "${KEYS[TXID]}" 2> >(exit_log);then
    print "ERROR: txid cant be found. Something have gone wrong"
    exit 1
  fi
  
  #Wait for transaction to be included in mainchain block
  mytest=$(echo "$OUT" | jq '.blockindex' );
  while [[ "$mytest" == "null" ]]; do
      print "Sidechain not included in mainchain yet. This can take a while"
      print "Waiting 60 seconds and checking again"
      sleep 60
      cli gettransaction "${KEYS[TXID]}" 2> >(exit_log)
      mytest=$(echo "$OUT" | jq '.blockindex' )
  done;


  print "Get information from mainchain for forming genesis Sidechain block"
  cli getscgenesisinfo "${KEYS[SCID]}" 2> >(exit_log)

  #Save key and log
  KEYS[GENESISBIN]="$OUT"
  to_log "${KEYS[GENESISBIN]}" "JQOUTPUT" " KEYS[GENESISBIN]"
  #-------------------------------------------------------------#

  #Running step 10
  #-------------------------------------------------------------#
  print "----------------------------------------------------"
  print "Running Step 10"
  print "Forming Sidechain genesis block in hex string form by Bootstraping tool"

  bootstrap genesisinfo "{\"info\":\"${KEYS[GENESISBIN]}\", \"secret\": ${KEYS[SECRET]}, \"vrfSecret\": ${KEYS[VRFSECRET]}})" 2> >(exit_log)

  #Save key and log
  KEYS[GENESISINFO]=$(echo "$OUT" | jq -c -R -r '. as $line | try fromjson catch $line')
  to_log "${KEYS[GENESISINFO]}" "JQOUTPUT" "KEYS[GENESISINFO]"
  #-------------------------------------------------------------#

  #Running step 11
  #-------------------------------------------------------------#
  print "----------------------------------------------------"
  print "Running Step 11"
  print "Updating Sidechain configuration file"
  sdkconfig
  #-------------------------------------------------------------#

  #Running step 12
  #-------------------------------------------------------------#
  print "----------------------------------------------------"
  print "Configuration is done. Program will now try to start sdkapp and its automatic forger" "\033[32m"
  print "----------------------------------------------------"

  print "Do you want to start the Sidechain?"
  promt
  if [[ $REPLY =~ ^[yY]$ ]]; then
    print "Opening a new scren with name Scapp and running schainapp there"
    print "screen -dmS Scapp java -cp $sdkfolder/./examples/simpleapp/target/$sdkappjar:$sdkfolder/./examples/simpleapp/target/lib/* com.horizen.examples.SimpleApp $sdkconfigfolder/./$configfile"
  
    screen -dmS Scapp java -cp "$sdkfolder"/./examples/simpleapp/target/"$sdkappjar":"$sdkfolder"/./examples/simpleapp/target/lib/* com.horizen.examples.SimpleApp "$sdkconfigfolder"/./"$configfile" 2> >(exit_log)
    print "to access the screen run the command screen -r Scapp"
    #-------------------------------------------------------------#

    #Starting forger
    #-------------------------------------------------------------#
    print "Do you want to start the forger?"

    promt
    if [[ $REPLY =~ ^[yY]$ ]]; then
      print "Starting forger with curl"
      sleep 10
      to_log 'curl -X POST "http://"$rpcip"/block/startForging" -H "accept: application/json" ' "COMMAND"
      curl -X POST "http://$rpcip/block/startForging" -H "accept: application/json" 2> >(exit_log)
    fi
  
    print ""
    print "Do you want to open the screen with?"

    promt
    if [[ $REPLY =~ ^[yY]$ ]]; then
      screen -r Scapp 2> >(exit_log)
    fi
  fi

  
  print "All done"
  #-------------------------------------------------------------#
}

#Save keys to config for later use
function saveallkeys()
{
  declare -p KEYS > "$logs"/sidechainkeys.keys
}

#Run boostrap tool
function bootstrap() 
{
  #args
  #$1 = bootstrap command
  #$2 = param
  #end
  to_log "java -jar $bootstraptool/$bootstrapjar $1 $2" "COMMAND"
  OUT=$(java -jar "$bootstraptool"/"$bootstrapjar" "$1" "$2")
  to_log "$OUT" "RAWOUTPUT"
}

function zendstart()
{
  #return code if it succeded or failed
  local returnvalue=
  #If network is empty then dont run zend with it
  if [[ -z $network ]]; then

    to_log "$zenpath/./zend -websocket -daemon $zendatadir " "COMMAND"
    "$zenpath"/./zend -websocket -daemon $zendatadir
    returnvalue="$?"

  else

    to_log "$zenpath/./zend $network -websocket -daemon $zendatadir " "COMMAND"
    "$zenpath"/./zend "$network" -websocket -daemon $zendatadir
    returnvalue="$?"

  fi
  #to_log "$OUT" "RAWOUTPUT"
  return "$returnvalue"
}

function cli()
{
  to_log "$zenpath/./zen-cli $network $zendatadir $*" "COMMAND"
  OUT=$(eval "$zenpath"/./zen-cli "$network" "$zendatadir" "$@" )
  local returnvalue="$?"

  to_log "$OUT" "RAWOUTPUT"
  return "$returnvalue"
}

function print()
{
  local printvalue=$1
  local color=$2

  #if printvalue exist then use it
  if [[ -z "$color" ]]; then
    echo "$printvalue"
  else 
    echo -e "$color" "$printvalue" "\033[0m"
  fi

  to_log "$printvalue" "PRINT"
}

#Read input from user and print promt
function promt()
{
  read -rp "[y/n]:> "
  
  to_log "$REPLY" "READ"
}

#Prints config values
function print_config()
{

  print "Your seed=$seed"
  print "Inital amount of zen to send to sidechain=$initamount"
  print "withdrawepochlength=$withdrawepochlength"
  print "Using network=$network"
  print "Sidechain rpcip=$rpcip"
  print "Sidechain network bindadress=$netbindaddress"
  print "websocketip for zend=$websocketip"
  print "zenpath=$zenpath"
  print "The folder containing your sdk=$sdkfolder"
  print "The folder that will store your config=$sdkconfigfolder"
  print "The name of the config you want to create=$configfile"
  print "The folder that will store your sidechain data=$scdatadir"
  print "The folder that will store your sidechain log=$sclogdir "
  print "WARNING this will overwrite the any exisiting file named $configfile in $sdkconfigfolder"
  print " "
  #less important variables
  print "Name of your node is=$nodename"
  print "The agent name is=$agentname"
  print "Websocket connectionTimeout is=$connectiontimeout"
  print "Websocket reconnectionDelay is=$reconnectiondelay"
  print "Websocket reconnectionMaxAttempts is=$reconnectionmaxattempts"
}

#Let user specify config
function change_config()
{
      print "Change the values you would like to. Default value press Enter"
      print "--------------------------------------------------------------"
      read -e -r -p "seed=" -i "$seed" seed
      read -e -r -p "initamount=" -i "$initamount" initamount
      read -e -r -p "withdrawepochlength=" -i "$withdrawepochlength" withdrawepochlength
      read -e -r -p "network=" -i "$network" network
      read -e -r -p "rpcip=" -i "$rpcip" rpcip
      read -e -r -p "netbindaddress=" -i "$netbindaddress" netbindaddress
      read -e -r -p "websocketip=" -i "$websocketip" websocketip
      read -e -r -p "zenpath=" -i "$zenpath" zenpath
      read -e -r -p "sdkfolder=" -i "$sdkfolder" sdkfolder
      read -e -r -p "sdkconfigfolder=" -i "$sdkconfigfolder" sdkconfigfolder
      read -e -r -p "configfile=" -i "$configfile" configfile
      read -e -r -p "scdatadir=" -i "$scdatadir" scdatadir
      read -e -r -p "sclogdir=" -i "$sclogdir" sclogdir
      read -e -r -p "bootstraptool=" -i "$bootstraptool" bootstraptool

      read -e -r -p "zendatadir=" -i "$zendatadir" zendatadir
      read -e -r -p "nodename=" -i "$nodename" nodename

      read -e -r -p "submitterisenabled=" -i "$submitterisenabled" submitterisenabled
      read -e -r -p "agentname=" -i "$agentname" agentname
      read -e -r -p "connectiontimeout=" -i "$connectiontimeout" connectiontimeout
      read -e -r -p "reconnectiondelay=" -i "$reconnectiondelay" reconnectiondelay
      read -e -r -p "reconnectionmaxattempts=" -i "$reconnectionmaxattempts" reconnectionmaxattempts

      print "--------------------------------------------------------------"
}



#log handler. Pass values you expect to give errors or Normal text
function to_log()
{
  local message=$1
  local type=$2
  local jqtype=$3
  
  if [[ -z "$message" && -z "$type" ]]; then

    while read -r message; do
      log "$message" "ERROR"
    done
  else

    log "$message" "$type" "$jqtype" 
  fi
}

#logs errors to the right log file
function log()
{
  local message=$1
  local type=$2
  local jqtype=$3
  if [[ ${log_type[$type]} -le ${log_type[COMMAND]} ]];then 
    echo "$type: $message" >> "$logs"/nooutput.log
  fi

  if [[ ${log_type[$type]} -le ${log_type[RAWOUTPUT]} ]];then
    echo "$message" >> "$logs"/rawoutputs.log
  fi

  if [[ ${log_type[$type]} -eq ${log_type[JQOUTPUT]} ]];then
    echo "$jqtype: $message" >> "$logs"/jqoutput.log
  fi
}

#log handler for commands we dont expect to give errors and then exit after printing and logging.
function exit_log()
{
  local message
  if [[ -z "$message" && -z "$type" ]]; then

    while read -r message; do
      log "$message" "ERROR"
      echo "$message"
    done
  fi

}

#Updates the template with new values from user
function sdkconfig()
{
  to_log "rm -f $sdkconfigfolder/$configfile" "COMMAND"
  rm -f "$sdkconfigfolder"/"$configfile"
  
  template=$(curl -s "$jsontemplateurl")
  
  template=$(jq '.scorex.dataDir = "'$scdatadir'" ' <<< "$template")
  template=$(jq '.scorex.logDir = "'$sclogdir'" ' <<< "$template")
  template=$(jq '.scorex.restApi.bindAddress = "'$rpcip'" ' <<< "$template")
  template=$(jq '.scorex.websocket.address = "ws://'$websocketip'" ' <<< "$template")
  template=$(jq '.scorex.websocket.zencliCommandLine = "'$zenpath'/zen-cli" ' <<< "$template")
  if [[ ! -z $zendatadir ]]; then
        template=$(jq '.scorex.websocket.zencliCommandLineArguments = "'$zendatadir'" ' <<< "$template")
  fi
  template=$(jq --argjson schpubkeys ${KEYS[SCHPUBKEYS]} '.scorex.withdrawalEpochCertificate.signersPublicKeys = $schpubkeys '<<< "$template")
  template=$(jq --argjson schsecrets ${KEYS[SCHSECRETS]} '.scorex.withdrawalEpochCertificate.signersSecrets = $schsecrets ' <<< "$template")
  template=$(jq '.scorex.withdrawalEpochCertificate.provingKeyFilePath = "'$sdkfolder'/sdk/src/test/resources/sample_proving_key_7_keys_with_threshold_5" '  <<< "$template")
  template=$(jq '.scorex.withdrawalEpochCertificate.verificationKeyFilePath = "'$sdkfolder'/sdk/src/test/resources/sample_vk_7_keys_with_threshold_5" ' <<< "$template")
  
  template=$(jq '.scorex.wallet.seed = "'$seed'" ' <<< "$template")
  template=$(jq '.scorex.wallet.genesisSecrets = [
		     '${KEYS[VRFSECRET]}',
		     '${KEYS[SECRET]}'
		     ] ' <<< "$template")

  template=$(jq --argjson genesisinfo ${KEYS[GENESISINFO]}  '.scorex.genesis = $genesisinfo ' <<< "$template")
  
  template=$(jq '.scorex.withdrawalEpochCertificate.submitterIsEnabled = '"$submitterisenabled"'  '  <<< "$template")
  template=$(jq '.scorex.network.nodeName = "'$nodename'" ' <<< "$template")
  template=$(jq '.scorex.network.agentName = "'$agentname'" ' <<< "$template")
  template=$(jq '.scorex.network.bindAddress = "'$netbindaddress'" ' <<< "$template")
  #NOT WORKING 
  #template=$(jq '.scorex.websocket.connectionTimeout = "'$connectiontimeout'" ' <<< "$template")

  #template=$(jq '.scorex.websocket.reconnectionDelay = "'$reconnectiondelay'" ' <<< "$template")
  #template=$(jq '.scorex.websocket.reconnectionMaxAttempts = '$reconnectionmaxattempts' ' <<< "$template")


  
  #

  to_log "$template" "JQOUTPUT"
  cat <<< "$template" > "$sdkconfigfolder"/"$configfile"
}

#Starts main function
main
