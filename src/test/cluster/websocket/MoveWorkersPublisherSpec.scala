package cluster.websocket

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.ws.TextMessage
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import generated.models.{MoveWorkers, MoveWorkersSuccess, Worker}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._


/**
  * Created by Brian.Yip on 8/2/2016.
  */
class MoveWorkersPublisherSpec extends TestKit(ActorSystem("MessagePublisherSpec"))
  with ImplicitSender
  with Matchers
  with WordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A MoveWorkersPublisher" must {
    implicit val materializer = ActorMaterializer()
    val messagePublisherRef = system.actorOf(Props[MoveWorkersPublisher], "MessagePublisher")
    val messagePublisher = ActorPublisher[MoveWorkersSuccess](messagePublisherRef)
    val source = Source.fromPublisher(messagePublisher)
    val testSink = TestSink.probe[MoveWorkersSuccess]

    // Really silly thing: You need to send a (subscribe) request to the publisher.
    // If the publisher is not aware of its subscriber, then it will ignore all messages.
    // In short, we need to activate the subscription
    val subscription = source.runWith(testSink)

    "accept MoveWorkers messages" in {
      // We will let the publisher know that we want it to eventually publish two messages.
      subscription.request(2)

      // Now that we have subscribed to the publisher, let's send the publisher a message
      val moveWorkersMessage: MoveWorkers = MoveWorkers(Seq[Worker](Worker("Alice")))
      messagePublisherRef ! moveWorkersMessage

      // The publisher should now publish to its subscriber
      val firstExpectedMessage =
      MoveWorkersSuccess(MoveWorkersPublisher.missingDestinationActorMessage, MoveWorkersSuccess.Status.FAIL)
      val secondExpectedMessage =
        MoveWorkersSuccess(MoveWorkersPublisher.missingSourceActorMessage, MoveWorkersSuccess.Status.FAIL)
      subscription.expectNext(500.millis, firstExpectedMessage)
      subscription.expectNext(500.millis, secondExpectedMessage)
    }

    "accept MoveWorkersSuccess messages" in {
      subscription.request(1)

      val workersSuccessMessage = MoveWorkersSuccess("OK", MoveWorkersSuccess.Status.OK)
      messagePublisherRef ! workersSuccessMessage

      subscription.expectNext(500.millis, workersSuccessMessage)
    }

    "proxy MoveWorkers messages to a destination actor" in {
      subscription.request(1)

      val probe = TestProbe()
      val dummyWorkers = Seq[Worker](new Worker("Alice"), new Worker("Bob"))
      val destinationActorPath = probe.ref.path.toString
      val sourceActorPath = "Not used"

      val moveWorkersMessage = MoveWorkers(dummyWorkers, destinationActorPath, sourceActorPath)
      messagePublisherRef ! moveWorkersMessage

      probe.expectMsg(500.millis, moveWorkersMessage)
    }

    "not accept any other kind of message" in {
      subscription.request(1)

      val textMessage = TextMessage("I do not expect a reply")
      messagePublisherRef ! textMessage

      subscription.expectNoMsg(500.millis)
    }

  }
}