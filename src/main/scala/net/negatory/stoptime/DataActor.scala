package net.negatory.stoptime

import scala.actors.Actor._
import android.content.Context
import android.test.AndroidTestCase
import actors.{TIMEOUT, Actor}
import java.io.Closeable

class IteratorActor[T](iter: Iterator[T]) extends Actor with Iterator[T] {

  start

  import IteratorActor._

  override def hasNext: Boolean = (this !? HasNext).asInstanceOf[Boolean]

  override def next: T = (this !? Next).asInstanceOf[T]

  override def act() {
    loopWhile (iter.hasNext) {
      val next = iter.next

      var waitingForRequest = true
      loopWhile (waitingForRequest) {
        react {
          case HasNext => reply(true)
          case Next => waitingForRequest = false; reply(next)
        }
      }
    } andThen loop {
      react {
        case HasNext => reply(false)
      }
    }
  }
}

object IteratorActor {
  case object HasNext // responds with true/false
  case object Next // responds with item
}

object DataActor {
  case object Close
  case object Done

  case object CreateScene
  case class SceneCreated(sceneId: Int)

  case class LoadScene(sceneId: Int)
  case class SceneLoaded(scene: Scene)

  case object LoadAllScenes
  case class AllScenesLoaded(iterator: IteratorActor[Scene])

  case class CreateFrame(sceneId: Int, frameData: Array[Byte])
  case class FrameCreated(frameId: Int)

  case class LoadFrame(frameId: Int)
  case class FrameLoaded(frame: Frame)

  case class LoadAllFrames(sceneId: Int)
}



class DataActorTest extends AndroidTestCase with Logging {

  import junit.framework.Assert._
  import DataActor._, TestTools._
  import IteratorActor._

  var dataActor: DataActor = null

  override def setUp = {
    dataActor = new DataActor(getContext, null)
  }

  override def tearDown = {
    dataActor ! DataActor.Close
  }

  def createScene: Int = dataActor !? (timeout, CreateScene) match {
    case Some(SceneCreated(sceneId)) => sceneId
    case Some(msg) => unexpectedMsg("creating scene", msg)
    case None => msgTimeout("creating scene")
  }

  def testCreateAndLoadScenes {
    val sceneId = createScene

    val scene: Scene = dataActor !? (timeout, LoadScene(sceneId)) match {
      case Some(SceneLoaded(scene)) => scene
      case Some(msg) => unexpectedMsg("loading scene", msg)
      case None => msgTimeout("creating scene")
    }
    assertEquals(sceneId, scene.id)
  }

  def testLoadAllScenes {
    val numScenes = 20
    val sceneIds = List.range(1, numScenes).map(_ => createScene)
    val sceneIter = dataActor !? (timeout, LoadAllScenes) match {
      case Some(AllScenesLoaded(iter)) => iter
      case Some(msg) => unexpectedMsg("loading all scenes", msg)
      case None => msgTimeout("loading all scenes")
    }
    val loadedIds = (sceneIter map ((s: Scene) => s.id)).toList
    assertEquals(sceneIds.length, loadedIds.length)
    (sceneIds zip loadedIds) foreach ( t => assertEquals(t._1, t._2))
  }

}

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
        case LoadAllScenes =>
          val sceneIter = (dao loadAllScenes) iterator
          val iteratorActor = new IteratorActor(sceneIter)
          reply(AllScenesLoaded(iteratorActor))          
        case Close =>
          dao.close
          exit
      }
    }
  }
}