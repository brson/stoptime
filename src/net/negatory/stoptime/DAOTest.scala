package net.negatory.stoptime

import android.test.{AndroidTestCase}

class DAOTest extends AndroidTestCase {
  def testSceneCrud {
    val dao = new DAO(getContext, null)

    val sceneId = dao.createScene
    val scene = dao.loadScene(sceneId)
    assert(sceneId == scene.id, "The loaded scene should contain the correct id")

    dao.close
  }

  def testFrameCrud {
    val dao = new DAO(getContext, null)

    dao.close
  }
}