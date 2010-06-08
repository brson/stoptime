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

  case object LoadScene
  case class SceneLoaded(scene: Scene)

  case object LoadAllScenes

  case class CreateFrame(sceneId: Int, frameData: Array[Byte])
  case class FrameCreated(frameId: Int)

  case class LoadFrame(frameId: Int)
  case class FrameLoaded(frame: Frame)

  case class LoadAllFrames(sceneId: Int)
}

trait TestTools {
  val timeout = 10000
}

import junit.framework.TestCase
import junit.framework.Assert._

class DataActorTest extends AndroidTestCase with TestTools {

  var dataActor: DataActor = null

  override def setUp = {
    dataActor = new DataActor(getContext, null)
  }

  override def tearDown = {
    dataActor ! DataActor.Close
  }

  def testCreateAndLoadScenes {
    val sceneId = dataActor !? (timeout, DataActor.CreateScene) match {
      case Some(DataActor.SceneCreated(sceneId)) => sceneId
      case Some(msg) => fail("Scene not created: " + msg)
      case None => fail("Timeout")
    }
  }
}