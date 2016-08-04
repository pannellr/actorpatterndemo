package cluster

import akka.actor.{Actor, ActorLogging}
import akka.cluster.ClusterEvent._
import generated.models._

import scala.collection.mutable

object ClusterBackend {
  val illegalWorkersString = "Cannot have less than 0 workers!"
}

class ClusterBackend extends Actor with ActorLogging {

  var workers = mutable.MutableList[Worker]()

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
    // Pipe this to a web socket
    sender() ! WorkersResult(workers)
  }

  private def handleRemoveWorkers(workerCount: Int): Unit = {
    workers = workers.drop(workerCount)
    sender() ! WorkersResult(workers)
  }

  private def handleMoveWorkers(incomingWorkers: Seq[Worker]): Unit = {
    workers = mutable.MutableList[Worker]()
    incomingWorkers.foreach {
      worker => workers += worker
    }
    sender() ! WorkersResult(workers)
  }
}
