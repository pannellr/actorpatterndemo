package cluster

import akka.actor.{ActorSystem, Props}
import cluster.http.HttpRouter
import com.typesafe.config.ConfigFactory
import generated.models.{ServeRoutes, StartAddingWorkers, Worker}

object ClusterApp extends App {
  type Workers = Seq[Worker]
  type WorkersFilter = Workers => Boolean

  val port = 2551
  val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
    withFallback(ConfigFactory.load())

  implicit val system = ActorSystem(config.getString("piCluster.appName"), config)

  def startup(): Unit = {
    val children: Int = config.getString("piCluster.childNodes").toInt
    val workersToAdd: Int = config.getString("piCluster.workersToAdd").toInt
    val master = system.actorOf(Props(new Master(children)), name = "Master")
    val httpServer = system.actorOf(Props[HttpRouter], name = "HttpRouter")

    master ! StartAddingWorkers(workersToAdd)
    httpServer ! ServeRoutes
  }

  startup()
}
