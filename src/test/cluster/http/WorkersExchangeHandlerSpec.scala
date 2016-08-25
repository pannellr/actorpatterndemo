package cluster.http

import akka.actor.{Actor, ActorNotFound, ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Flow
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import akka.util.Timeout
import cluster.websocket.WebSocketFlow
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}

/**
  * Created by Brian.Yip on 8/23/2016.
  */
class WorkersExchangeHandlerSpec extends TestKit(ActorSystem("WorkersExchangeHandlerSpec"))
  with ImplicitSender
  with Matchers
  with WordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  class WorkersExchangeImpl extends Actor with WorkersExchange {
    override def receive: Receive = {
      case _ =>
    }

    override def webSocketHandler(wsMessagePublisherRef: ActorRef): Flow[Message, Message, _] = {
      Flow[Message].map {
        case TextMessage.Strict(txt) => TextMessage(s"Hello $txt!")
        case _ => TextMessage(WebSocketFlow.unsupportedMessageType)
      }
    }
  }

  "An WorkersExchangeHandler actor" must {
    val workersExchangeActor = TestActorRef(new WorkersExchangeImpl)

    "be able to resolve an actor path" in {
      val probeRef = TestProbe().ref
      val probePath = probeRef.path.toString
      val routePromise = Promise[Route]
      val actorRefFuture = workersExchangeActor.underlyingActor.resolveActorPath(probePath, routePromise)

      Await.result(actorRefFuture, 1.second) shouldBe probeRef
      routePromise.future.isCompleted shouldBe true
    }

    s"throw an ${ActorNotFound.getClass.getSimpleName} exception when it cannot resolve an actor path" in {
      val invalidActorPath = "foobar"
      val routePromise = Promise[Route]
      val actorRefFuture = workersExchangeActor.underlyingActor.resolveActorPath(invalidActorPath, routePromise)

      an[ActorNotFound] shouldBe thrownBy(Await.result(actorRefFuture, 1.second))
      routePromise.future.isCompleted shouldBe true
    }

    "not throw an exception when successfully establishing a WebSocket connection with a cluster backend" in {
      val probeRef = TestProbe().ref
      val probeActorPath = probeRef.path.toString

      implicit val timeout = Timeout(1.second)
      val routeFuture = workersExchangeActor.underlyingActor.establishConnectionWithPiNode(probeActorPath)
      Await.result(routeFuture, 1.second)
      routeFuture.isCompleted shouldBe true
    }

    "let the WebSocket client know that the actor was not found when establishing a WebSocket connection" in {
      val invalidActorPath = "foobar"
      val routeFuture = workersExchangeActor.underlyingActor.establishConnectionWithPiNode(invalidActorPath)

      an[ActorNotFound] shouldBe thrownBy(Await.result(routeFuture, 1.second))
      routeFuture.isCompleted shouldBe true
    }
  }

}
