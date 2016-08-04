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
  var cancellableTask: Cancellable = scheduler.schedule(1.second, 1.second, self, "Waiting for task...")

  val random = Random.alphanumeric
  val childNodes = scala.collection.mutable.HashMap[Int, ActorRef]()
  val cluster = Cluster(context.system)
  var childIndex = 0

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])

    initializeChildren()
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
      handleStartAddingWorkers(workers)

    case addWorkers: AddWorkers => handleAddWorkers(addWorkers)

    case string: String => log.info(string)

    case _ =>
  }

  def handleAddWorkers(addWorkersMessage: AddWorkers): Unit = {
    childIndex += 1
    if (childIndex % (children + 1) == 0)
      childIndex = 1

    val result = childNodes.get(childIndex)
    result match {
      case Some(child) => child ! addWorkersMessage
      case None => log.warning(s"Child $childIndex does not exist!")
    }
  }

  def handleStartAddingWorkers(workerCount: Int): Unit = {
    log.info("Adding workers to children!")

    val workers = generateRandomWorkers(workerCount)

    // TODO: This could be moved to the MessageSimulator actor
    cancellableTask.cancel()
    cancellableTask =
      scheduler.schedule(1.second, 1.second, self, AddWorkers(workers))
  }

  def generateRandomWorkers(workerCount: Int): Seq[Worker] = {
    val result = mutable.MutableList[Worker]()
    for (i <- 0 to workerCount) {
      result += new Worker(generateRandomWorkerName())
    }
    result
  }

  def generateRandomWorkerName(): String = {
    var workerName = ""
    random.take(10).foreach {
      character => workerName += character
    }
    workerName
  }

  def initializeChildren(): Unit = {
    for (i <- 1 to children) {
      childNodes += (i -> context.actorOf(Props[ClusterBackend], s"${Master.childNodeName}$i"))
    }
  }
}

object Master {
  def childNodeName = "PiNode"
}