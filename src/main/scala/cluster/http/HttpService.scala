package cluster.http

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.scaladsl.Flow
import cluster.websocket.WSFlow

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

/**
  * Created by Brian.Yip on 8/25/2016.
  */
trait HttpService {
  val route = {
    get {
      pathEndOrSingleSlash {
        complete(HttpService.greeting)
      }
    } ~
      pathPrefix("ws") {
        path("echo") {
          handleWebSocketMessages(echoService())
        } ~
          path("workers-exchange") {
            parameters('nodeId) { (nodeId) =>
              workersExchangeRoute(nodeId)
            }
          }
      }
  }

  implicit def executor: ExecutionContextExecutor

  /**
    * Returns a route where a connection is established between a WebSocket client and a Cluster Backend
    *
    * @param nodeId The id of the node we wish to monitor
    * @return
    */
  def workersExchangeRoute(nodeId: String): Route

  def echoService(): Flow[Message, Message, _] = Flow[Message].map {
    case TextMessage.Strict(txt) => TextMessage("ECHO: " + txt)
    case _ => TextMessage(WSFlow.unsupportedMessageType)
  }
}

object HttpService {
  val greeting = "Welcome to the HTTP microservice"
  val echoMessage = "Echo endpoint was hit!"
  val workersExchangeMessage = "Workers exchange endpoint was hit!"

  // Wait 2 seconds before deciding that there is no service to handle web sockets
  val workersExchangeTimeoutDuration = 2.seconds
}