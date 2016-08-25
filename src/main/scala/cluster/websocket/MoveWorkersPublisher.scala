package cluster.websocket

import akka.actor.ActorLogging
import akka.stream.actor.ActorPublisher
import generated.models.{MoveWorkers, MoveWorkersSuccess}

/**
  * Created by Brian.Yip on 7/29/2016.
  */

object MoveWorkersPublisher {
  val missingDestinationActorMessage = "Please specify a destination actor name"
  val missingSourceActorMessage = "Please specify a source actor name"
}

class MoveWorkersPublisher extends ActorPublisher[MoveWorkersSuccess] with ActorLogging {

  override def receive: Receive = {
    case moveWorkers: MoveWorkers =>
      if (canPublish)
        handleMoveWorkers(moveWorkers)
    case moveWorkersSuccess: MoveWorkersSuccess =>
      if (canPublish) {
        onNext(moveWorkersSuccess)
      }
    case _ =>
      log.info(WebSocketFlow.unsupportedMessageType)
  }


  def handleMoveWorkers(moveWorkersMessage: MoveWorkers): Unit = {
    log.info(s"Got a ${MoveWorkers.getClass.getName} message: ${moveWorkersMessage.toString}")

    if (!moveWorkersMessage.destinationActorPath.isEmpty && !moveWorkersMessage.sourceActorPath.isEmpty) {
      proxyMessageToClusterBackend(moveWorkersMessage)
      return
    }

    if (moveWorkersMessage.destinationActorPath.isEmpty)
      onNext(MoveWorkersSuccess(MoveWorkersPublisher.missingDestinationActorMessage, MoveWorkersSuccess.Status.FAIL))
    if (moveWorkersMessage.sourceActorPath.isEmpty)
      onNext(MoveWorkersSuccess(MoveWorkersPublisher.missingSourceActorMessage, MoveWorkersSuccess.Status.FAIL))
  }

  private def proxyMessageToClusterBackend(moveWorkersMessage: MoveWorkers): Unit = {
    val destinationRef = context.system.actorSelection(moveWorkersMessage.destinationActorPath)
    destinationRef ! moveWorkersMessage
  }


  private def canPublish: Boolean = isActive && totalDemand > 0
}