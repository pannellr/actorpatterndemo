package cluster

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import generated.models.{StartAddingWorkers, Worker}

object WorkersFlow {
  val unsupportedMessageType = "Message type not supported"
}

object ClusterApp extends App {
  type Workers = Seq[Worker]
  type WorkersFilter = Workers => Boolean

  val config = ConfigFactory.load()
  implicit val system = ActorSystem("ClusterSystem", config)

  def startup(): Unit = {

    // TODO: This can be moved to a configuration file
    val children = 3
    val workersToAdd = 1
    val master = system.actorOf(Props(new Master(children)), name = "Master")
    master ! StartAddingWorkers(workersToAdd)

    system.actorOf(Props[HttpRouter], name = "HttpRouter")
  }

  startup()
}
