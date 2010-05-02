package net.negatory.stoptime

import android.database.sqlite.{SQLiteOpenHelper, SQLiteDatabase}
import android.content.Context

class StoptimeOpenHelper(context: Context, dbName: String)
  extends SQLiteOpenHelper(context, dbName, null, StoptimeOpenHelper.version) {

  override def onCreate(db: SQLiteDatabase) {
    db.execSQL("create table scene (id integer primary key autoincrement)")
    db.execSQL("create table frame (id integer primary key autoincrement, sceneId integer, frameData blob, " +
      "foreign key(sceneId) references scene(id))")
  }

  override def onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    assert(false, "DB upgrade not implemented")
  }
}

private object StoptimeOpenHelper {
  val version: Int = 1
}

// dbName may be null to create an in-memory database
class DAO(context: Context, dbName: String) extends AnyRef with Logging {

  val dbHelper = new StoptimeOpenHelper(context, dbName)
  val db = dbHelper.getWritableDatabase

  def close() {
    db.close
  }

  def createScene: Int = {

    db.execSQL("insert into scene default values")
    val cursor = db.rawQuery("select id from scene where rowid=last_insert_rowid()", null)
    cursor.moveToNext
    val newSceneId = cursor.getInt(0)
    Log.d("Created new scene with id = " + newSceneId)
    cursor.close
    newSceneId
  }

  def loadScene(sceneId: Int): Scene = {
    // TODO
    new Scene(sceneId, this)
  }

  def createFrame(sceneId: Int, imageData: Array[Byte]): Int = {
    // todo need a transaction
    db.execSQL("insert into frame default values")
    val cursor = db.rawQuery("select id from frame where rowid=last_insert_rowid()", null)
    cursor.moveToNext
    val newFrameId = cursor.getInt(0)

    cursor.close
    db.execSQL("update frame set sceneId=?, frameData=? where id=?",
      Array[AnyRef](sceneId: java.lang.Integer, imageData, newFrameId: java.lang.Integer))
    Log.d("Created new frame for scene " + sceneId + " with id = " + newFrameId)
    newFrameId
  }

  def loadFrame(frameId: Int, sceneId: Int): Frame = {
    // TODO
    new Frame(frameId, sceneId, null)
  }
}