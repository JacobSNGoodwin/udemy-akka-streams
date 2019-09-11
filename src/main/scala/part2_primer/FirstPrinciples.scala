package part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future

object FirstPrinciples extends App {
  implicit val system = ActorSystem("FirstPrinciples") // is second argument of ActorMaterializer
  implicit val materializer = ActorMaterializer() // implicit for graph.run below

  // sources
  val source = Source(1 to 10)
  // sinks
  val sink = Sink.foreach[Int](println)

//  val graph = source.to(sink)
//  graph.run()

  // flows transform elements
  val flow = Flow[Int].map(x => x + 1)
  val sourceWithFlow = source.via(flow) // is a Source
  val flowWithSink = flow.to(sink) // is a Sink

  sourceWithFlow.to(sink).run()
  source.to(flowWithSink).run()
  source.via(flow).to(sink).run()

  // nulls are NOT allowed per reactive streams specification
  //  val illegalSource = Source.single[String](null)
  //  illegalSource.to(Sink.foreach(println)) // NullPointerSpecification
  // use options instead

  // various kinds of source
  val finiteSource = Source.single(1)
  val anotherFiniteSource = Source(List(1,2,3))
  val emptySource = Source.empty[Int]
  val infiniteSource = Source(Stream.from(1)) // do not confuse an Akka stream with a "collection" stream

  import scala.concurrent.ExecutionContext.Implicits.global
  val futureSource = Source.fromFuture(Future(42))

  // sinks
  val theMostBoringSink = Sink.ignore
  val forEachSink = Sink.foreach[String](println)
  val headSink = Sink.head[Int] // retrieves head and then closes the stream
  val foldSink = Sink.fold[Int, Int](0)((a, b) => a + b)

  // flows - usually mapped to collections operators
  val mapFlow = Flow[Int].map(x => 2* x)
  val takeFlow = Flow[Int].take(5) // turns stream into finite stream
  // drop, filter
  // NOT have flatMap!

  // source -> flow -> flow -> ... -> sink
  val doubleFlowGraph = source.via(mapFlow).via(takeFlow).to(sink)
  doubleFlowGraph.run()

  // syntactic sugars
  val mapSource = Source(1 to 10).map(x => x * 2) // Source(1 to 10).via(Flow[Int].map(x => x * 2))
  //run streams directly
  mapSource.runForeach(println) // mapSource.to(Sink.foreach[Int](println)).run()

  // OPERATORS = components

  /*
    Exercise: create a stream that takes the names of persons, then you will keep the first 2 names with length > 5
   */

  val nameSource = Source(List("Bob", "Graham", "Bill", "Jim", "Gregory", "Jacob", "Ed"))
  nameSource.filter(_.length > 5).take(2).runForeach(println)

  system.terminate()
}
