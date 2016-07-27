package cluster

/**
  * Created by Brian.Yip on 7/22/2016.
  */

import akka.actor.Props
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.ImplicitSender
import com.typesafe.config.ConfigFactory
import generated.models.Worker
import org.scalatest.WordSpecLike

object MultiNodeClusterConfig extends MultiNodeConfig {
  override val config = ConfigFactory.load()

  val first = role("first")
  val second = role("second")
  val third = role("third")

  def nodeList = Seq(first, second, third)


  nodeList foreach { role ⇒
    nodeConfig(role) {
      config
    } // Load config from the multi-jvm resources
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

        secondNode ! SetWorkers(Seq[Worker](new Worker()))
        expectMsg(WorkersResult(1))
      }

      runOn(second) {
        system.actorOf(Props[ClusterBackend], "second")
        enterBarrier("deployed")
      }

      runOn(third) {
        system.actorOf(Props[ClusterBackend], "third")
        enterBarrier("deployed")

        val secondNode = system.actorSelection(node(second) / "user" / "second")
        val twoWorkers = Seq[Worker](new Worker(), new Worker())
        secondNode ! SetWorkers(twoWorkers)
        expectMsg(WorkersResult(2))
      }

      enterBarrier("finished")
    }
  }
}