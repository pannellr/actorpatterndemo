package cluster.websocket

/**
  * Created by Brian.Yip on 7/26/2016.
  */

import akka.actor.{ActorSystem, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream._
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Source}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import generated.models.Worker

import scala.io.StdIn

object WorkersFlow {
  val unsupportedMessageType = "Message type not supported"
}

trait WorkersFlow {
  type Workers = Seq[Worker]
  type WorkersFilter = Workers => Boolean

  val config = ConfigFactory.load("simpleHTTP")
  implicit val system = ActorSystem("SimpleHTTP", config)
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()

  // val setWorkersDeserializer = system.actorOf(Props[SetWorkersDeserializerProxy])

  val workersFlow: Flow[Message, Message, _] = Flow[Message].map {
    case TextMessage.Strict(text) => TextMessage("Got a text message!")
    case BinaryMessage.Strict(data: ByteString) =>
      // setWorkersDeserializer ! data
      TextMessage("Got a binary message!")
    case _ => TextMessage(WorkersFlow.unsupportedMessageType)
  }

  val echoService: Flow[Message, Message, _] = Flow[Message].map {
    case TextMessage.Strict(txt) => TextMessage("ECHO: " + txt)
    case _ => TextMessage(WorkersFlow.unsupportedMessageType)
  }


  // TODO: This should eventually be context.actorOf
  private val messagePublisherRef = system.actorOf(Props[MessagePublisher], "MessagePublisher")
  private val messagePublisher = ActorPublisher[String](messagePublisherRef)

  val webSocketFlow: Flow[Message, Message, _] =
    Flow.fromGraph(
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        // Proxy all incoming messages to the publisher
        val fromWebSocket = builder.add(
          Flow[Message].collect {
            case TextMessage.Strict(text) =>
              // Let's let the message publisher know that an update has occurred
              messagePublisherRef ! TextMessage(text)

              // Let the client know that we are alive and what we are up to.
              // Once the worker has successfully been moved, we will then send a push
              // notification to the client using this same websocket
              TextMessage("Moving worker...")
          }
        )

        // TODO: Send back a MoveWorkersSuccess back to the client instead of a TextMessage
        // This will emit any updates back to the client as text messages
        val updatesPublisher = Source.fromPublisher(messagePublisher)
          .map(messageAsString => TextMessage(messageAsString))

        // The client needs to be aware of two types of asynchronous messages:
        // The ones from the proxy and any updates from the messagePublisher
        val merge = builder.add(Merge[TextMessage](2))

        // Plug into this flow
        val toWebSocket = builder.add(
          Flow[Message].map {
            case TextMessage.Strict(text) => TextMessage(text)
            case _ => TextMessage(WorkersFlow.unsupportedMessageType)
          }
        )

        // Plug in the mapped results computed from this component
        fromWebSocket ~> merge

        // Plug in the mapped results from the updates publisher
        updatesPublisher ~> merge

        // Merge the both mapped results so that they can both write to this same websocket
        merge ~> toWebSocket

        // Convert this graph into a flow
        FlowShape(fromWebSocket.in, toWebSocket.out)
      }
    )
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
            handleWebSocketMessages(webSocketFlow)
          }
        }
    }
  }
  val serverBinding = Http().bindAndHandle(route, interface, port)
  logger.info(s"HTTP server listening on $interface:$port")

  // Serve indefinitely
  StdIn.readLine()
}
