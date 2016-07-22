/**
  * Created by Brian.Yip on 7/21/2016.
  */

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import cluster.{AddWorkers, ClusterBackend, IllegalWorkers, WorkersResult}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}


// Asynchronous testing
class ClusterSpec() extends TestKit(ActorSystem("ClusterSpec"))
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

    "update its worker count when it receives an AddWorkers message" in {
      val clusterBackend = system.actorOf(Props[ClusterBackend])

      clusterBackend ! AddWorkers(1)
      expectMsg(WorkersResult(1))

      clusterBackend ! AddWorkers(3)
      expectMsg(WorkersResult(4))
    }

    "send an IllegalWorkers message when it has less than 0 workers" in {
      val clusterBackend = system.actorOf(Props[ClusterBackend])
      clusterBackend ! AddWorkers(-1)
      expectMsg(IllegalWorkers(ClusterBackend.illegalWorkersString))
    }

  }
}
