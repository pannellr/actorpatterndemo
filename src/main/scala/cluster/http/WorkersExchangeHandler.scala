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
object WorkersExchangeHandler {
  val actorPathResolutionTimeout = 2.seconds
}

trait WorkersExchangeHandler {

  val context: ActorContext

  /**
    * The WebSocket flow function to exchange data between cluster and client
    *
    * @return
    */
  def webSocketHandler(): Flow[Message, Message, _]

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
    implicit val timeout = Timeout(WorkersExchangeHandler.actorPathResolutionTimeout)
    context.actorSelection(actorPath).resolveOne().andThen {
      case Success(actor) => routePromise.success(handleWebSocketMessages(webSocketHandler()))
      case Failure(ex) => routePromise.failure(ex)
    }
  }

}
