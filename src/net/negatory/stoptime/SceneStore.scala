package net.negatory.stoptime

import android.content.Context



class SceneStore(context: Context) {

  val dao = new DAO(context, "stoptime")

  def newScene: Scene = {
    val id = dao.createScene
    dao.loadScene(id)
  }
}