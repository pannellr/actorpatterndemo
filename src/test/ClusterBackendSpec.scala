/**
  * Created by Brian.Yip on 7/21/2016.
  */

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import cluster.{ClusterBackend, SetWorkers, WorkersResult}
import generated.models.Worker
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

    // TODO: Create a custom serializer because the default Java serializer is very slow
    "update its worker count when it receives an SetWorkers message" in {
      val clusterBackend = system.actorOf(Props[ClusterBackend])
      clusterBackend ! SetWorkers(Seq[Worker](new Worker()))
      expectMsg(WorkersResult(1))

      clusterBackend ! SetWorkers(Seq[Worker](new Worker(), new Worker(), new Worker()))
      expectMsg(WorkersResult(3))
    }

  }
}
