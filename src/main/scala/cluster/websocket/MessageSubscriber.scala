package cluster.websocket

import akka.actor.ActorLogging
import akka.http.scaladsl.model.ws.TextMessage
import akka.stream.actor.ActorSubscriberMessage.{OnComplete, OnError, OnNext}
import akka.stream.actor.{ActorSubscriber, RequestStrategy, WatermarkRequestStrategy}

/**
  * Created by Brian.Yip on 7/29/2016.
  */
class MessageSubscriber extends ActorSubscriber with ActorLogging {
  override protected def requestStrategy: RequestStrategy = WatermarkRequestStrategy(50)

  override def receive: Receive = {
    case OnNext(TextMessage.Strict(text)) =>
      log.debug(s"${classOf[MessageSubscriber]}: Received $text")
      TextMessage(s"Received text: $text")
    case OnError(err: Exception) =>
      log.error(s"${classOf[MessageSubscriber]}: Received exception")
    case OnComplete =>
      log.info(s"${classOf[MessageSubscriber]}: Text stream completed!")
      TextMessage("Done!")
  }
}
