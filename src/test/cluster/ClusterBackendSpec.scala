package cluster

/**
  * Created by Brian.Yip on 7/21/2016.
  */

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestActors, TestKit}
import generated.models._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}


// Asynchronous testing
class ClusterBackendSpec() extends TestKit(ActorSystem("ClusterBackendSpec"))
  with ImplicitSender
  with Matchers
  with WordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "An Echo actor" must {
    "send back messages unchanged" in {
      val echo = system.actorOf(TestActors.echoActorProps)

      // The echo actor sends itself a message and should expect hello world
      echo ! "hello world"
      expectMsg("hello world")
    }
  }

  // Note that in these unit tests, actors will just be sending messages to themselves
  "A ClusterBackend" must {

    "append a new worker when it receives an AddWorkers message" in {
      val clusterBackend = TestActorRef(new ClusterBackend())
      val workers = Seq[Worker](new Worker("Tim"), new Worker("Jim"))
      val addWorkersMessage = AddWorkers(workers)

      clusterBackend ! addWorkersMessage
      clusterBackend.underlyingActor.workers shouldBe workers

      val nextWorkers = Seq[Worker](new Worker("Tom"), new Worker("Jerry"))
      val nextAddWorkersMessage = AddWorkers(nextWorkers)
      val expectedWorkers: Seq[Worker] = workers ++ nextWorkers

      clusterBackend ! nextAddWorkersMessage
      clusterBackend.underlyingActor.workers shouldBe expectedWorkers
    }

    "remove workers when it receives a RemoveWorkers message" in {
      val clusterBackend = TestActorRef(new ClusterBackend())
      val workers = Seq[Worker](new Worker("Tim"), new Worker("Jim"), new Worker("Bob"))

      clusterBackend ! AddWorkers(workers)
      clusterBackend.underlyingActor.workers shouldBe workers

      val removeWorkersMessage = RemoveWorkers(2)
      val expectedWorkers = Seq[Worker](new Worker("Bob"))

      clusterBackend ! removeWorkersMessage
      clusterBackend.underlyingActor.workers shouldBe expectedWorkers
    }

    "update its workers when it receives an MoveWorkers message" in {
      val clusterBackend = system.actorOf(Props[ClusterBackend])
      val oneWorker = Seq[Worker](new Worker("Alice"))
      clusterBackend ! MoveWorkers(oneWorker)
      expectMsg(WorkersResult(oneWorker))

      val threeWorkers = Seq[Worker](new Worker("Alice"), new Worker("Bob"), new Worker("Charlie"))
      clusterBackend ! MoveWorkers(threeWorkers)
      expectMsg(WorkersResult(threeWorkers))
    }

  }
}
