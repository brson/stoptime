package net.negatory.stoptime

class Scene(val id: Int, dao: DAO) extends AnyRef with Logging {
  def appendFrame(imageData: Array[Byte]): Frame = {
    Log.d("Appending frame to scene " + id)
    val frameId = dao.createFrame(id, imageData)
    return dao.loadFrame(frameId, id)
  }
}

object Scene {
  val DefaultScene = new Scene(-1, null)
}