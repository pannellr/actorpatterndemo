package examples.streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.util.Random

/**
  * Created by Brian.Yip on 7/29/2016.
  */

// In this example, we are migrating data from the input customer to the output customer
case class InputCustomer(name: String)

case class OutputCustomer(firstName: String, lastName: String)

object InputCustomer {
  def random(): InputCustomer = {
    InputCustomer(s"FistName${Random.nextInt(1000)} LastName${Random.nextInt(1000)}")
  }
}

// For more examples on Akka streams: http://boldradius.com/blog-post/VS0NpTAAADAACs_E/introduction-to-akka-streams
// https://github.com/pkinsky/akka-streams-example
object SimpleFlowExample extends App {

  // --------------------->
  // Source ~> Flow ~> Sink

  implicit val actorSystem = ActorSystem()
  implicit val flowMaterializer = ActorMaterializer()

  // This is the data source where we generate our customers
  val inputCustomers = Source((1 to 100).map(_ => InputCustomer.random()))

  // Here is a transformation to convert the input source data into a desirable output format
  val normalize = Flow[InputCustomer].map(customer => customer.name.split(" ").toList).collect {
    case firstName :: lastName :: Nil => OutputCustomer(firstName, lastName)
  }

  // Let's do something with this transformed data
  val writeCustomers = Sink.foreach[OutputCustomer] { customer => println(customer) }

  // Put all of the pieces together
  inputCustomers.via(normalize).runWith(writeCustomers)
}



