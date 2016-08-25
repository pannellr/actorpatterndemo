package cluster.http

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{InitialStateAsEvents, MemberEvent, UnreachableMember}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{Flow, Source}
import cluster.websocket.WSFlow
import cluster.{ClusterBackend, Master}
import com.typesafe.config.ConfigFactory
import generated.models.ServeRoutes
import org.reactivestreams.Publisher

import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.io.StdIn

/**
  * Created by Brian.Yip on 8/19/2016.
  */
class HttpRouter extends Actor with ActorLogging with HttpService with WorkersExchange with WSFlow {
  val cluster = Cluster(context.system)
  val config = ConfigFactory.load()
  val interface = config.getString("piCluster.http.interface")
  val httpPort = config.getInt("piCluster.http.port")

  implicit val system = ActorSystem(config.getString("piCluster.appName"), config)
  implicit val materializer = ActorMaterializer()

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  override def receive: Receive = {
    case ServeRoutes => serveRoutes()
    case _ =>
  }

  def serveRoutes() = {
    Http().bindAndHandle(route, interface, httpPort)
    log.info(s"HTTP server listening on $interface:$httpPort")

    // Serve indefinitely
    StdIn.readLine()
  }

  override implicit def executor: ExecutionContextExecutor = system.dispatcher

  /**
    * Returns a route where a connection is established between a WebSocket client and a Cluster Backend
    *
    * @param nodeId The id of the node we wish to monitor
    * @return
    */
  override def workersExchangeRoute(nodeId: String): Route = {
    val piNodePublisherActorRefPath =
      s"../${Master.masterNodeName}/${Master.childNodeName}$nodeId/${ClusterBackend.WSMessagePublisherRelativeActorPath}"
    val routeFuture = establishConnectionWithPiNode(piNodePublisherActorRefPath)

    // Wait for the backend to be ready before opening the WebSocket connection with the client
    Await.result(routeFuture, HttpService.workersExchangeTimeoutDuration)
  }

  override def webSocketHandler(wsMessagePublisherRef: ActorRef): Flow[Message, Message, _] = {
    log.info("Establishing a connection with a WebSocket client")
    val actorPublisher: Publisher[Message] = ActorPublisher[Message](wsMessagePublisherRef)
    val messagePublisherSource: Source[Message, _] = Source.fromPublisher(actorPublisher)
    webSocketFlow(messagePublisherSource)
  }
}
