package com.horizen

import java.lang.{Byte => JByte}
import java.util.{List => JList, HashMap => JHashMap}
import com.horizen.utils.Pair
import akka.actor.ActorRef
import com.horizen.api.http._
import com.horizen.block.{ProofOfWorkVerifier, SidechainBlock, SidechainBlockSerializer}
import com.horizen.box.BoxSerializer
import com.horizen.companion.{SidechainBoxesCompanion, SidechainSecretsCompanion, SidechainTransactionsCompanion}
import com.horizen.params._
import com.horizen.secret.{PrivateKey25519Serializer, SecretSerializer}
import com.horizen.state.ApplicationState
import com.horizen.storage._
import com.horizen.transaction.TransactionSerializer
import scorex.core.{ModifierTypeId, NodeViewModifier}
import com.horizen.wallet.ApplicationWallet
import scorex.core.network.message.MessageSpec
import scorex.core.network.{NodeViewSynchronizerRef, PeerFeature}
import scorex.core.serialization.ScorexSerializer
import scorex.core.settings.ScorexSettings
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler}
import scorex.core.api.http.ApiRoute
import scorex.core.app.Application
import com.horizen.forge.{ForgerRef, MainchainSynchronizer}
import com.horizen.websocket._
import scorex.core.transaction.Transaction
import scorex.util.ScorexLogging

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.Try
import scala.collection.immutable.Map
import scala.io.Source
import com.google.inject.Inject
import com.google.inject.name.Named
import com.horizen.utils.BytesUtils

class SidechainApp @Inject()
  (@Named("SidechainSettings") val sidechainSettings: SidechainSettings,
   @Named("CustomBoxSerializers") val customBoxSerializers: JHashMap[JByte, BoxSerializer[SidechainTypes#SCB]],
   @Named("CustomSecretSerializers") val customSecretSerializers: JHashMap[JByte, SecretSerializer[SidechainTypes#SCS]],
   @Named("CustomTransactionSerializers") val customTransactionSerializers: JHashMap[JByte, TransactionSerializer[SidechainTypes#SCBT]],
   @Named("ApplicationWallet") val applicationWallet: ApplicationWallet,
   @Named("ApplicationState") val applicationState: ApplicationState,
   @Named("SecretStorage") val secretStorage: Storage,
   @Named("WalletBoxStorage") val walletBoxStorage: Storage,
   @Named("WalletTransactionStorage") val walletTransactionStorage: Storage,
   @Named("OpenedWalletBoxStorage") val openedWalletBoxStorage: Storage,
   @Named("WalletBoxOperationStorage") val walletBoxOperationStorage: Storage,
   @Named("StateStorage") val stateStorage: Storage,
   @Named("HistoryStorage") val historyStorage: Storage,
   @Named("CustomApiGroups") val customApiGroups: JList[ApplicationApiGroup],
   @Named("RejectedApiPaths") val rejectedApiPaths : JList[Pair[String, String]]
  )
  extends Application with ScorexLogging
{
  override type TX = SidechainTypes#SCBT
  override type PMOD = SidechainBlock
  override type NVHT = SidechainNodeViewHolder

  override implicit lazy val settings: ScorexSettings = sidechainSettings.scorexSettings

  private val storageList = mutable.ListBuffer[Storage]()

  System.out.println(s"Starting application with settings \n$sidechainSettings")
  log.debug(s"Starting application with settings \n$sidechainSettings")

  override implicit def exceptionHandler: ExceptionHandler = SidechainApiErrorHandler.exceptionHandler

  override implicit def rejectionHandler: RejectionHandler = SidechainApiRejectionHandler.rejectionHandler

  override protected lazy val features: Seq[PeerFeature] = Seq()

  override protected lazy val additionalMessageSpecs: Seq[MessageSpec[_]] = Seq(SidechainSyncInfoMessageSpec)

  protected val sidechainBoxesCompanion: SidechainBoxesCompanion =  SidechainBoxesCompanion(customBoxSerializers)
  protected val sidechainSecretsCompanion: SidechainSecretsCompanion = SidechainSecretsCompanion(customSecretSerializers)
  protected val sidechainTransactionsCompanion: SidechainTransactionsCompanion = SidechainTransactionsCompanion(customTransactionSerializers)

  // Deserialize genesis block bytes
  val genesisBlock: SidechainBlock = new SidechainBlockSerializer(sidechainTransactionsCompanion).parseBytes(
      BytesUtils.fromHexString(sidechainSettings.genesisData.scGenesisBlockHex)
    )
  val genesisPowData: Seq[(Int, Int)] = ProofOfWorkVerifier.parsePowData(sidechainSettings.genesisData.powData)

  // Init proper NetworkParams depend on MC network
  val params: NetworkParams = sidechainSettings.genesisData.mcNetwork match {
    case "regtest" => RegTestParams(
      BytesUtils.fromHexString(sidechainSettings.genesisData.scId),
      genesisBlock.id,
      genesisBlock.mainchainBlocks.head.hash,
      genesisPowData,
      sidechainSettings.genesisData.mcBlockHeight,
      sidechainSettings.genesisData.withdrawalEpochLength
    )
    case "testnet" => TestNetParams(
      BytesUtils.fromHexString(sidechainSettings.genesisData.scId),
      genesisBlock.id,
      genesisBlock.mainchainBlocks.head.hash,
      genesisPowData,
      sidechainSettings.genesisData.mcBlockHeight
    )
    case "mainnet" => MainNetParams(
      BytesUtils.fromHexString(sidechainSettings.genesisData.scId),
      genesisBlock.id,
      genesisBlock.mainchainBlocks.head.hash,
      genesisPowData,
      sidechainSettings.genesisData.mcBlockHeight,
      sidechainSettings.genesisData.withdrawalEpochLength
    )
    case _ => throw new IllegalArgumentException("Configuration file scorex.genesis.mcNetwork parameter contains inconsistent value.")
  }

  // Init all storages
  protected val sidechainSecretStorage = new SidechainSecretStorage(
    //openStorage(new JFile(s"${sidechainSettings.scorexSettings.dataDir.getAbsolutePath}/secret")),
    registerStorage(secretStorage),
    sidechainSecretsCompanion)
  protected val sidechainWalletBoxStorage = new SidechainWalletBoxStorage(
    //openStorage(new JFile(s"${sidechainSettings.scorexSettings.dataDir.getAbsolutePath}/wallet")),
    registerStorage(walletBoxStorage),
    sidechainBoxesCompanion)
  protected val sidechainWalletTransactionStorage = new SidechainWalletTransactionStorage(
    //openStorage(new JFile(s"${sidechainSettings.scorexSettings.dataDir.getAbsolutePath}/walletTransaction")),
    registerStorage(walletTransactionStorage),
    sidechainTransactionsCompanion)
  protected val sidechainOpenedWalletBoxStorage = new SidechainOpenedWalletBoxStorage(
    registerStorage(openedWalletBoxStorage),
    sidechainBoxesCompanion)
  protected val sidechainStateStorage = new SidechainStateStorage(
    //openStorage(new JFile(s"${sidechainSettings.scorexSettings.dataDir.getAbsolutePath}/state")),
    registerStorage(stateStorage),
    sidechainBoxesCompanion)
  protected val sidechainHistoryStorage = new SidechainHistoryStorage(
    //openStorage(new JFile(s"${sidechainSettings.scorexSettings.dataDir.getAbsolutePath}/history")),
    registerStorage(historyStorage),
    sidechainTransactionsCompanion, params)


  // Append genesis secrets if we start the node first time
  if(sidechainSecretStorage.isEmpty) {
    for(secretHex <- sidechainSettings.wallet.genesisSecrets)
      sidechainSecretStorage.add(PrivateKey25519Serializer.getSerializer.parseBytes(BytesUtils.fromHexString(secretHex)))
  }

  override val nodeViewHolderRef: ActorRef = SidechainNodeViewHolderRef(sidechainSettings, sidechainHistoryStorage,
    sidechainStateStorage,
    sidechainWalletBoxStorage, sidechainSecretStorage, sidechainWalletTransactionStorage,
    sidechainOpenedWalletBoxStorage, params, timeProvider,
    applicationWallet, applicationState, genesisBlock) // TO DO: why not to put genesisBlock as a part of params? REVIEW Params structure


  def modifierSerializers: Map[ModifierTypeId, ScorexSerializer[_ <: NodeViewModifier]] =
    Map(SidechainBlock.ModifierTypeId -> new SidechainBlockSerializer(sidechainTransactionsCompanion),
      Transaction.ModifierTypeId -> sidechainTransactionsCompanion)

  override val nodeViewSynchronizer: ActorRef =
    actorSystem.actorOf(NodeViewSynchronizerRef.props[SidechainTypes#SCBT, SidechainSyncInfo, SidechainSyncInfoMessageSpec.type,
      SidechainBlock, SidechainHistory, SidechainMemoryPool]
      (networkControllerRef, nodeViewHolderRef,
        SidechainSyncInfoMessageSpec, settings.network, timeProvider,
        modifierSerializers
      ))

  // Retrieve information for using a web socket connector
  val communicationClient: WebSocketCommunicationClient = new WebSocketCommunicationClient()
  val webSocketReconnectionHandler: WebSocketReconnectionHandler = new DefaultWebSocketReconnectionHandler(sidechainSettings.websocket)

  // Create the web socket connector and configure it
  val webSocketConnector : WebSocketConnector = new WebSocketConnectorImpl(
    sidechainSettings.websocket.address,
    sidechainSettings.websocket.connectionTimeout,
    communicationClient,
    webSocketReconnectionHandler
  )

  // Start the web socket connector
  val connectorStarted : Try[Unit] = webSocketConnector.start()

  // If the web socket connector can be started, maybe we would to associate a client to the web socket channel created by the connector
  if(connectorStarted.isSuccess)
    communicationClient.setWebSocketChannel(webSocketConnector)

  // Init Forger with a proper web socket client
  val mainchainNodeChannel = new MainchainNodeChannelImpl(communicationClient, params)
  val mainchainSynchronizer = new MainchainSynchronizer(mainchainNodeChannel)
  val sidechainBlockForgerActorRef: ActorRef = ForgerRef(sidechainSettings, nodeViewHolderRef, mainchainSynchronizer, sidechainTransactionsCompanion, params)


  // Init Transactions and Block actors for Api routes classes
  val sidechainTransactionActorRef: ActorRef = SidechainTransactionActorRef(nodeViewHolderRef)
  val sidechainBlockActorRef: ActorRef = SidechainBlockActorRef(sidechainSettings, nodeViewHolderRef, sidechainBlockForgerActorRef)


  // Init API
  var rejectedApiRoutes : Seq[SidechainRejectionApiRoute] = Seq[SidechainRejectionApiRoute]()
  rejectedApiPaths.asScala.foreach(path => rejectedApiRoutes = rejectedApiRoutes :+ SidechainRejectionApiRoute(path.getKey, path.getValue, settings.restApi, nodeViewHolderRef))

  // Once received developer's custom api, we need to create, for each of them, a SidechainApiRoute.
  // For do this, we use an instance of ApplicationApiRoute. This is an entry point between SidechainApiRoute and external java api.
  var applicationApiRoutes : Seq[ApplicationApiRoute] = Seq[ApplicationApiRoute]()
  customApiGroups.asScala.foreach(apiRoute => applicationApiRoutes = applicationApiRoutes :+ ApplicationApiRoute(settings.restApi, nodeViewHolderRef, apiRoute))

  val coreApiRoutes: Seq[SidechainApiRoute] = Seq[SidechainApiRoute](
    MainchainBlockApiRoute(settings.restApi, nodeViewHolderRef),
    SidechainBlockApiRoute(settings.restApi, nodeViewHolderRef, sidechainBlockActorRef, sidechainBlockForgerActorRef),
    SidechainNodeApiRoute(peerManagerRef, networkControllerRef, timeProvider, settings.restApi, nodeViewHolderRef),
    SidechainTransactionApiRoute(settings.restApi, nodeViewHolderRef, sidechainTransactionActorRef),
    SidechainWalletApiRoute(settings.restApi, nodeViewHolderRef)
  )

  // In order to provide the feature to override core api and exclude some other apis,
  // first we create custom reject routes (otherwise we cannot know which route has to be excluded), second we bind custom apis and then core apis
  override val apiRoutes: Seq[ApiRoute] = Seq[SidechainApiRoute]()
    .union(rejectedApiRoutes)
    .union(applicationApiRoutes)
    .union(coreApiRoutes)

  override val swaggerConfig: String = Source.fromResource("api/sidechainApi.yaml").getLines.mkString("\n")


  override def stopAll(): Unit = {
    super.stopAll()
    storageList.foreach(_.close())
  }

  private def registerStorage(storage: Storage) : Storage = {
    storageList += storage
    storage
  }
}
