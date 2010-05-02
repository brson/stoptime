package net.negatory.stoptime

import android.test.{AndroidTestCase}
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

class DAOTest extends AndroidTestCase {
  def testSceneCrud {
    val dao = new DAO(getContext, null)

    val sceneId = dao.createScene
    val scene = dao.loadScene(sceneId)
    assert(sceneId == scene.id, "The loaded scene should contain the correct id")

    dao.close
  }


  def fakeFrameData: Array[Byte] = {
    val frameBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
    val memoryStream = new ByteArrayOutputStream()
    frameBitmap.compress(Bitmap.CompressFormat.JPEG, 50, memoryStream)
    memoryStream toByteArray

  }

  def testFrameCrud {
    val dao = new DAO(getContext, null)

    val sceneId = dao.createScene
    val frameId = dao.createFrame(sceneId, fakeFrameData)
    val frame = dao.loadFrame(sceneId, frameId)

    dao.close
  }
}