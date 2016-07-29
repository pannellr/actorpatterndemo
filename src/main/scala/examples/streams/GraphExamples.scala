package examples.streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip, ZipWith}
import akka.stream.{ActorMaterializer, ClosedShape, SourceShape, UniformFanInShape}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


/**
  * Created by Brian.Yip on 7/29/2016.
  */

// Graphs are a slightly advanced topic.
// It is recommended to become comfortable with Sources, Sinks, and Flows before continuing
// Then, play around with some of Akka graph examples and see how the pieces are put together.
object GraphExamples extends App {

  // These are implicitly needed for the akka streams to function properly
  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def simpleGraph(): Unit = {
    val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val in = Source(1 to 10)
      val out = Sink.foreach((x: Int) => println(x))

      val bcast = builder.add(Broadcast[Int](2))
      val merge = builder.add(Merge[Int](2))

      val f1, f2, f3, f4 = Flow[Int].map(_ + 10)

      in ~> f1 ~> bcast ~> f2 ~> merge ~> f3 ~> out
      bcast ~> f4 ~> merge
      ClosedShape
    })
    g.run()
  }

  def partialGraph(): Unit = {
    val pickMaxOfThree = GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val zip1 = b.add(ZipWith[Int, Int, Int](math.max _))
      val zip2 = b.add(ZipWith[Int, Int, Int](math.max _))
      zip1.out ~> zip2.in0

      UniformFanInShape(zip2.out, zip1.in0, zip1.in1, zip2.in1)
    }

    val resultSink = Sink.head[Int]

    // Create a graph from the result sink
    val g = RunnableGraph.fromGraph(GraphDSL.create(resultSink) { implicit b => sink =>
      import GraphDSL.Implicits._

      // importing the partial graph will return its shape (inlets & outlets)
      val pm3 = b.add(pickMaxOfThree)

      Source.single(1) ~> pm3.in(0)
      Source.single(6) ~> pm3.in(1)
      Source.single(3) ~> pm3.in(2)
      pm3.out ~> sink.in
      ClosedShape
    })

    val max: Future[Int] = g.run()
    val result = Await.result(max, 300.millis)
    println(s"The result is: $result")
  }

  def sourceFromGraph(): Unit = {
    val pairs = Source.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      // prepare graph elements
      val zip = b.add(Zip[Int, Int]())
      def ints = Source.fromIterator(() => Iterator.from(1))

      // connect the graph
      ints.filter(_ % 2 != 0) ~> zip.in0
      ints.filter(_ % 2 == 0) ~> zip.in1

      // expose port
      SourceShape(zip.out)
    })

    val firstPair: Future[(Int, Int)] = pairs.runWith(Sink.head)
    val values = Await.result(firstPair, 300.millis)
    println(s"Values: $values")
  }

  sourceFromGraph()
}
