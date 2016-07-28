package cluster.websocket

/**
  * Created by Brian.Yip on 7/26/2016.
  */

import akka.actor.{ActorSystem, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import generated.models.Worker

import scala.io.StdIn


trait WorkersFlow {
  type Workers = Seq[Worker]
  type WorkersFilter = Workers => Boolean

  val config = ConfigFactory.load("simpleHTTP")
  implicit val system = ActorSystem("SimpleHTTP", config)
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val setWorkersDeserializer = system.actorOf(Props[SetWorkersDeserializerProxy])

  val workersFlow: Flow[Message, Message, _] = Flow[Message].map {
    case TextMessage.Strict(text) => TextMessage("Got a text message!")
    case BinaryMessage.Strict(data: ByteString) =>
      setWorkersDeserializer ! data
      TextMessage("Got a binary message!")
  }

  val echoService: Flow[Message, Message, _] = Flow[Message].map {
    case TextMessage.Strict(txt) => TextMessage("ECHO: " + txt)
    case _ => TextMessage("Message type unsupported")
  }
}

object WebSocketMicroservice extends App with WorkersFlow {

  val logger = Logging(system, getClass)
  val interface = config.getString("http.interface")
  val port = config.getInt("http.port")

  // Configure HTTP routes
  val route = {
    get {
      pathEndOrSingleSlash {
        complete("Welcome to the HTTP microservice")
      }
    }

    // WebSocket endpoints
    pathPrefix("ws") {
      path("echo") {
        handleWebSocketMessages(echoService)
      } ~
        path("workers-exchange") {
          get {
            handleWebSocketMessages(workersFlow)
          }
      }
    }
  }

  val serverBinding = Http().bindAndHandle(route, interface, port)
  logger.info(s"HTTP server listening on $interface:$port")

  // Serve indefinitely
  StdIn.readLine()
}
