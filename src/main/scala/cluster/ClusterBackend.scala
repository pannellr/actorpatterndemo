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

  // workOrders = number of seconds to work
  val workOrders = new mutable.HashMap[String, Int]()
  workOrders += ("green" -> 500)
  workOrders += ("red" -> 1000)
  workOrders += ("yellow" -> 1500)

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
    case AddWorkers(incomingWorkers) => handleAddWorkers(incomingWorkers)

//    case RemoveWorkers(workerCount) => handleRemoveWorkers(workerCount)
//
//    case MoveWorkers(incomingWorkers, destinationActorName, sourceActorName) =>
//      handleMoveWorkers(incomingWorkers)

    case _: MemberEvent => // ignore
  }

  def handleAddWorkers(incomingWorker: Worker): Unit = {

    println("!!!!!!!!!!!!!")
    println(incomingWorker)

//    incomingWorkers.foreach {
//      worker => workers += worker
//    }
    log.info(myWorkersMessage)
    sendMessageToPublisher(myWorkersMessage)
  }

  def doWork(workTime: Int): Unit = {
    Thread.sleep(workTime);
  }

  def myWorkersMessage = s"PI node $nodeId's workers: ${workers.size}"

  def sendMessageToPublisher(messageAsString: String): Unit = {
    val stringPublisherRef = context.actorSelection(ClusterBackend.WSMessagePublisherRelativeActorPath)
    stringPublisherRef ! TextMessage(messageAsString)
  }

}
