package net.negatory.stoptime

import actors.Actor
import android.os.{Message, Handler}

class PlaybackActor(dao: DAO, scene: Scene, handler: Handler) extends Actor with Logging {


  def act() = {
    Log.d("Beggining playback of scene " + scene.id)

    val playbackIterator = dao.readFrames(scene.id)
    try {
      for (frame <- playbackIterator) {
        val playFrameMsg = Message.obtain(handler, PlaybackActor.PLAYFRAME, frame)
        handler.sendMessage(playFrameMsg)
        Thread.sleep(1000)
      }
      Log.d("Finished playback of scene " + scene.id)
    }
    finally {
      playbackIterator.close      
    }

  }

}

object PlaybackActor {
  val PLAYFRAME = 1
}