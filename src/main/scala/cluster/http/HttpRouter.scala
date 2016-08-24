package cluster.http

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{InitialStateAsEvents, MemberEvent, UnreachableMember}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.StandardRoute
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.io.StdIn

/**
  * Created by Brian.Yip on 8/19/2016.
  */

object HttpService {
  val greeting = "Welcome to the HTTP microservice"
  val echoMessage = "Echo endpoint was hit!"
  val workersExchangeMessage = "Workers exchange endpoint was hit!"

  // Wait 2 seconds before deciding that there is no service to handle web sockets
  val workersExchangeTimeoutDuration = 2.seconds
}

trait HttpService {

  val route = {
    get {
      pathEndOrSingleSlash {
        complete(HttpService.greeting)
      }
    } ~
      pathPrefix("ws") {
        path("echo") {
          complete(HttpService.echoMessage)
          //        //          handleWebSocketMessages(echoService)
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
  def workersExchangeRoute(nodeId: String): StandardRoute
}

class HttpRouter extends Actor with ActorLogging with HttpService {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val config = ConfigFactory.load()
  val interface = config.getString("http.interface")
  val port = config.getInt("http.port")
  val cluster = Cluster(context.system)

  override implicit def executor: ExecutionContextExecutor = system.dispatcher

  override def receive: Receive = {
    case _ => log.warning("Not Implemented")
  }

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  /**
    * Returns a route where a connection is established between a WebSocket client and a Cluster Backend
    *
    * @param nodeId The id of the node we wish to monitor
    * @return
    */
  override def workersExchangeRoute(nodeId: String): StandardRoute = {
    val workersExchangeHandler = context.actorOf(Props[WorkersExchangeHandler])
    implicit val timeout = Timeout(HttpService.workersExchangeTimeoutDuration)
    val future = workersExchangeHandler ? nodeId
    Await.result(future, timeout.duration).asInstanceOf[StandardRoute]
  }

  def serveRoutes() = {
    Http().bindAndHandle(route, interface, port)
    log.info(s"HTTP server listening on $interface:$port")

    // Serve indefinitely
    StdIn.readLine()
  }

  serveRoutes()
}
