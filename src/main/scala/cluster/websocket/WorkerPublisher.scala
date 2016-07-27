package cluster.websocket

import akka.actor.Props
import akka.stream.actor.ActorPublisher
import generated.models.Worker

/**
  * Created by Brian.Yip on 7/26/2016.
  */
class WorkerPublisher extends ActorPublisher[Seq[Worker]] {

  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[Seq[Worker]])
  }

  override def receive: Receive = {
    case workers: Seq[Worker] =>
      // Do not send sequences of workers if a client is not reading from the stream fast enough
      if (isActive && totalDemand > 0)
        onNext(workers)
  }
}

object WorkerPublisher {
  def props: Props = Props(new WorkerPublisher())
}