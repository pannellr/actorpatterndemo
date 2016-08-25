package cluster.websocket

import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Source}
import akka.util.ByteString

/**
  * Created by Brian.Yip on 8/22/2016.
  */
object WSFlow {
  val notImplementedMessage = "I don't handle input yet"
  val unsupportedMessageType = "Message type not supported"
}

trait WSFlow {

  def workersFlow(): Flow[Message, Message, _] = Flow[Message].map {
    case TextMessage.Strict(text) => TextMessage("Got a text message!")
    case BinaryMessage.Strict(data: ByteString) => TextMessage("Got a binary message!")
  }

  def webSocketFlow(messagePublisherSource: Source[Message, _]): Flow[Message, Message, _] =
    Flow.fromGraph(
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        // Proxy all incoming messages to the publisher
        val fromWebSocket = builder.add(
          Flow[Message].collect {
            // Let the client know that we are alive and what we are up to.
            // Once the worker has successfully been moved, we will then send a push
            // notification to the client using this same WebSocket
            case TextMessage.Strict(text) => TextMessage(WSFlow.notImplementedMessage)
          }
        )

        // This will emit any updates back to the client as text messages
        val updatesPublisher = messagePublisherSource.map(message => message.asInstanceOf[TextMessage])

        // Plug into this flow
        val toWebSocket = builder.add(
          Flow[Message].map {
            case TextMessage.Strict(text) => TextMessage(text)
          }
        )

        // The client needs to be aware of two types of asynchronous messages:
        // The ones from the proxy and any updates from the messagePublisher
        val merge = builder.add(Merge[TextMessage](2))

        // Plug in the mapped results computed from this component
        fromWebSocket ~> merge

        // Plug in the mapped results from the updates publisher
        updatesPublisher ~> merge

        // Merge the both mapped results so that they can both write to this same WebSocket
        merge ~> toWebSocket

        // Convert this graph into a flow
        FlowShape(fromWebSocket.in, toWebSocket.out)
      }
    )
}
