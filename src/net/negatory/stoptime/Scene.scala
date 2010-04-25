package net.negatory.stoptime

class Scene(val id: Int) extends AnyRef with Logging {
  def appendFrame(data: Array[Byte]) {
    Log.d("Appending frame to scene")
    // TODO                          
  }
}

object Scene {
  val DefaultScene = new Scene(-1)
}