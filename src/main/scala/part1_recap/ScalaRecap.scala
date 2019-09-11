package part1_recap

import scala.concurrent.Future
import scala.util.{Failure, Success}

object ScalaRecap extends App {
  val aCondition: Boolean = false
  def myFunction(x: Int): Int = {
    // code
    if (x > 4) 42 else 65
  }

  // instructions vs expressions - expressions are evaluated
  // types + type inference

  // 00 features of Scala
  class Animal
  trait Carnivore {
    def eat(a: Animal): Unit
  }

  object Carnivore

  // generics
  abstract class MyList[+A]

  // method notations
  1 + 2 // infix notation
  1.+(2)

  // FP
//  val anIncrementer: Function1[Int, Int]
  val anIncrementer: Int => Int = (x:Int) => x + 1 // apply method takes int as parameter
  anIncrementer(1)

  List(1,2,3).map(anIncrementer)
  // HOF: map, flatMap, filter
  // for-comprehensions

  // Monads: Option, Try

  // Pattern matching
  val unknown: Any = 2
  val order = unknown match {
    case 1 => "first"
    case 2 => "second"
    case _ => "unknown"
  }

  try {
    // code that can throw an exception
    throw new RuntimeException
  } catch {
    case e: Exception => println("I caught it!")
  }

  /*
    Scala advanced
   */

  // multithreading
  import scala.concurrent.ExecutionContext.Implicits.global
  val future = Future {
    // long computation here executed on SOME other thread
    42
  }
  // map, flatMap, filter + others such as recover/recoverWith

  future onComplete {
    case Success(value) => println(s"I found the meaning of life! Turns out it's $value")
    case Failure(exception) => println(s"Wawawa! Fail!: $exception")
  } // on SOME thread

  val partialFunction: PartialFunction[Int, Int] = {
    case 1 => 42
    case 2 => 65
    case _ => 999
  }
  // based on pattern matching

  // type aliases
  type AkkaReceive = PartialFunction[Any, Unit]
  def receive: AkkaReceive = {
    case 1 => println("hello!")
    case _ => println("confused...")
  }

  // Implicits!
  implicit val timeout = 3000
  def setTimeout(f: () => Unit)(implicit timeout: Int) = f()
  setTimeout(() => println("timeout")) // don't need to supply the implicit timeout, injected by compiler
  // Future uses implicit global context, for example

  // conversions
  // 1 - implicit methods
  case class Person(name: String) {
    def greet: String = "Hi, my name is $name"
  }

  implicit def fromStringToPerson(name: String) = Person(name)

  "Peter".greet // will transform to person and call greet
  // fromStringToPerson("Peter").greet

  // 2 - implicit classes
  implicit class Dog(name: String) {
    def bark = println("Woof!")
  }

  "Lassie".bark // constructs dog from string and then calls bark

  // implicit organization
  // local scope first
  implicit val numberOrdering: Ordering[Int] = Ordering.fromLessThan(_ > _)
  List(1,2,3).sorted//(numberOrdering) ... automatically injected => List(3,2,1)

  // imported scope - like the implicit Execution context
  // companion objects of the types involved in the call
  object Person {
    // companion object for Person class
    implicit val personOrdering: Ordering[Person] = Ordering.fromLessThan((a, b) => a.name.compareTo(b.name) < 0)
  }

  List(Person("Bob"), Person("Alice")).sorted // compiled will find ordering, Person.personOrdering in companion object!

}
