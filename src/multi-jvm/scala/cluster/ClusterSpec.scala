package cluster

/**
  * Created by Brian.Yip on 7/22/2016.
  */

import akka.actor.{Actor, Props}
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.ImplicitSender
import sample.cluster.multi.STMultiNodeSpec

object ClusterConfig extends MultiNodeConfig {
  val node1 = role("node1")
  val node2 = role("node2")
}

class ClusterSpecMultiJvmNode1 extends ClusterSpec

class ClusterSpecMultiJvmNode2 extends ClusterSpec

object MultiNodeSample {

  class Ponger extends Actor {
    def receive = {
      case "ping" => sender() ! "pong"
    }
  }

}

class ClusterSpec extends MultiNodeSpec(ClusterConfig)
  with STMultiNodeSpec with ImplicitSender {

  import ClusterConfig._
  import MultiNodeSample._

  def initialParticipants = roles.size

  "A MultiNodeSample" must {

    "wait for all nodes to enter a barrier" in {
      enterBarrier("startup")
    }

    "send to and receive from a remote node" in {
      runOn(node1) {
        enterBarrier("deployed")
        val ponger = system.actorSelection(node(node2) / "user" / "ponger")
        ponger ! "ping"
        import scala.concurrent.duration._
        expectMsg(10.seconds, "pong")
      }

      runOn(node2) {
        system.actorOf(Props[Ponger], "ponger")
        enterBarrier("deployed")
      }

      enterBarrier("finished")
    }
  }
}