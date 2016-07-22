package cluster

import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._

object ClusterBackend {
  val illegalWorkersString = "Cannot have less than 0 workers!"
}

class ClusterBackend extends Actor with ActorLogging {

  val cluster = Cluster(context.system)
  var workers: Int = 0

  // subscribe to cluster changes, re-subscribe when restart 
  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case MemberUp(member) =>
      log.info("Member is Up: {}", member.address)

    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)

    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}",
        member.address, previousStatus)

    case AddWorkers(incomingWorkers) =>
      if (workers + incomingWorkers < 0) {
        sender() ! IllegalWorkers(ClusterBackend.illegalWorkersString)
      } else {
        workers += incomingWorkers
        sender() ! WorkersResult(workers)
      }
      log.info(s"Workers: $workers")

    case _: MemberEvent => // ignore
  }
}
