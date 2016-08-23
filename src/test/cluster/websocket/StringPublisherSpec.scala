package cluster.websocket

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

/**
  * Created by Brian.Yip on 8/22/2016.
  */
class StringPublisherSpec extends TestKit(ActorSystem("StringPublisherSpec"))
  with ImplicitSender
  with Matchers
  with WordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A MessagePublisher" must {
    implicit val materializer = ActorMaterializer()
    val stringPublisherRef = system.actorOf(Props[StringPublisher], "StringPublisher")
    val stringPublisher = ActorPublisher[String](stringPublisherRef)
    val source = Source.fromPublisher(stringPublisher)
    val testSink = TestSink.probe[String]

    val subscription = source.runWith(testSink)

    "accept string messages" in {
      // We will let the publisher know that we want it to eventually publish two messages.
      subscription.request(1)

      // Now that we have subscribed to the publisher, let's send the publisher a message
      val stringMessage = "Hello"
      stringPublisherRef ! stringMessage

      // The publisher should now publish to its subscriber
      subscription.expectNext(500.millis, stringMessage)
    }
  }

}
