package part2_primer

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}

object BackPressureBasics extends App {
  implicit val system = ActorSystem("BackPressureBasics")
  implicit val materializer = ActorMaterializer()

  val fastSource = Source(1 to 1000)
  val slowSink = Sink.foreach[Int] { x =>
    // simulate long process
    Thread.sleep(1000)
    println(s"Sink: $x")
  }

  // NOT BACKPRESSURE! This is fusing, all run on same actor, so of course will print 1 per second
//   fastSource.to(slowSink).run()

  // this time we have backpressure in place!
//  fastSource.async.to(slowSink).run()

  val simpleFlow = Flow[Int].map { x =>
    println(s"Incoming: $x")
    x + 1
  }

  // uses default buffering
//  fastSource.async
//    .via(simpleFlow).async
//    .to(slowSink)
//    .run()

  /*
    reactions to backpressure - in order
    - try to slow down if possible
    - buffer elements until there's more demand
    - drop down elements from the buffer if it overflows
    - teardown/kill the whole stream (failure)
   */

  // sink buffers the first 16 numbers (default) and flow buffers the last 10 numbers

//  val bufferedFlow = simpleFlow.buffer(10, overflowStrategy = OverflowStrategy.dropHead)
//  fastSource.async
//    .via(bufferedFlow).async
//    .to(slowSink)
//    .run()

  /*
  1-16: nobody is backpressued (sink buffers 16 elements)
  17-26: flow will buffer, flow will start dropping at the next element
  26-1000: flow will always drop the oldest element
   => 991-1000 => 992 - 1001 => sink
  */

  /*
    overflow strategies:
    - drop head = oldest
    - drop new = newest
    - drop new = exact element to be added = keeps the buffer
    - drop the entire buffer
    - backpressure signal
    - fail
   */

  // throttling
  import scala.concurrent.duration._
  fastSource.throttle(2, 1 second).runWith(Sink.foreach(println))
}
