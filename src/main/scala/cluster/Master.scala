package cluster

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import generated.models.{AddWorkers, StartAddWorkers, Worker}

import scala.util.Random

/**
  * Created by Brian.Yip on 8/3/2016.
  */
class Master extends Actor with ActorLogging {

  //  private var counter = 0
  //  private val scheduler = context.system.scheduler
  //    .schedule(3.seconds, 3.seconds, self, Tick)
  //
  //  def receive = {
  //    case Tick =>
  //      counter += 1
  //  }
  //

  val random = Random.alphanumeric
  val childNodes = scala.collection.mutable.HashMap[Int, ActorRef]()
  val cluster = Cluster(context.system)

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

    case StartAddWorkers => addWorkersToChildren()

    case _ => log.info("Received an unknown message")
  }

  def addWorkersToChildren(): Unit = {
    log.info("Adding workers to children!")

    childNodes.foreach {
      case (index, child) =>
        child ! AddWorkers(Seq[Worker](new Worker(generateRandomWorkerName())))
    }
  }

  def generateRandomWorkerName(): String = {
    var workerName = ""
    random.take(10).foreach {
      character => workerName += character
    }
    workerName
  }

  def initializeChildren(): Unit = {
    // TODO: This should be configurable based on the number of hosts
    for (i <- 1 to 3) {
      childNodes += (i -> context.actorOf(Props[ClusterBackend], s"${Master.childNodeName}$i"))
    }
  }
}

object Master {
  def childNodeName = "PiNode"
}
