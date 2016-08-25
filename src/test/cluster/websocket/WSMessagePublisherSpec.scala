package cluster.websocket

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.ByteString
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

/**
  * Created by Brian.Yip on 8/22/2016.
  */
class WSMessagePublisherSpec extends TestKit(ActorSystem("StringPublisherSpec"))
  with ImplicitSender
  with Matchers
  with WordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A WSMessagePublisher" must {
    implicit val materializer = ActorMaterializer()
    val wsMessagePublisherRef = system.actorOf(Props[WSMessagePublisher], "WSMessagePublisher")
    val wsMessagePublisher = ActorPublisher[Message](wsMessagePublisherRef)
    val source = Source.fromPublisher(wsMessagePublisher)
    val testSink = TestSink.probe[Message]

    val subscription = source.runWith(testSink)

    "accept WS text messages" in {
      // We will let the publisher know that we want it to eventually publish two messages.
      // This implicitly sends a message to the publisher.
      subscription.request(2)

      // Now that we have subscribed to the publisher, let's send the publisher a message
      val textMessage = TextMessage("Hello")
      wsMessagePublisherRef ! textMessage

      // The publisher should now publish to its subscriber
      // This first message is a reply to the subscription message
      subscription.expectNext(500.millis, TextMessage(WebSocketFlow.unsupportedMessageType))

      // This is the actual expected result
      subscription.expectNext(500.millis, textMessage)
    }

    "accept WS binary messages" in {
      subscription.request(2)
      val binaryMessage = BinaryMessage(ByteString("foobar"))
      wsMessagePublisherRef ! binaryMessage
      subscription.expectNext(500.millis, TextMessage(WebSocketFlow.unsupportedMessageType))
      subscription.expectNext(500.millis, TextMessage(WSMessagePublisher.binaryMessageUnimplemented))
    }
  }

}
