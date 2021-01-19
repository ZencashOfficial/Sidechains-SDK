package com.horizen.forge

import java.time.Instant
import java.util.{Timer, TimerTask}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.horizen._
import com.horizen.block.SidechainBlock
import com.horizen.companion.SidechainTransactionsCompanion
import com.horizen.consensus.{ConsensusEpochAndSlot, ConsensusEpochNumber, ConsensusSlotNumber, TimeToEpochSlotConverter}
import com.horizen.forge.Forger.ReceivableMessages.{GetForgingInfo, StartForging, StopForging, TryForgeNextBlockForEpochAndSlot}
import com.horizen.params.NetworkParams
import scorex.core.NodeViewHolder.ReceivableMessages
import scorex.core.NodeViewHolder.ReceivableMessages.LocallyGeneratedModifier
import scorex.util.ScorexLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

class Forger(settings: SidechainSettings,
             viewHolderRef: ActorRef,
             mainchainSynchronizer: MainchainSynchronizer,
             companion: SidechainTransactionsCompanion,
             applicationForger: ApplicationForger,
             val params: NetworkParams) extends Actor with ScorexLogging with TimeToEpochSlotConverter {
  val forgeMessageBuilder: ForgeMessageBuilder = new ForgeMessageBuilder(mainchainSynchronizer, companion, applicationForger, params, settings.websocket.allowNoConnectionInRegtest)
  val timeoutDuration: FiniteDuration = settings.scorexSettings.restApi.timeout
  implicit val timeout: Timeout = Timeout(timeoutDuration)


  private val consensusMillisecondsInSlot: Int = params.consensusSecondsInSlot * 1000
  private def forgingInitiatorTimerTask: TimerTask = new TimerTask {override def run(): Unit = tryToCreateBlockNow()}
  private var timerOpt: Option[Timer] = None

  private def startTimer(): Unit = {
    this.timerOpt match {
      case Some(_) => log.info("Automatically forging already had been started")
      case None => {
        val newTimer = new Timer()
        newTimer.scheduleAtFixedRate(forgingInitiatorTimerTask, 0, consensusMillisecondsInSlot)
        timerOpt = Some(newTimer)
        log.info("Automatically forging had been started")
      }
    }
  }

  private def stopTimer(): Unit = {
    this.timerOpt match {
      case Some(timer) => {
        timer.cancel()
        log.info("Automatically forging had been stopped")
        this.timerOpt = None
      }
      case None => log.info("Automatically forging had been already stopped")
    }
  }

  override def receive: Receive = {
    processStartForgingMessage orElse
    processStopForgingMessage orElse
    processTryForgeNextBlockForEpochAndSlotMessage orElse
    processGetForgeInfo orElse {
      case message: Any => log.error(s"Forger received strange message: ${message} from ${sender().path.name}")
    }
  }

  protected def processStartForgingMessage: Receive = {
    case StartForging => {
      log.info("Receive StartForging message")
      startTimer()
      sender() ! Success()
    }
  }

  protected def processStopForgingMessage: Receive = {
    case StopForging => {
      log.info("Receive StopForging message")
      stopTimer()
      sender() ! Success()
    }
  }

  protected def processTryForgeNextBlockForEpochAndSlotMessage: Receive = {
    case TryForgeNextBlockForEpochAndSlot(epochNumber, slotNumber) => tryToCreateBlockForEpochAndSlot(epochNumber, slotNumber, Some(sender()))
  }

  protected def tryToCreateBlockNow(): Unit = {
    val currentTime: Long = Instant.now.getEpochSecond
    val epochAndSlot = timestampToEpochAndSlot(currentTime)
    log.info(s"Send TryForgeNextBlockForEpochAndSlot message with epoch and slot ${epochAndSlot}")
    tryToCreateBlockForEpochAndSlot(epochAndSlot.epochNumber, epochAndSlot.slotNumber, None)
  }

  protected def tryToCreateBlockForEpochAndSlot(epochNumber: ConsensusEpochNumber, slot: ConsensusSlotNumber, respondsToOpt: Option[ActorRef]): Unit = {
    val forgeMessage: ForgeMessageBuilder#ForgeMessageType = forgeMessageBuilder.buildForgeMessageForEpochAndSlot(epochNumber, slot)
    val forgedBlockAsFuture = (viewHolderRef ? forgeMessage).asInstanceOf[Future[ForgeResult]]
    forgedBlockAsFuture.onComplete{
      case Success(ForgeSuccess(block)) => {
        log.info(s"Got successfully forged block with id ${block.id}")
        viewHolderRef ! LocallyGeneratedModifier[SidechainBlock](block)
        respondsToOpt.map(respondsTo => respondsTo ! Success(block.id))
      }

      case Success(SkipSlot) => {
        log.info(s"Slot is skipped")
        respondsToOpt.map(respondsTo => respondsTo ! Failure(new RuntimeException("Slot had been skipped")))
      }

      case Success(NoOwnedForgingStake) => {
        log.info(s"No forging stake.")
        respondsToOpt.map(respondsTo => respondsTo ! Failure(new RuntimeException("Can't forge block, no forging stake is present for epoch.")))
      }

      case Success(ForgeFailed(ex)) => {
        log.error(s"Forging had been failed. Reason: ${ex.getMessage}")
        respondsToOpt.map(respondsTo => respondsTo ! Failure(ex))
      }

      case failure @ Failure(ex) => {
        log.error(s"Forging had been failed. Reason: ${ex.getMessage}")
        respondsToOpt.map(respondsTo => respondsTo ! failure)
      }
    }
  }

  protected def processGetForgeInfo: Receive = {
    case GetForgingInfo => {
      val forgerInfoRequester = sender()

      val getInfoMessage
        = ReceivableMessages.GetDataFromCurrentView[SidechainHistory, SidechainState, SidechainWallet, SidechainMemoryPool, ConsensusEpochAndSlot](getEpochAndSlotForBestBlock)
      val epochAndSlotFut = (viewHolderRef ? getInfoMessage).asInstanceOf[Future[ConsensusEpochAndSlot]]
      epochAndSlotFut.onComplete{
        case Success(epochAndSlot: ConsensusEpochAndSlot) => {
          forgerInfoRequester ! Success(ForgingInfo(params.consensusSecondsInSlot, params.consensusSlotsInEpoch, epochAndSlot))
        }
        case failure @ Failure(ex) => {
          forgerInfoRequester ! failure
        }
      }
    }
  }

  def getEpochAndSlotForBestBlock(view: View): ConsensusEpochAndSlot = {
    val history = view.history
    history.timestampToEpochAndSlot(history.bestBlockInfo.timestamp)
  }
}

object Forger extends ScorexLogging {
  object ReceivableMessages {
    case object StartForging
    case object StopForging
    case class  TryForgeNextBlockForEpochAndSlot(consensusEpochNumber: ConsensusEpochNumber, consensusSlotNumber: ConsensusSlotNumber)
    case object GetForgingInfo
  }
}


object ForgerRef {
  def props(settings: SidechainSettings,
            viewHolderRef: ActorRef,
            mainchainSynchronizer: MainchainSynchronizer,
            companion: SidechainTransactionsCompanion,
            applicationForger: ApplicationForger,
            params: NetworkParams): Props = Props(new Forger(settings, viewHolderRef, mainchainSynchronizer, companion, applicationForger, params))

  def apply(settings: SidechainSettings,
            viewHolderRef: ActorRef,
            mainchainSynchronizer: MainchainSynchronizer,
            companion: SidechainTransactionsCompanion,
            applicationForger: ApplicationForger,
            params: NetworkParams)
           (implicit system: ActorSystem): ActorRef = system.actorOf(props(settings, viewHolderRef, mainchainSynchronizer, companion, applicationForger, params))

  def apply(name: String,
            settings: SidechainSettings,
            viewHolderRef: ActorRef,
            mainchainSynchronizer: MainchainSynchronizer,
            companion: SidechainTransactionsCompanion,
            applicationForger: ApplicationForger,
            params: NetworkParams)
           (implicit system: ActorSystem): ActorRef = system.actorOf(props(settings, viewHolderRef, mainchainSynchronizer, companion, applicationForger, params), name)
}