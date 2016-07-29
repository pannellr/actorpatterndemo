package cluster.websocket

import akka.actor.ActorLogging
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.actor.ActorPublisher

/**
  * Created by Brian.Yip on 7/29/2016.
  */
class MessagePublisher extends ActorPublisher[Message] with ActorLogging {

  override def preStart = {
    context.system.eventStream.subscribe(self, classOf[Message])
  }

  override def receive: Receive = {
    case TextMessage.Strict(text) =>
      // Only publish when we get a message
      if (canPublish) {
        log.info(s"Got text: ${text.toString}")
        onNext(TextMessage(s"Message published!"))
      }
  }

  private def canPublish: Boolean = isActive && totalDemand > 0
}