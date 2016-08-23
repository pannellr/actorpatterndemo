package cluster.websocket

import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.FlowShape
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Source}
import akka.util.ByteString
import cluster.WorkersFlow

/**
  * Created by Brian.Yip on 8/22/2016.
  */
trait WebSocketFlow {

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

  //  val messagePublisherRef = context.actorOf(Props[MessagePublisher], "MessagePublisher")
  val messagePublisherRef: ActorRef
  val messagePublisher = ActorPublisher[String](messagePublisherRef)

  val webSocketFlow: Flow[Message, Message, _] =
    Flow.fromGraph(
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        // Proxy all incoming messages to the publisher
        val fromWebSocket = builder.add(
          Flow[Message].collect {
            case TextMessage.Strict(text) =>
              // Let the message publisher know that an update has occurred
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
