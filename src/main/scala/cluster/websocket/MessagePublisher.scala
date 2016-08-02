package cluster.websocket

import akka.actor.ActorLogging
import akka.http.scaladsl.model.ws.TextMessage
import akka.stream.actor.ActorPublisher
import generated.models.SetWorkers

/**
  * Created by Brian.Yip on 7/29/2016.
  */
class MessagePublisher extends ActorPublisher[String] with ActorLogging {

  override def receive: Receive = {
    case TextMessage.Strict(text) =>
      // Only publish when we get a message
      if (canPublish) {
        log.info(s"Got text: $text")
        onNext(s"Message published: $text")
      }
    case setWorkers: SetWorkers =>
      if (canPublish) {
        log.info(s"Got a ${SetWorkers.getClass.getName} message")
        onNext("FooBar")
      }
    case _ =>
      log.info(WorkersFlow.unsupportedMessageType)
  }

  private def canPublish: Boolean = isActive && totalDemand > 0
}