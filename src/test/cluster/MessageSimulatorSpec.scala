package cluster

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestFSMRef, TestKit, TestProbe}
import generated.models.StartAddingWorkers
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
  * Created by Brian.Yip on 8/3/2016.
  */
class MessageSimulatorSpec extends TestKit(ActorSystem("MessageSimulatorSpec"))
  with ImplicitSender
  with Matchers
  with WordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A MessageSimulator" must {
    "reply back when it is in an idle state" in {
      val probe = TestProbe()
      val messageSimulator = TestFSMRef(new MessageSimulator(probe.ref))
      messageSimulator.stateName shouldBe Idle
      messageSimulator ! StartAddingWorkers
      expectMsg("FooBar!")
    }
  }
}
