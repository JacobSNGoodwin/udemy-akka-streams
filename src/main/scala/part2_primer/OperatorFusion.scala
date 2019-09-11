package part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

object OperatorFusion extends App {
  implicit val system = ActorSystem("OperatorFusion")
  implicit val materializer = ActorMaterializer()

  val simpleSource = Source(1 to 1000)
  val simpleFlow = Flow[Int].map(_ + 1)
  val simpleFlow2 = Flow[Int].map(_ * 10)
  val simpleSink = Sink.foreach[Int](println)

  // runs on the same actor
  // operator/components FUSION - good thing for cheap operations
//  simpleSource.via(simpleFlow).via(simpleFlow2).to(simpleSink).run()

  // complex flows:
  val complexFlow = Flow[Int].map { x =>
    // simulating long computation
    Thread.sleep(1000)
    x + 1
  }
  val complexFlow2 = Flow[Int].map { x =>
    // simulating long computation
    Thread.sleep(1000)
    x * 10
  }

  // Below produces a 2 second delay between each number printing out
//  simpleSource.via(complexFlow).via(complexFlow2).to(simpleSink).run()

  // async boundary - now only 1 second between output now
//  simpleSource
//    .via(complexFlow).async // runs on one actor
//    .via(complexFlow2).async // runs on second actor
//    .to(simpleSink) // rest runs on a third actor
//    .run

  // ordering guarantees
  // deterministic ordering in this case
//  Source(1 to 3)
//    .map(element => {println(s"Flow A: $element"); element}) // prints element while also returning it
//    .map(element => {println(s"Flow B: $element"); element})
//    .map(element => {println(s"Flow C: $element"); element})
//    .runWith(Sink.ignore)

  // ordering maintained within each flow/map
  Source(1 to 3)
    .map(element => {println(s"Flow A: $element"); element}).async
    .map(element => {println(s"Flow B: $element"); element}).async
    .map(element => {println(s"Flow C: $element"); element}).async
    .runWith(Sink.ignore)
}
