package cluster

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import generated.models.StartAddingWorkers

object ClusterApp {

  def clusterSystem = "ClusterSystem"

  def main(args: Array[String]): Unit = {
    if (args.isEmpty)
      startup(Seq("2551", "2552", "0"))
    else
      startup(args)
  }

  def startup(ports: Seq[String]): Unit = {
    ports foreach { port =>
      val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
        withFallback(ConfigFactory.load())

      val system = ActorSystem(ClusterApp.clusterSystem, config)

      val children = 3
      val workersToAdd = 1
      val master = system.actorOf(Props(new Master(children)), name = "Master")
      master ! StartAddingWorkers(workersToAdd)
    }
  }

}

