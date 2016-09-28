package cluster

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import generated.models._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Random

/**
  * Created by Brian.Yip on 8/3/2016.
  */
class Master(children: Int) extends Actor with ActorLogging {

  val scheduler = context.system.scheduler
  val random = Random.alphanumeric
  val childNodes = scala.collection.mutable.HashMap[Int, ActorRef]()
  val cluster = Cluster(context.system)
  var cancellableTask: Cancellable = scheduler.schedule(1.second, 1.second, self, "Waiting for task...")
  var childIndex = 0


  //workPlan for round robin
//  val workPlan = new mutable.HashMap[String, Int]()
//  workPlan += ("green" -> 3)
//  workPlan += ("yellow" -> 1)

  //workPlan basic
  val workPlan = new mutable.HashMap[String, Int]()
  workPlan += ("green" -> 7)
  workPlan += ("red" -> 3)
  workPlan += ("yellow" -> 2)

  //workPlan long
//  val workPlan = new mutable.HashMap[String, Int]()
//  workPlan += ("green" -> 12)
//  workPlan += ("red" -> 10)
//  workPlan += ("yellow" -> 20)


  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])

    initializeChildren()
  }

  def initializeChildren(): Unit = {
    for (piNodeId <- 1 to children) {
      childNodes +=
        (piNodeId -> context.actorOf(Props(new ClusterBackend(piNodeId)), s"${Master.childNodeName}$piNodeId"))
    }
  }

  override def receive: Receive = {
    case MemberUp(member) =>
      log.info("Member is Up: {}", member.address)

    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)

    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}",
        member.address, previousStatus)

    case StartAddingWorkers(workers) =>
      log.info("Start adding workers!")
      handleStartAddingWorkers()

    case addWorkers: AddWorkers => handleAddWorkers(addWorkers)

    case addWorker: AddWorker => handleAddSingleWorker(addWorker)

    case string: String => log.info(string)

    case _ =>
  }

  def handleAddWorkers(addWorkersMessage: AddWorkers): Unit = {
    proxyMessageToChild(addWorkersMessage)
  }

  def handleAddSingleWorker(addWorker: AddWorker): Unit = {
    proxyMessageToChild(addWorker)
  }

  def proxyMessageToChild[Message](message: Message): Unit = {
    childIndex += 1
    if (childIndex % (children + 1) == 0)
      childIndex = 1

    val result = childNodes.get(childIndex)
    result match {
      case Some(child) => child ! message
      case None => log.warning(s"Child $childIndex does not exist!")
    }
  }

  def handleStartAddingWorkers(): Unit = {

    val workers = generateWorkersFromPlan()
    for (worker <- workers) scheduler.scheduleOnce(1.second, self, AddWorker(Option[Worker](worker)))


  }

  def generateWorkersFromPlan(): Seq[Worker] = {
    val workers = mutable.MutableList[Worker]()
    workPlan.foreach { worker =>
      for (i <- 1 to worker._2) {
        workers += new Worker(worker._1)
      }
    }
    workers
  }


}

object Master {
  def masterNodeName = "Master"
  def childNodeName = "PiNode"
}