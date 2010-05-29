package net.negatory.stoptime

import android.database.sqlite.{SQLiteOpenHelper, SQLiteDatabase}
import android.content.Context
import java.nio.ByteBuffer
import java.util.EnumSet
import android.os.Environment
import runtime.RichInt
import java.lang.Integer
import java.io._
import android.database.Cursor
import AndroidKit.CursorIterator


class StoptimeOpenHelper(context: Context, dbName: String)
  extends SQLiteOpenHelper(context, dbName, null, StoptimeOpenHelper.version) {

  override def onCreate(db: SQLiteDatabase) {
    db.execSQL("create table scene (id integer primary key autoincrement)")
    db.execSQL("create table frame (id integer primary key autoincrement, sceneId integer, " +
      "foreign key(sceneId) references scene(id))")
  }

  override def onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    assert(false, "DB upgrade not implemented")
  }
}

private object StoptimeOpenHelper {
  val version: Int = 1
}

abstract class PlaybackIterator extends Iterator[Frame] with Closeable

// dbName may be null to create an in-memory database
class DAO(context: Context, dbName: String) extends AnyRef with Logging with Closeable {

  def this(context: Context) {
    this(context, "stoptime")
  }

  val dbHelper = new StoptimeOpenHelper(context, dbName)
  val db = dbHelper.getWritableDatabase

  def close() {
    db.close
  }

  def createScene: Int = {

    db.execSQL("insert into scene default values")
    val cursor = db.rawQuery("select id from scene where rowid=last_insert_rowid()", null)
    try {
      cursor.moveToNext
      val newSceneId = cursor.getInt(0)
      Log.d("Created new scene with id = " + newSceneId)
      newSceneId
    } finally {
      cursor.close
    }
  }

  def loadScene(sceneId: Int): Scene = {
    // TODO
    new Scene(sceneId, this)
  }

  def loadAllScenes: List[Scene] = {
    Log.d("Loading all scenes")
    val cursor = db.rawQuery("select id from scene order by id", null)

    try {

      (for (c <- new CursorIterator(cursor)) yield {
        val sceneId = cursor.getInt(0)
        Log.d("Loading scene " + sceneId)
        new Scene(sceneId, this)
      }) toList
    } finally {
      cursor.close
    }

  }

  def createFrame(sceneId: Int, frameData: Array[Byte]): Int = {
    // todo need a transaction
    db.execSQL("insert into frame default values")
    val cursor = db.rawQuery("select id from frame where rowid=last_insert_rowid()", null)
    cursor.moveToNext
    val newFrameId = cursor.getInt(0)

    cursor.close
    db.execSQL("update frame set sceneId=? where id=?",
      Array[AnyRef](sceneId: java.lang.Integer, newFrameId: java.lang.Integer))
    Log.d("Created new frame for scene " + sceneId + " with id = " + newFrameId)

    saveFrameToFile(sceneId, newFrameId, frameData)
    newFrameId
  }


  def loadFrame(frameId: Int): Frame = {

    val cursor = db.rawQuery(
      "select sceneId from frame where id=?"
      , Array[String](frameId.toString))
    cursor.moveToNext
    assert(!cursor.isAfterLast, "Didn't find the requested frame")

    val sceneId = cursor.getInt(0)
    Log.d("Loaded frame has sceneId " + sceneId)
    cursor.close
    val frameData = loadFrameFromFile(sceneId, frameId)
    new Frame(frameId, sceneId, frameData)
  }

  def readFrames(sceneId: Int): PlaybackIterator = {

    val cursor = db.rawQuery(
      "select id from frame where sceneId=? order by id"
      , Array[String](sceneId.toString));

    new PlaybackIterator {
      override def hasNext =  !(cursor.isLast || cursor.isAfterLast)

      override def next = {
        cursor.moveToNext
        val frameId = cursor.getInt(0)
        Log.d("Loading frame " + frameId + " for playback")
        loadFrame(frameId)
      }

      override def close = cursor.close
    }
  }

  private def saveFrameToFile(sceneId: Int, frameId: Int, frameData: Array[Byte]) {

    try {
      val frameFile = getFrameFile(sceneId, frameId)
      val fileStream: FileOutputStream = new FileOutputStream(frameFile)

      try {
        Log.d("Writing " + frameData.length + " bytes to frame file " + frameFile.toString)
        fileStream.write(frameData)
      }
      finally {
        fileStream.close
      }
    }
    catch {
      case e: IOException => throw e
    }
  }

  private def loadFrameFromFile(sceneId: Int, frameId: Int): Array[Byte] = {
    try {
      val frameFile = getFrameFile(sceneId, frameId)
      val fileStream: FileInputStream = new FileInputStream(frameFile)

      try {
        val frameData: Array[Byte] = new Array[Byte](frameFile.length.toInt)
        Log.d("Reading " + frameData.length + " bytes from frame file " + frameFile.toString)
        fileStream.read(frameData)
        frameData
      }
      finally {
        fileStream.close
      }
    }
    catch {
      case e: IOException => throw e
    }
  }

  private def getFrameFile(sceneId: Int, frameId: Int): File = {

    val root: File = Environment.getExternalStorageDirectory
    val frameFileName = "%04X-%04X".format(sceneId, frameId) + ".jpg"
    val storageDir = new File(root, "flipbook")
    storageDir.mkdirs // TODO This side affect doesn't belong here
    new File(storageDir, frameFileName)
  }


}