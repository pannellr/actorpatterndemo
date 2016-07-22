package cluster

/**
  * Created by Brian.Yip on 7/22/2016.
  */

import akka.actor.Props
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.ImplicitSender
import com.typesafe.config.ConfigFactory
import org.scalatest.WordSpecLike
import sample.cluster.multi.STMultiNodeSpec

object MultiNodeClusterConfig extends MultiNodeConfig {
  val first = role("first")
  val second = role("second")
  val third = role("third")

  def nodeList = Seq(first, second, third)

  // Extract individual sigar library for every node.
  nodeList foreach { role ⇒
    nodeConfig(role) {
      ConfigFactory.parseString(
        s"""
      akka.loglevel = INFO
      akka.actor.provider = "akka.cluster.ClusterActorRefProvider"

      akka.remote.log-remote-lifecycle-events = off
      akka.cluster.roles = [node]

      # Disable legacy metrics in akka-cluster.
      akka.cluster.metrics.enabled=off
      """)
    }
  }
}

class ClusterSpecMultiJvmNode1 extends ClusterSpec

class ClusterSpecMultiJvmNode2 extends ClusterSpec

class ClusterSpecMultiJvmNode3 extends ClusterSpec

class ClusterSpec extends MultiNodeSpec(MultiNodeClusterConfig)
  with STMultiNodeSpec
  with ImplicitSender
  with WordSpecLike {

  import MultiNodeClusterConfig._

  import scala.concurrent.duration._

  override def beforeAll() = multiNodeSpecBeforeAll()

  override def afterAll() = multiNodeSpecAfterAll()

  override def initialParticipants = roles.size

  "A MultiNodeCluster" must {

    "wait for all nodes to enter a barrier" in {
      enterBarrier("startup")
    }

    "send to and receive from a remote node" in {
      runOn(first) {
        enterBarrier("deployed")
        val secondNode = system.actorSelection(node(second) / "user" / "second")
        within(500.millis) {
          secondNode ! AddWorkers(1)
          expectMsg(10.seconds, WorkersResult(1))
        }
      }

      runOn(second) {
        system.actorOf(Props[ClusterBackend], "second")
        enterBarrier("deployed")
      }

      runOn(third) {
        system.actorOf(Props[ClusterBackend], "third")
        enterBarrier("deployed")
        val secondNode = system.actorSelection(node(second) / "user" / "second")

        // Wait 200 millis so that we can wait for the first worker to send a message
        Thread.sleep(200)
        within(500.millis) {
          secondNode ! AddWorkers(1)
          expectMsg(10.seconds, WorkersResult(2))
        }
      }

      enterBarrier("finished")
    }
  }
}