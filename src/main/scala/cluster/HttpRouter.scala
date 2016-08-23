package cluster

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{InitialStateAsEvents, MemberEvent, UnreachableMember}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.io.StdIn

/**
  * Created by Brian.Yip on 8/19/2016.
  */

// The only reason this is a trait is to that it can be tested
trait HttpService {
  val greeting = "Welcome to the HTTP microservice"
  val echoMessage = "Echo endpoint was hit!"
  val workersExchangeMessage = "Workers exchange endpoint was hit!"
  val route = {
    get {
      pathEndOrSingleSlash {
        complete(greeting)
      }
    } ~
      pathPrefix("ws") {
        path("echo") {
          complete(echoMessage)
          //        //          handleWebSocketMessages(echoService)
        } ~
          path("workers-exchange") {
            parameters('nodeId) { (nodeId) =>


              // Find an existing actorRef
              // If the actorRef was found, let the actorRef handle the connections
              // If the actorRef was not found, then establish the route here
              // Return a route via handleWebSocketMessages in a separate class


              //          complete {
              //            example(nodeId)
              //          }
              //              handleWebSocketMessages(echoService)
              complete(nodeId)
            }
          }
      }
  }

  implicit def executor: ExecutionContextExecutor

  //  def makeConnectionWithPiNode(nodeId: String): Future[ToResponseMarshallable] = {
  //    val piNodePath = Master.childNodeName + nodeId
  //    val actorRefFuture = context.actorSelection(s"$piNodePath").resolveOne()
  //
  //    actorRefFuture.andThen {
  //      case Success(actor) => actor ! "Cool!"
  //      case Failure(exception) => log.info(s"Exception happened: $exception")
  //    }
  //
  //  }

  def example(nodeId: String): Future[String] = {
    val myFuture = Future[String] {
      nodeId
    }
    Await.result(myFuture, 1.second)
    myFuture
  }


}

class HttpRouter extends Actor with ActorLogging with HttpService {
  val config = ConfigFactory.load()
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
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


  def serveRoutes() = {
    Http().bindAndHandle(route, interface, port)
    log.info(s"HTTP server listening on $interface:$port")

    // Serve indefinitely
    StdIn.readLine()
  }

  serveRoutes()
}
