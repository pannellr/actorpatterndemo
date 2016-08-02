package cluster.websocket

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.{ImplicitSender, TestKit}
import generated.models.{SetWorkers, Worker}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._


/**
  * Created by Brian.Yip on 8/2/2016.
  */
class MessagePublisherSpec extends TestKit(ActorSystem("MessagePublisherSpec"))
  with ImplicitSender
  with Matchers
  with WordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A MessagePublisher" must {
    implicit val materializer = ActorMaterializer()
    val messagePublisherRef = system.actorOf(Props[MessagePublisher], "MessagePublisher")
    val messagePublisher = ActorPublisher[String](messagePublisherRef)

    "accept SetWorkers messages" in {
      val source = Source.fromPublisher(messagePublisher)
      val testSink = TestSink.probe[String]

      // Really silly thing: You need to send a (subscribe) request to the publisher.
      // If the publisher is not aware of its subscriber, then it will ignore all messages.
      // We will let the publisher know that we want it to eventually publish one message.
      val subscription = source.runWith(testSink).request(1)

      // Now that we have subscribed to the publisher, let's send the publisher a message
      val setWorkersMessage: SetWorkers = SetWorkers(Seq[Worker](Worker("Alice")))
      messagePublisherRef ! setWorkersMessage

      // The publisher should now publish to its subscriber
      subscription.expectNext(200.millis, "FooBar")
    }

  }
}