package part3_graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}

object GraphBasics extends App {
  implicit val system = ActorSystem("GraphBasics")
  implicit val materializer = ActorMaterializer()

  val input = Source(1 to 1000)
  val incrementer = Flow[Int].map(x => x + 1) // hard computation
  val multiplier = Flow[Int].map(x => x* 10) // hard computation
  val output = Sink.foreach[(Int, Int)](println)

  // step 1 - setting up the fundamentals/scaffold for the graph
  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] => // builder = MUTABLE data structure
      import GraphDSL.Implicits._ // brings nice operators into scope

      // step 2 - add the necessary components/operators for this graph
      val broadcast = builder.add(Broadcast[Int](2)) // fan-out operator (int to 2 outputs)
      val zip = builder.add(Zip[Int, Int]) // fan-int operator

      // step 3 - tying up the components
      input ~> broadcast
      broadcast.out(0) ~> incrementer ~> zip.in0
      broadcast.out(1) ~> multiplier ~> zip.in1

      zip.out ~>  output

      // step 4 - return a closed shape
      ClosedShape // FREEZE the builder's shape

      // shape
    } // graph
  ) // runnable graph

//  graph.run() // run and materialize the graph

  /*
    EXERCISES
    1. Feed a source into two sinks at the same time
   */
  val firstSink = Sink.foreach[Int](x => println(s"First sink: $x"))
  val secondSink = Sink.foreach[Int](x => println(s"First sink: ${2*x}"))

  // step 1 - setting up the fundamentals/scaffold for the graph
  val sourceToTwoSinksGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] => // builder = MUTABLE data structure
      import GraphDSL.Implicits._ // brings nice operators into scope

      // step 2 - add the necessary components/operators for this graph
      val broadcast = builder.add(Broadcast[Int](2))

      // step 3 - tying up the components
//      input ~> broadcast

//      broadcast.out(0) ~> firstSink
//      broadcast.out(1) ~> secondSink

      // or using implicit port numbering
      input ~>  broadcast ~> firstSink
                broadcast ~> secondSink

      // step 4 - return a closed shape
      ClosedShape // FREEZE the builder's shape

      // shape
    } // graph
  ) // runnable graph

  /*
    Exercise 2 - balance
   */
  import scala.concurrent.duration._
  val fastSource = input.throttle(5, 1 second)
  val slowSource = input.throttle(2, 1 second)

  val sink1 = Sink.fold[Int, Int](0)((count, _) => {
    println(s"Sink 1 number of elements: $count")
    count + 1
  })
  val sink2 = Sink.fold[Int, Int](0)((count, _) => {
    println(s"Sink 2 number of elements: $count")
    count + 1
  })

  val balanceGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      // declare components
      val merge = builder.add(Merge[Int](2))
      val balance = builder.add(Balance[Int](2))

      // tie them up
      fastSource ~> merge ~> balance ~> sink1
      slowSource ~> merge
      balance ~> sink2

      ClosedShape
    }
  )

  balanceGraph.run()
}
