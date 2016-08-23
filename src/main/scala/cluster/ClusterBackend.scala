package cluster

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import cluster.websocket.StringPublisher
import generated.models._

import scala.collection.mutable

object ClusterBackend {
  val stringPublisherRelativeActorPath = "StringPublisher"
}

class ClusterBackend extends Actor with ActorLogging {

  val cluster = Cluster(context.system)
  var workers = mutable.MutableList[Worker]()

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
    createMessagePublisher()
  }

  private def createMessagePublisher(): Unit = {
    context.actorOf(Props[StringPublisher], ClusterBackend.stringPublisherRelativeActorPath)
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  def receive = {
    case AddWorkers(incomingWorkers) => handleAddWorkers(incomingWorkers)

    case RemoveWorkers(workerCount) => handleRemoveWorkers(workerCount)

    case MoveWorkers(incomingWorkers, destinationActorName, sourceActorName) =>
      handleMoveWorkers(incomingWorkers)

    case _: MemberEvent => // ignore
  }

  private def handleAddWorkers(incomingWorkers: Seq[Worker]): Unit = {
    incomingWorkers.foreach {
      worker => workers += worker
    }
    sendWorkerCountToPublisher()
  }

  private def sendWorkerCountToPublisher(): Unit = {
    val stringPublisherRef = context.actorSelection(ClusterBackend.stringPublisherRelativeActorPath)
    stringPublisherRef ! workers.size.toString
  }

  private def handleRemoveWorkers(workerCount: Int): Unit = {
    workers = workers.drop(workerCount)
    sendWorkerCountToPublisher()
  }

  private def handleMoveWorkers(incomingWorkers: Seq[Worker]): Unit = {
    workers = mutable.MutableList[Worker]()
    incomingWorkers.foreach {
      worker => workers += worker
    }
    sender() ! WorkersResult(workers)
  }
}
