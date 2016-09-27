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
    case AddWorkers(incomingWorkers) => handleAddWorkers(incomingWorkers)
    case _: MemberEvent => println("no match")
  }

  def handleAddWorkers(incomingWorkers: Seq[Worker]): Unit = {
    println("!!!handle add workers")
    incomingWorkers.foreach {
      worker => workers += worker
    }
    log.info(myWorkersMessage)
    sendMessageToPublisher(myWorkersMessage)
  }

  def handleAddWorker(incomingWorker: Option[Worker]): Unit = {

    println("!!!!!!!!!!!!! handle add worker")
//    println(incomingWorker)

//    incomingWorker match {
//      case Some(worker) =>
//      case None =>
//    }

//    doWork(workOrders(incomingWorker))
    //sendMessageToPublisher(myWorkersMessage)
  }


  def doWork(workTime: Int): Unit = {
    Thread.sleep(workTime)
  }

  def myWorkersMessage = s"PI node $nodeId's workers: ${workers.size}"

  def sendMessageToPublisher(messageAsString: String): Unit = {
    val stringPublisherRef = context.actorSelection(ClusterBackend.WSMessagePublisherRelativeActorPath)
    stringPublisherRef ! TextMessage(messageAsString)
  }

}
