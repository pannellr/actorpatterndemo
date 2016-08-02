package cluster.websocket

import akka.actor.ActorLogging
import akka.stream.actor.ActorPublisher
import generated.models.MoveWorkers

/**
  * Created by Brian.Yip on 7/29/2016.
  */
class MessagePublisher extends ActorPublisher[String] with ActorLogging {

  override def receive: Receive = {
    case setWorkers: MoveWorkers =>
      if (canPublish) {
        log.info(s"Got a ${MoveWorkers.getClass.getName} message: ${setWorkers.toString}")

        // Find the name of the destination actor node
        // Go update the workers on the destination node
        onNext("FooBar")
      }
    case _ =>
      log.info(WorkersFlow.unsupportedMessageType)
  }

  private def canPublish: Boolean = isActive && totalDemand > 0
}