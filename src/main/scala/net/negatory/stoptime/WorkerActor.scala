package net.negatory.stoptime

import scala.actors.Actor
import scala.actors.Actor._

class WorkerActor extends Actor {

  start

  override def act() {
    import WorkerActor._

    loop {
      react {
        case Run(fun) => reply(fun())
      }
    }
  }
}

object WorkerActor {
  case class Run[T](fun: () => T)
}