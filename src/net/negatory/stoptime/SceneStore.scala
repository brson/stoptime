package net.negatory.stoptime

import android.content.Context

class SceneStore(context: Context) {

  val dao = new DAO(context)

  def newScene = dao.newScene
}