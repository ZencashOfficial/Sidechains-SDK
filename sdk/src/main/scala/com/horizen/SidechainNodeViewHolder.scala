package com.horizen


import akka.actor.{ActorRef, ActorSystem, Props}
import com.horizen.block.SidechainBlock
import com.horizen.node.SidechainNodeView
import com.horizen.params.NetworkParams
import com.horizen.state.ApplicationState
import com.horizen.storage._
import com.horizen.validation.{HistoryBlockValidator, MainchainPoWValidator, SemanticBlockValidator, SidechainBlockSemanticValidator, WithdrawalEpochValidator}
import com.horizen.wallet.ApplicationWallet
import scorex.core.settings.ScorexSettings
import scorex.core.utils.NetworkTimeProvider
import scorex.util.ScorexLogging

import scala.util.{Failure, Success}
class SidechainNodeViewHolder(sidechainSettings: SidechainSettings,
                              historyStorage: SidechainHistoryStorage,
                              stateStorage: SidechainStateStorage,
                              walletBoxStorage: SidechainWalletBoxStorage,
                              secretStorage: SidechainSecretStorage,
                              walletTransactionStorage: SidechainWalletTransactionStorage,
                              openedWalletBoxStorage: SidechainOpenedWalletBoxStorage,
                              params: NetworkParams,
                              timeProvider: NetworkTimeProvider,
                              applicationWallet: ApplicationWallet,
                              applicationState: ApplicationState,
                              genesisBlock: SidechainBlock)
  extends scorex.core.NodeViewHolder[SidechainTypes#SCBT, SidechainBlock]
  with ScorexLogging
  with SidechainTypes
{
  override type SI = SidechainSyncInfo
  override type HIS = SidechainHistory
  override type MS = SidechainState
  override type VL = SidechainWallet
  override type MP = SidechainMemoryPool

  override val scorexSettings: ScorexSettings = sidechainSettings.scorexSettings

  private def semanticBlockValidators(params: NetworkParams): Seq[SemanticBlockValidator] = Seq(new SidechainBlockSemanticValidator(params))
  private def historyBlockValidators(params: NetworkParams): Seq[HistoryBlockValidator] = Seq(new WithdrawalEpochValidator(params), new MainchainPoWValidator(params))

  override def restoreState(): Option[(HIS, MS, VL, MP)] = for {
    history <- SidechainHistory.restoreHistory(historyStorage, params, semanticBlockValidators(params), historyBlockValidators(params))
    state <- SidechainState.restoreState(stateStorage, params, applicationState)
    wallet <- SidechainWallet.restoreWallet(sidechainSettings.wallet.seed.getBytes, walletBoxStorage, secretStorage,
      walletTransactionStorage, openedWalletBoxStorage, applicationWallet)
    pool <- Some(SidechainMemoryPool.emptyPool)
  } yield (history, state, wallet, pool)

  override protected def genesisState: (HIS, MS, VL, MP) = {
    val result = for {
      history <- SidechainHistory.genesisHistory(historyStorage, params, genesisBlock, semanticBlockValidators(params), historyBlockValidators(params))
      state <- SidechainState.genesisState(stateStorage, params, applicationState, genesisBlock)
      wallet <- SidechainWallet.genesisWallet(sidechainSettings.wallet.seed.getBytes, walletBoxStorage, secretStorage,
        walletTransactionStorage, openedWalletBoxStorage, applicationWallet, genesisBlock)
      pool <- Success(SidechainMemoryPool.emptyPool)
    } yield (history, state, wallet, pool)

    result.get
  }

  protected def getCurrentSidechainNodeViewInfo: Receive = {
    case SidechainNodeViewHolder.ReceivableMessages
      .GetDataFromCurrentSidechainNodeView(f) => sender() ! f(new SidechainNodeView(history(), minimalState(), vault(), memoryPool()))
  }

  protected def processLocallyGeneratedSecret: Receive = {
    case ls: SidechainNodeViewHolder.ReceivableMessages.LocallyGeneratedSecret[SidechainTypes#SCS] =>
      secretModify(ls.secret)
  }

  protected def secretModify(secret: SidechainTypes#SCS): Unit = {
    vault().addSecret(secret) match {
      case Success(newVault) =>
        updateNodeView(updatedVault = Some(newVault))
        sender() ! Success(Unit)
      case Failure(ex) =>
        sender() ! Failure(ex)
    }
  }

  override def receive: Receive = getCurrentSidechainNodeViewInfo orElse processLocallyGeneratedSecret orElse super.receive
}

object SidechainNodeViewHolder /*extends ScorexLogging with ScorexEncoding*/ {
  object ReceivableMessages{
    case class GetDataFromCurrentSidechainNodeView[HIS, MS, VL, MP, A](f: SidechainNodeView => A)
    case class LocallyGeneratedSecret[S <: SidechainTypes#SCS](secret: S)
  }
}

object SidechainNodeViewHolderRef {
  def props(sidechainSettings: SidechainSettings,
            historyStorage: SidechainHistoryStorage,
            stateStorage: SidechainStateStorage,
            walletBoxStorage: SidechainWalletBoxStorage,
            secretStorage: SidechainSecretStorage,
            walletTransactionStorage: SidechainWalletTransactionStorage,
            openedWalletBoxStorage: SidechainOpenedWalletBoxStorage,
            params: NetworkParams,
            timeProvider: NetworkTimeProvider,
            applicationWallet: ApplicationWallet,
            applicationState: ApplicationState,
            genesisBlock: SidechainBlock): Props =
    Props(new SidechainNodeViewHolder(sidechainSettings, historyStorage, stateStorage, walletBoxStorage, secretStorage,
      walletTransactionStorage, openedWalletBoxStorage, params, timeProvider,
      applicationWallet, applicationState, genesisBlock))

  def apply(sidechainSettings: SidechainSettings,
            historyStorage: SidechainHistoryStorage,
            stateStorage: SidechainStateStorage,
            walletBoxStorage: SidechainWalletBoxStorage,
            secretStorage: SidechainSecretStorage,
            walletTransactionStorage: SidechainWalletTransactionStorage,
            openedWalletBoxStorage: SidechainOpenedWalletBoxStorage,
            params: NetworkParams,
            timeProvider: NetworkTimeProvider,
            applicationWallet: ApplicationWallet,
            applicationState: ApplicationState,
            genesisBlock: SidechainBlock)
           (implicit system: ActorSystem): ActorRef =
    system.actorOf(props(sidechainSettings, historyStorage, stateStorage, walletBoxStorage, secretStorage,
      walletTransactionStorage, openedWalletBoxStorage, params, timeProvider,
      applicationWallet, applicationState, genesisBlock))

  def apply(name: String,
            sidechainSettings: SidechainSettings,
            historyStorage: SidechainHistoryStorage,
            stateStorage: SidechainStateStorage,
            walletBoxStorage: SidechainWalletBoxStorage,
            secretStorage: SidechainSecretStorage,
            walletTransactionStorage: SidechainWalletTransactionStorage,
            openedWalletBoxStorage: SidechainOpenedWalletBoxStorage,
            params: NetworkParams,
            timeProvider: NetworkTimeProvider,
            applicationWallet: ApplicationWallet,
            applicationState: ApplicationState,
            genesisBlock: SidechainBlock)
           (implicit system: ActorSystem): ActorRef =
    system.actorOf(props(sidechainSettings, historyStorage, stateStorage, walletBoxStorage, secretStorage,
      walletTransactionStorage, openedWalletBoxStorage, params, timeProvider,
      applicationWallet, applicationState, genesisBlock), name)
}
