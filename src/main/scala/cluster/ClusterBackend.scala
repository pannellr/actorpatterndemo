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
  var numWorkers = 0

  // workOrders = number of seconds to work
  val workOrders = new mutable.HashMap[String, Int]()
  workOrders += ("green" -> 2000)
  workOrders += ("red" -> 5000)
  workOrders += ("yellow" -> 15000)

  override def preStart(): Unit = {
    println("node prestart")
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
    createMessagePublisher()
  }

  def createMessagePublisher(): Unit = {
    println("create message publisher")
    context.actorOf(Props[WSMessagePublisher], ClusterBackend.WSMessagePublisherRelativeActorPath)
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  def receive = {
    //case AddWorker(incomingWorker) => println(incomingWorker) //handleAddWorker(incomingWorker)
    case _: MemberEvent => println("no match")

    case AddWorker(incomingWorker) =>
      incomingWorker match {
        case Some(worker) => handleAddWorker(worker)
        case None =>
      }
  }

  def handleAddWorker(worker: Worker): Unit = {

    println(worker)
    val message = s"$nodeId|${worker.name}|$numWorkers"
    println(message)
    sendMessageToPublisher(message)
    doWork(workOrders(worker.name))
    numWorkers = numWorkers + 1
    val doneMessage = s"$nodeId|none|$numWorkers"
    sendMessageToPublisher(doneMessage)
  }

  def doWork(workTime: Int): Unit = {
    Thread.sleep(workTime)
  }

  def sendMessageToPublisher(messageAsString: String): Unit = {
    val stringPublisherRef = context.actorSelection(ClusterBackend.WSMessagePublisherRelativeActorPath)
    stringPublisherRef ! TextMessage(messageAsString)
  }

}
