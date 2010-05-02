package net.negatory.stoptime

import android.content.Context



class SceneStore(val dao: DAO) {

  def newScene: Scene = {
    val id = dao.createScene
    dao.loadScene(id)
  }
}