package net.negatory.stoptime

import actors.Actor
import android.os.{Message, Handler}

case class NextFrame(handler: Handler)

class PlaybackActor(dao: DAO, scene: Scene) extends Actor with Logging {

  start()

  def act() = {
    Log.d("Beggining playback of scene " + scene.id)

    val playbackIterator = dao.readFrames(scene.id)

    loopWhile(playbackIterator.hasNext) {

      val frame = playbackIterator.next

      react {
        case NextFrame(handler) =>
          val playFrameMsg = Message.obtain(handler, PlaybackActor.PLAYFRAME, frame)
          handler.sendMessage(playFrameMsg)
      }
    } andThen {
      playbackIterator.close
      react {
        case NextFrame(handler) =>
          Log.d("Finished playback of scene " + scene.id)
          val stopMsg = Message.obtain(handler, PlaybackActor.STOP)
          handler.sendMessage(stopMsg)
      }
    }

  }

}

object PlaybackActor {
  val PLAYFRAME = 1
  val STOP = 2
}