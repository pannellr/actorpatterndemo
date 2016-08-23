package cluster.http

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{InitialStateAsEvents, MemberEvent, UnreachableMember}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.StandardRoute
import akka.util.Timeout
import cluster.{ClusterBackend, Master}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

/**
  * Created by Brian.Yip on 8/23/2016.
  */
object WorkersExchangeHandler {
  val piNodePublisherActorPath = '/' + Master.masterNodeName + '/' + ClusterBackend.stringPublisherRelativeActorPath
  val actorPathResolutionTimeout = 2.seconds
}

class WorkersExchangeHandler extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  override def receive: Receive = {
    case nodeId: String => establishConnectionWithPiNode(nodeId)
  }

  def establishConnectionWithPiNode(nodeId: String): Unit = {
    implicit val timeout = Timeout(HttpService.workersExchangeTimeoutDuration)
    val piNodePath = WorkersExchangeHandler.piNodePublisherActorPath
    val standardRoutePromise = Promise[StandardRoute]
    val standardRouteFuture = standardRoutePromise.future
    resolveActorPath(piNodePath, standardRoutePromise)

    standardRouteFuture.andThen {
      case Success(route) => sender ! route
      case Failure(ex) => sender ! complete(ex)
    }
  }

  def resolveActorPath(actorPath: String, standardRoutePromise: Promise[StandardRoute]): Future[ActorRef] = {
    implicit val timeout = Timeout(WorkersExchangeHandler.actorPathResolutionTimeout)
    context.actorSelection(actorPath).resolveOne().andThen {
      case Success(actor) => standardRoutePromise.success(complete(actor.path.toString))
      case Failure(ex) => standardRoutePromise.failure(ex)
    }
  }

}
