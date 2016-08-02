package cluster.websocket

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.util.ByteString
import com.google.protobuf.InvalidProtocolBufferException
import generated.models.MoveWorkers


/**
  * Created by Brian.Yip on 7/28/2016.
  */
class SetWorkersDeserializerProxy(actorRef: ActorRef) extends Actor with ActorLogging {

  override def receive: Receive = {
    case serializedByteString: ByteString => deserializeByteString(serializedByteString)
    case _ => sender ! SetWorkersDeserializerProxy.noSerializedByteStringReply
  }

  def deserializeByteString(byteString: ByteString): Unit = {
    try {
      val setWorkersMessage = MoveWorkers.parseFrom(byteString.toArray)
      actorRef ! setWorkersMessage
    } catch {
      case invalidProtocolException: InvalidProtocolBufferException =>
        log.info(SetWorkersDeserializerProxy.invalidSetWorkersMessage)
        sender ! SetWorkersDeserializerProxy.invalidProtocolBufferExceptionReply
    }
  }
}

object SetWorkersDeserializerProxy {
  val invalidProtocolBufferExceptionReply = s"Expected a ${MoveWorkers.getClass.getName} message"
  val noSerializedByteStringReply = s"Expected a ${ByteString.getClass.getName}"
  val invalidSetWorkersMessage = s"Received an invalid SetWorkersMessage"
}
