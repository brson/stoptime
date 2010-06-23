package net.negatory.stoptime

import scala.actors.Actor
import scala.actors.Actor._

class IteratorActor[T](iter: Iterator[T]) extends Actor with Iterator[T] {

  start

  import IteratorActor._

  override def hasNext: Boolean = (this !? HasNext).asInstanceOf[Boolean]

  override def next: T = (this !? Next).asInstanceOf[T]

  override def act() {
    loopWhile (iter.hasNext) {
      val next = iter.next

      var waitingForRequest = true
      loopWhile (waitingForRequest) {
        react {
          case HasNext => reply(true)
          case Next => waitingForRequest = false; reply(next)
        }
      }
    } andThen loop {
      react {
        case HasNext => reply(false)
      }
    }
  }
}

object IteratorActor {
  case object HasNext // responds with true/false
  case object Next // responds with item
}