package net.negatory.stoptime

import scala.actors.Actor._
import android.content.Context
import android.test.AndroidTestCase
import actors.{TIMEOUT, Actor}
import java.io.Closeable

case class Result[T](value: T)

class WorkerActor extends Actor {

  start

  override def act() {
    import WorkerActor._

    loop {
      react {
        case Run(fun) => reply(fun())
      }
    }
  }
}

object WorkerActor {
  case class Run[T](fun: () => T)
}

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
  case class AllFramesLoaded(iterator: IteratorActor[Frame] with Closeable)
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
    dataActor ! Close
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

  def testCreateAndLoadFrame {
    val sceneId = createScene
    val frameData = Array[Byte](1, 2, 3)
    val frameId = dataActor !? (timeout, CreateFrame(sceneId, frameData)) match {
      case Some(FrameCreated(frameId)) => frameId
      case Some(msg) => unexpectedMsg("creating frame", msg)
      case None => msgTimeout("creating frame")
    }
    val frame = dataActor !? (timeout, LoadFrame(frameId)) match {
      case Some(FrameLoaded(frame)) => frame
      case Some(msg) => unexpectedMsg("loading frame", msg)
      case None => msgTimeout("loading frame")
    }
    assertEquals(sceneId, frame.sceneId)
    assertEquals(frameId, frame.id)
    assertEquals(frameData length, frame.frameData length)
    (frameData zip frame.frameData) foreach (t => assertEquals(t._1, t._2))
  }

  def testLoadAllFrames {
    val sceneId = createScene
    val frameData = for (x <- 1 to 10) yield Array[Byte](1)
    for (data <- frameData) dataActor ! CreateFrame(sceneId, data)
    val frameIter = dataActor !? (timeout, LoadAllFrames(sceneId)) match {
      case Some(AllFramesLoaded(iter)) => iter
      case Some(msg) => unexpectedMsg("loading all frames", msg)
      case None => msgTimeout("loading all frames")
    }
    val frameList: List[Frame] = frameIter.toList
    assertEquals(frameData length, frameList length)
    for ((frame, data) <- frameList zip frameData) {
      (data zip frame.frameData) foreach (t => assertEquals(t._1, t._2))
    }
  }
}

class DataActor(context: Context, dbName: String) extends Actor with Logging {

  start

  // All data access must be done by the worker, in a single thread
  val worker = new WorkerActor

  private val dao = new DAO(context, dbName)

  def this(context: Context) {
    this(context, "stoptime")
  }

  override def act() {
    import DataActor._
    import WorkerActor._

    def runWork[T](f: () => T): T = (worker !? Run(f)).asInstanceOf[T]

    loop {
      react {
        case CreateScene =>
          Log.d("Creating scene")
          val sceneId = runWork(dao.createScene _)
          reply(SceneCreated(sceneId))
        case LoadScene(sceneId) =>
          val scene = runWork(() => dao.loadScene(sceneId))
          reply(SceneLoaded(scene))
        case LoadAllScenes =>
          val sceneList = runWork(dao.loadAllScenes _)
          val sceneIter = sceneList iterator
          val delegateIter = new Iterator[Scene] {
            override def hasNext = runWork(sceneIter.hasNext _)
            override def next = runWork(sceneIter.next _)
          }
          val iteratorActor = new IteratorActor(delegateIter)
          reply(AllScenesLoaded(iteratorActor))
        case CreateFrame(sceneId, frameData) =>
          Log.d("Creating frame")
          val frameId = runWork(() => dao.createFrame(sceneId, frameData))
          reply(FrameCreated(frameId))
        case LoadFrame(frameId) =>
          Log.d("Loading frame")
          val frame = runWork(() => dao.loadFrame(frameId))
          reply(FrameLoaded(frame))
        case LoadAllFrames(sceneId) =>
          val frameIter = runWork(() => dao.readFrames(sceneId))
          val delegateIter = new Iterator[Frame] {
            override def hasNext = runWork(frameIter.hasNext _)
            override def next = runWork(frameIter.next _)
          }
          val iteratorActor = new IteratorActor(delegateIter) with Closeable {
            override def close = runWork(frameIter.close _)
          }
          reply(AllFramesLoaded(iteratorActor))
        case Close =>
          runWork(dao.close _)
          exit
      }
    }
  }
}