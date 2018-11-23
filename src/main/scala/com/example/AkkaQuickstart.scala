package com.example

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }
import kamon.Kamon
import kamon.prometheus.PrometheusReporter
import kamon.zipkin.ZipkinReporter

object Greeter {
  def props(message: String, printerActor: ActorRef): Props = Props(new Greeter(message, printerActor))
  final case class WhoToGreet(who: String)
  case object Greet
}

class Greeter(message: String, printerActor: ActorRef) extends Actor {
  import Greeter._
  import Printer._
  var greeting = ""

  def receive = {
    case WhoToGreet(who) =>
      greeting = message + ", " + who
    case Greet           =>
      printerActor ! Greeting(greeting)
  }
}

object Printer {
  def props: Props = Props[Printer]
  final case class Greeting(greeting: String)
}

class Printer extends Actor with ActorLogging {
  import Printer._
  def receive = {
    case Greeting(greeting) =>
      log.info("Greeting received (from " + sender() + "): " + greeting)
  }
}

object AkkaQuickstart extends App {
  import Greeter._

  // starts Prometheus exporter on localhost:9095
  Kamon.addReporter(new PrometheusReporter())
  // 23:26:01.077 [main] WARN kamon.zipkin.ZipkinReporter - For full Zipkin compatibility enable `kamon.trace.join-remote-parents-with-same-span-id` to preserve span id across client/server sides of a Span.
  Kamon.addReporter(new ZipkinReporter())
  

  val system: ActorSystem = ActorSystem("helloAkka")

  val printer: ActorRef = system.actorOf(Printer.props, "printerActor")
  val howdyGreeter: ActorRef =
    system.actorOf(Greeter.props("Howdy", printer), "howdyGreeter")
  val helloGreeter: ActorRef =
    system.actorOf(Greeter.props("Hello", printer), "helloGreeter")
  val goodDayGreeter: ActorRef =
    system.actorOf(Greeter.props("Good day", printer), "goodDayGreeter")

  howdyGreeter ! WhoToGreet("Akka")
  howdyGreeter ! Greet
  howdyGreeter ! WhoToGreet("Lightbend")
  howdyGreeter ! Greet
  helloGreeter ! WhoToGreet("Scala")
  helloGreeter ! Greet
  goodDayGreeter ! WhoToGreet("Play")
  goodDayGreeter ! Greet


  import scala.util.Random
  val allGreeters = Vector(howdyGreeter, helloGreeter, goodDayGreeter)
  def randomGreeter = allGreeters(Random.nextInt(allGreeters.length))
  while(true) {
    randomGreeter ! Greet
    Thread.sleep(500)
  }
 
}
