package net.negatory.stoptime

import android.database.sqlite.{SQLiteOpenHelper, SQLiteDatabase}
import android.content.Context

class StoptimeOpenHelper(context: Context)
  extends SQLiteOpenHelper(context, "stoptime", null, StoptimeOpenHelper.version) {

  override def onCreate(db: SQLiteDatabase) {
    db.execSQL("create table scene (id integer primary key autoincrement)")
  }

  override def onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    assert(false, "DB upgrade not implemented")
  }
}

private object StoptimeOpenHelper {
  val version: Int = 1
}

class DAO(context: Context) extends AnyRef with Logging {

  val dbHelper = new StoptimeOpenHelper(context)
  val db = dbHelper.getWritableDatabase

  def createScene: Int = {

    db.execSQL("insert into scene default values")
    val cursor = db.rawQuery("select id from scene where rowid=last_insert_rowid()", null)
    cursor.moveToNext
    val newSceneId = cursor.getInt(0)
    Log.d("Created new scene with id = " + newSceneId)
    cursor.close
    db.close
    newSceneId
  }

  def loadScene(sceneId: Int): Scene = {
    // TODO
    new Scene(sceneId, this)
  }

  def createFrame(sceneId: Int, imageData: Array[Byte]): Int = {
    // TODO
    0
  }

  def loadFrame(sceneId: Int, frameId: Int): Frame = {
    // TODO
    new Frame
  }
}