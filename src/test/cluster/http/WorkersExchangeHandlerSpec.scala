package cluster.http

import akka.actor.{ActorNotFound, ActorRef, ActorSystem}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.StandardRoute
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}

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

  "An WorkersExchangeHandler actor" must {
    val workersExchangeActor = TestActorRef(new WorkersExchangeHandler)

    "be able to resolve an actor path" in {
      val probeRef = TestProbe().ref
      val probePath = probeRef.path.toString
      val standardRoutePromise = Promise[StandardRoute]
      val actorRefFuture: Future[ActorRef] =
        workersExchangeActor.underlyingActor.resolveActorPath(probePath, standardRoutePromise)

      Await.result(actorRefFuture, 1.second) shouldBe probeRef
      standardRoutePromise.future.isCompleted shouldBe true
    }

    s"throw an ${ActorNotFound.getClass.getSimpleName} exception when it cannot resolve an actor path" in {
      val invalidActorPath = "foobar"
      val standardRoutePromise = Promise[StandardRoute]
      val actorRefFuture: Future[ActorRef] =
        workersExchangeActor.underlyingActor.resolveActorPath(invalidActorPath, standardRoutePromise)

      an[ActorNotFound] shouldBe thrownBy(Await.result(actorRefFuture, 1.second))
      standardRoutePromise.future.isCompleted shouldBe true
    }

    "be able to establish a WebSocket connection with a cluster backend" in {
      val probeRef = TestProbe().ref
      val probeActorPath = probeRef.path.toString
      val standardRouteFuture: Future[StandardRoute] =
        workersExchangeActor.underlyingActor.establishConnectionWithPiNode(probeActorPath)

      Await.result(standardRouteFuture, 1.second).toString shouldBe complete(probeActorPath).toString
      standardRouteFuture.isCompleted shouldBe true
    }

    "let the WebSocket client know that the actor was not found when establishing a WebSocket connection" in {
      val invalidActorPath = "foobar"
      val standardRouteFuture: Future[StandardRoute] =
        workersExchangeActor.underlyingActor.establishConnectionWithPiNode(invalidActorPath)

      an[ActorNotFound] shouldBe thrownBy(Await.result(standardRouteFuture, 1.second))
      standardRouteFuture.isCompleted shouldBe true
    }
  }

}
