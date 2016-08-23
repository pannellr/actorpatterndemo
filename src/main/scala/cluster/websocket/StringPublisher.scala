package cluster.websocket

import akka.actor.ActorLogging
import akka.stream.actor.ActorPublisher
import cluster.WorkersFlow

/**
  * Created by Brian.Yip on 8/22/2016.
  */
class StringPublisher extends ActorPublisher[String] with ActorLogging {
  override def receive: Receive = {
    case value: String =>
      if (canPublish)
        onNext(value)
    case _ =>
      log.info(WorkersFlow.unsupportedMessageType)
  }

  private def canPublish: Boolean = isActive && totalDemand > 0
}
