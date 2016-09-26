package cluster

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.http.scaladsl.model.ws.TextMessage
import cluster.websocket.WSMessagePublisher
import generated.models._

import scala.collection.mutable

object ClusterBackend {
  val WSMessagePublisherRelativeActorPath = WSMessagePublisher.getClass.getSimpleName
}

class ClusterBackend(nodeId: Int) extends Actor with ActorLogging {

  val cluster = Cluster(context.system)
  var workers = mutable.MutableList[Worker]()

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
    createMessagePublisher()
  }

  def createMessagePublisher(): Unit = {
    context.actorOf(Props[WSMessagePublisher], ClusterBackend.WSMessagePublisherRelativeActorPath)
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  def receive = {
    case AddWorker(incomingWorker) => handleAddWorker(incomingWorker)
    case AddWorkers(incomingWorkers) => handleAddWorkers(incomingWorkers)
    case _: MemberEvent => // ignore
  }

  def handleAddWorkers(incomingWorkers: Seq[Worker]): Unit = {
    incomingWorkers.foreach {
      worker => workers += worker
    }
    log.info(myWorkersMessage)
    sendMessageToPublisher(myWorkersMessage)
  }

  def handleAddWorker(incomingWorker: Option[Worker]): Unit = {

    incomingWorker match {
      case Some(worker) =>
      case None =>
    }

    println("!!!!!!!!!!!!!")
    println(incomingWorker)

//    incomingWorkers.foreach {
//      worker => workers += worker
//    }
    log.info(myWorkersMessage)
    sendMessageToPublisher(myWorkersMessage)
  }

  def myWorkersMessage = s"PI node $nodeId's workers: ${workers.size}"

  def sendMessageToPublisher(messageAsString: String): Unit = {
    val stringPublisherRef = context.actorSelection(ClusterBackend.WSMessagePublisherRelativeActorPath)
    stringPublisherRef ! TextMessage(messageAsString)
  }
}
