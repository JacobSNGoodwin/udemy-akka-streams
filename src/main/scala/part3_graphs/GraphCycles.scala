package part3_graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, OverflowStrategy, UniformFanInShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, MergePreferred, RunnableGraph, Sink, Source, Zip}

object GraphCycles extends App {
  implicit val system = ActorSystem("GraphCycles")
  implicit val materializer = ActorMaterializer()

  val accelerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(Merge[Int](2))
    val incrementerShape = builder.add(Flow[Int].map { x =>
      println(s"Accelerating $x")
      x + 1
    })

    sourceShape ~> mergeShape ~> incrementerShape
                  mergeShape <~ incrementerShape

    ClosedShape
  }

//  RunnableGraph.fromGraph(accelerator).run()
  // graph cycle deadlock!

    /*
      Solution 1: MergePreferred
     */

  val actualAccelerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(MergePreferred[Int](1)) // preferred port plus one additional port
    val incrementerShape = builder.add(Flow[Int].map { x =>
      println(s"Accelerating $x")
      x + 1
    })

    sourceShape ~> mergeShape ~> incrementerShape
    mergeShape.preferred <~ incrementerShape // feeds back into preferred port

    ClosedShape
  }

//  RunnableGraph.fromGraph(actualAccelerator).run()

  /*
    Solution 2 - buffers
   */

  val bufferedRepeater = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(Merge[Int](2))
    val repeaterShape = builder.add(Flow[Int].buffer(10, OverflowStrategy.dropHead).map { x =>
      println(s"Accelerating $x")
      Thread.sleep(100)
      x
    })

    sourceShape ~> mergeShape ~> repeaterShape
    mergeShape <~ repeaterShape

    ClosedShape
  }

//  RunnableGraph.fromGraph(bufferedRepeater).run()

  /*
    cycles risk deadlocking
    - add bounds to the number of elements

    boundedness vs liveness
   */

  /*
    Challenge: create a fan-in shape
    - two inputs which will be fed with EXACTLY ONE number (1 and 1)
    - output will emit an INFINITE FIBONACCI SEQUENCE based off of those two numbers

    Hint: Use ZipWith and cycles, avoid deadlocks with MergePreferred
   */

  val fibonacciGenetator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val zip = builder.add(Zip[BigInt, BigInt])
    val mergePreferred = builder.add(MergePreferred[(BigInt,BigInt)](1))
    val fiboLogic = builder.add(Flow[(BigInt,BigInt)].map { pair =>
      val last = pair._1
      val previous = pair._2

      Thread.sleep(100) // for the sake of seeing the progress
      (last + previous, last)
    })
    val broadcast = builder.add(Broadcast[(BigInt, BigInt)](2)) // second will go back to mergePreferred
    val extractLast = builder.add(Flow[(BigInt, BigInt)].map(pair => pair._1)) // get summed number

    zip.out ~> mergePreferred ~> fiboLogic ~> broadcast ~> extractLast
    mergePreferred.preferred <~ broadcast

    UniformFanInShape(extractLast.out, zip.in0, zip.in1)
  }

  val fibGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val source1 = builder.add(Source.single[BigInt](1))
      val source2 = builder.add(Source.single[BigInt](1))
      val sink = builder.add(Sink.foreach[BigInt](println))
      val fibo = builder.add(fibonacciGenetator)

      source1 ~> fibo.in(0)
      source2 ~> fibo.in(1)

      fibo.out ~> sink

      ClosedShape
    }
  )

  fibGraph.run()
}
