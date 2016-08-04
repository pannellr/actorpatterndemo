package cluster

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import generated.models.StartAddingWorkers
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}


/**
  * Created by Brian.Yip on 8/3/2016.
  */
class MasterSpec extends TestKit(ActorSystem("MasterSpec"))
  with ImplicitSender
  with Matchers
  with WordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A Master node" must {
    val children = 3
    val masterNode = TestActorRef(new Master(children), "Master")

    "have the number of specified children" in {
      masterNode.underlyingActor.childNodes.size shouldBe children
    }

    s"add workers to its children when it receives a ${StartAddingWorkers.getClass.getSimpleName} message" in {
      masterNode.underlyingActor.childIndex shouldBe 0

      val startAddWorkersMessage = StartAddingWorkers(1)
      masterNode ! startAddWorkersMessage

      /* TODO: A more effective way to test a dispatcher is to load it into a separate configuration and
      * tell the master node which configuration to load either by message passing or instantiation.
      * Then, we could make it dispatch messages faster so that we do not have to wait so long.
      */
      val interval = 1100

      Thread.sleep(interval)
      masterNode.underlyingActor.childIndex shouldBe 1

      Thread.sleep(interval)
      masterNode.underlyingActor.childIndex shouldBe 2

      Thread.sleep(interval)
      masterNode.underlyingActor.childIndex shouldBe 3

      Thread.sleep(interval)
      masterNode.underlyingActor.childIndex shouldBe 1
    }

  }


}