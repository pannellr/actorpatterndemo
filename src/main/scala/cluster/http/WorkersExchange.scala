package cluster.http

import akka.actor.{ActorContext, ActorRef}
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Flow
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

/**
  * Created by Brian.Yip on 8/23/2016.
  */
object WorkersExchange {
  val actorPathResolutionTimeout = 2.seconds
}

trait WorkersExchange {

  val context: ActorContext
  /**
    * The WebSocket flow function to exchange data between cluster and client
    *
    * @param wsMessagePublisherRef The actor publisher reference from which we wish to receive messages from
    * @return
    */
  def webSocketHandler(wsMessagePublisherRef: ActorRef): Flow[Message, Message, _]

  def establishConnectionWithPiNode(piNodePath: String): Future[Route] = {
    implicit val timeout = Timeout(HttpService.workersExchangeTimeoutDuration)
    val routePromise = Promise[Route]
    val routeFuture = routePromise.future
    resolveActorPath(piNodePath, routePromise)

    routeFuture.andThen {
      case Success(route) => route
      case Failure(ex) => complete(ex)
    }
    routeFuture
  }

  def resolveActorPath(actorPath: String, routePromise: Promise[Route]): Future[ActorRef] = {
    implicit val timeout = Timeout(WorkersExchange.actorPathResolutionTimeout)
    context.actorSelection(actorPath).resolveOne().andThen {
      case Success(messagePublisher) =>
        routePromise.success(handleWebSocketMessages(webSocketHandler(messagePublisher)))
      case Failure(ex) =>
        routePromise.failure(ex)
    }
  }

}
