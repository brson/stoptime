package net.negatory.stoptime

import scala.actors.Actor._
import android.content.Context
import android.test.AndroidTestCase
import actors.{TIMEOUT, Actor}

class DataActor(context: Context, dbName: String) extends Actor with Logging {

  start
  
  private val dao = new DAO(context, dbName)

  def this(context: Context) {
    this(context, "stoptime")
  }

  override def act() {
    import DataActor._
    loop {
      react {
        case CreateScene =>
          Log.d("Creating scene")
          reply(SceneCreated(dao.createScene))
        case LoadScene(sceneId) => reply(SceneLoaded(dao.loadScene(sceneId)))
        case LoadAllScenes => actor {
          loop {
            
          }
        }
        case Close =>
          dao.close
          exit
      }
    }
  }
}

object DataActor {
  case object Close
  case object Done

  case object CreateScene
  case class SceneCreated(sceneId: Int)

  case class LoadScene(sceneId: Int)
  case class SceneLoaded(scene: Scene)

  case object LoadAllScenes

  case class CreateFrame(sceneId: Int, frameData: Array[Byte])
  case class FrameCreated(frameId: Int)

  case class LoadFrame(frameId: Int)
  case class FrameLoaded(frame: Frame)

  case class LoadAllFrames(sceneId: Int)
}

import junit.framework.Assert._

class DataActorTest extends AndroidTestCase with Logging {

  import DataActor._, TestTools._

  var dataActor: DataActor = null

  override def setUp = {
    dataActor = new DataActor(getContext, null)
  }

  override def tearDown = {
    dataActor ! DataActor.Close
  }

  def testCreateAndLoadScenes {
    val sceneId: Int = dataActor !? (timeout, CreateScene) match {
      case Some(SceneCreated(sceneId)) => sceneId
      case Some(msg) => unexpectedMsg("creating scene", msg)
      case None => msgTimeout("creating scene")
    }

    val scene: Scene = dataActor !? (timeout, LoadScene(sceneId)) match {
      case Some(SceneLoaded(scene)) => scene
      case Some(msg) => unexpectedMsg("loading scene", msg)
      case None => msgTimeout("creating scene")
    }
    assertEquals(sceneId, scene.id)
  }

  def testLoadAllScenes {
    
  }

}