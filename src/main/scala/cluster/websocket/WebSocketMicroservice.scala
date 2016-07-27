package cluster.websocket

/**
  * Created by Brian.Yip on 7/26/2016.
  */

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import cluster.ClusterApp
import com.typesafe.config.ConfigFactory
import generated.models.Worker

import scala.io.StdIn


trait WorkersFlow {
  type Workers = Seq[Worker]
  type WorkersFilter = Workers => Boolean

  implicit val system = ActorSystem(ClusterApp.clusterSystem)
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()

  // TODO: Integrate Protobufs for binary messages so that we can use this workersSource
  private val workersSource: Source[Workers, ActorRef] = Source.actorPublisher[Workers](WorkerPublisher.props)

  private def workersFlow(workersFilter: WorkersFilter): Flow[Message, Message, Any] =
    Flow[Message].mapConcat {
      case tm: TextMessage => TextMessage(Source.single("Hello") ++ Source.single("!")) :: Nil
      case bm: BinaryMessage =>
        bm.dataStream.runWith(Sink.ignore)
        Nil
    }

  def allWorkersFlow = workersFlow(_ => true)
}

object WebSocketMicroservice extends App with WorkersFlow {

  val config = ConfigFactory.load()
  val logger = Logging(system, getClass)
  val interface = config.getString("http.interface")
  val port = config.getInt("http.port")

  // Configure HTTP routes
  val route = {
    path("ws-workers-exchange") {
      get {
        complete("Welcome to ws workers exchange!")
        // handleWebSocketMessages(allWorkersFlow)
      }
    }
  }

  val serverBinding = Http().bindAndHandle(route, interface, port)
  logger.info(s"HTTP server listening on $interface:$port")

  // Serve indefinitely
  StdIn.readLine()
}
