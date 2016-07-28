package cluster.websocket

/**
  * Created by Brian.Yip on 7/28/2016.
  */

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.ByteString
import generated.models.{SetWorkers, Worker}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._


class SetWorkersDeserializerProxySpec extends TestKit(ActorSystem("ClusterBackendSpec"))
  with ImplicitSender
  with Matchers
  with WordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A SetWorkersDeserializer" must {
    val testProbe = TestProbe()
    val setWorkersDeserializerProxy = system.actorOf(Props(new SetWorkersDeserializerProxy(testProbe.ref)))

    "deserialize SetWorkers messages and proxy them" in {
      val setWorkersMessage = SetWorkers(Seq[Worker](Worker("Alice")))
      val binaryByteString = ByteString(SetWorkers.toByteArray(setWorkersMessage))

      setWorkersDeserializerProxy ! binaryByteString
      testProbe.expectMsg(50.millis, setWorkersMessage)
    }

    "not reply when the serialized data does not represent a SetWorkers message" in {
      val notASetWorkersMessage = "notasetworkersmessaege"
      val binaryByteString = ByteString(notASetWorkersMessage)

      setWorkersDeserializerProxy ! binaryByteString
      testProbe.expectNoMsg(50.millis)
      expectMsg(SetWorkersDeserializerProxy.invalidProtocolBufferExceptionReply)
    }

    "not reply when it receives a message that is not a BinaryString" in {
      val notABinaryString = "notabinarystring"

      setWorkersDeserializerProxy ! notABinaryString
      testProbe.expectNoMsg(50.millis)
      expectMsg(SetWorkersDeserializerProxy.noSerializedByteStringReply)
    }
  }
}
