package part4_techniques

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout

import scala.concurrent.duration._

object IntegratingWithActors extends App {
  implicit val system = ActorSystem("IntegratingWithActors")
  implicit val materializer = ActorMaterializer()

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case s: String =>
        log.info(s"Just received a string: $s")
        sender() ! s"$s$s"
      case n: Int =>
        log.info(s"Just received a number: $n")
        sender() ! 2*n
      case _ =>
    }
  }

  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  val numberSource = Source(1 to 10)

  // actor as a flow
  implicit val timeout = Timeout(2.seconds) // ask pattern returns future which needs implicit timeout
  // parallelism -> max number of messages in Akka mailbox before back-pressuring
  // also typing ask to type the returned future of ask pattern
  val actorBasedFlow = Flow[Int].ask[Int](parallelism = 4)(simpleActor)

//  numberSource.via(actorBasedFlow).to(Sink.ignore).run()
//  numberSource.ask[Int](parallelism = 4)(simpleActor).to(Sink.ignore).run() // equivalent

  /*
    Actor as a source
   */

  val actorPoweredSource = Source.actorRef[Int](bufferSize = 10, overflowStrategy = OverflowStrategy.dropHead)
  // returns materialized value of ActorRef when plugged into a sink and run
  val materializedActorRef = actorPoweredSource.to(Sink.foreach[Int](number => println(s"Actor powered flow got number: $number"))).run()
  // graph is no active and waiting for me to send messages to it
//  materializedActorRef ! 10
  // terminating the stream
//  materializedActorRef ! akka.actor.Status.Success("complete") // may be useful in project

  /*
    Actor as a destination/sink
    - an init message
    - an ack message to confirm the reception (lack of acknowledgement means back pressure
    - a complete message
    - a function to generate a message in case the stream throws an exception
   */

  case object StreamInit
  case object StreamAck
  case object StreamComplete
  case class StreamFail(exception: Throwable)

  class DestinationActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case StreamInit =>
        log.info("Stream initialized")
        sender() ! StreamAck
      case StreamComplete =>
        log.info("Stream complete")
        context.stop(self) // free up actor
      case StreamFail(ex) =>
        log.warning(s"Stream failed: $ex")
      case message =>
        log.info(s"Message: $message has come to its final resting point.")
        sender() ! StreamAck // must send or else get back pressure
    }
  }

  val destinationActor = system.actorOf(Props[DestinationActor], "destinationActor")

  val actorPoweredSink = Sink.actorRefWithAck[Int](
    destinationActor,
    onInitMessage = StreamInit,
    onCompleteMessage = StreamComplete,
    ackMessage = StreamAck,
    onFailureMessage = throwable => StreamFail(throwable) // optional
  )

  Source(1 to 10).to(actorPoweredSink).run()

//  Sink.actorRef() - rarely used, not capable of providing back pressure
}
