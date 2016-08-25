package cluster.websocket

import akka.actor.ActorLogging
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{InitialStateAsEvents, MemberEvent, UnreachableMember}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.actor.ActorPublisher

/**
  * Created by Brian.Yip on 8/22/2016.
  */

object WSMessagePublisher {
  val binaryMessageUnimplemented = "Binary messages not implemented"
}

class WSMessagePublisher extends ActorPublisher[Message] with ActorLogging {

  val cluster = Cluster(context.system)

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  override def receive: Receive = {
    case TextMessage.Strict(text) =>
      if (canPublish)
        onNext(TextMessage(text))
    case bm: BinaryMessage =>
      if (canPublish)
        onNext(TextMessage(WSMessagePublisher.binaryMessageUnimplemented))
    case _ =>
      log.info(WSFlow.unsupportedMessageType)
  }

  private def canPublish: Boolean = isActive && totalDemand > 0
}
