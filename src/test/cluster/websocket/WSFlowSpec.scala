package cluster.websocket

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.testkit.TestProbe
import akka.util.ByteString
import org.reactivestreams.Publisher
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}

import scala.concurrent.duration._

/**
  * Created by Brian.Yip on 8/24/2016.
  */
class WSFlowSpec extends FlatSpec
  with Matchers
  with BeforeAndAfterEach
  with ScalatestRouteTest
  with WSFlow {

  var actorPublisherRef: ActorRef = system.actorOf(Props[WSMessagePublisher])
  var actorPublisher: Publisher[Message] = ActorPublisher[Message](actorPublisherRef)
  var messagePublisherSource: Source[Message, _] = Source.fromPublisher(actorPublisher)

  override def beforeEach(): Unit = {
    // We need to refresh the source because a publisher can only have one subscriber
    actorPublisherRef = system.actorOf(Props[WSMessagePublisher])
    actorPublisher = ActorPublisher[Message](actorPublisherRef)
    messagePublisherSource = Source.fromPublisher(actorPublisher)
  }

  "WebSocketFlow" should "work" in {
    def greeter: Flow[Message, Message, Any] =
      Flow[Message].mapConcat {
        case tm: TextMessage =>
          TextMessage(Source.single("Hello ") ++ tm.textStream ++ Source.single("!")) :: Nil
        case bm: BinaryMessage =>
          // ignore binary messages but drain content to avoid the stream being clogged
          bm.dataStream.runWith(Sink.ignore)
          Nil
      }
    val websocketRoute =
      path("greeter") {
        handleWebSocketMessages(greeter)
      }

    // create a testing probe representing the client-side
    val wsClient = WSProbe()

    // WS creates a WebSocket request for testing
    WS("/greeter", wsClient.flow) ~> websocketRoute ~>
      check {
        // check response for WS Upgrade headers
        isWebSocketUpgrade shouldEqual true

        // manually run a WS conversation
        wsClient.sendMessage("Peter")
        wsClient.expectMessage("Hello Peter!")

        wsClient.sendMessage(BinaryMessage(ByteString("abcdef")))
        wsClient.expectNoMessage(100.millis)

        wsClient.sendMessage("John")
        wsClient.expectMessage("Hello John!")

        wsClient.sendCompletion()
        wsClient.expectCompletion()
      }
  }

  it should "broadcast text messages to the client" in {
    val probe = TestProbe()
    val source = Source(0 to 0).map(_ => TextMessage("Ignore me"))
    val sink = Sink.actorRef(probe.ref, "Complete")
    webSocketFlow(messagePublisherSource).runWith(source, sink)

    probe.expectMsg(200.millis, TextMessage(WSFlow.notImplementedMessage))

    val publishedTextMessage = TextMessage("Hello!")
    actorPublisherRef ! publishedTextMessage
    probe.expectMsg(200.millis, publishedTextMessage)
  }

  it should "receive messages from the client" in {
    val probe = TestProbe()
    val source: Source[Message, _] = Source(1 to 1).map(_ => TextMessage("Hi"))
    val sink: Sink[Message, _] = Sink.actorRef(probe.ref, "Complete") // This will implicitly send a binary message
    webSocketFlow(messagePublisherSource).runWith(source, sink)

    // This project is not currently in a state where it accepts commands from a WebSocket client
    // Just implement the handler in webSocketFlow for the incoming client messages to make this system
    // responsive to WebSocketClients
    probe.expectMsg(200.millis, TextMessage(WSFlow.notImplementedMessage))
  }

}
