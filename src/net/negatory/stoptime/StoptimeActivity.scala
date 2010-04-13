package net.negatory.stoptime;

import android.app.ListActivity
import android.os.Bundle
import android.widget.{ListView, ArrayAdapter}
import android.view.{MenuItem, Menu, View}
import android.content.{Context, Intent}
import android.database.sqlite.{SQLiteOpenHelper, SQLiteDatabase}

class StoptimeOpenHelper(context: Context)
  extends SQLiteOpenHelper(context, "stoptime", null, 1) {

  override def onCreate(db: SQLiteDatabase) {
    db.execSQL("create table scene (id integer primary key autoincrement)")
  }

  override def onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    assert(false, "DB upgrade not implemented")
  }
}

class StoptimeActivity extends ListActivity {
  override def onCreate(savedInstanceState: Bundle)
    {
      super.onCreate(savedInstanceState)

      setListAdapter (
        new ArrayAdapter [String] (
          this,
          android.R.layout.simple_list_item_1,
          Array ("scene1", "scene2")
          )
        )
    }

  override def onListItemClick(l: ListView, v: View, position: Int, id: Long) {
    
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    menu.add(0, 0, 0, "New")
    true
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = item.getItemId match {
    case 0 => newScene; true
    case _ => false
  }

  def newScene {
    val sceneId = createScene

    val intent = new Intent(this, classOf[EditorActivity])
    startActivity(intent)
  }

  def createScene: Int = {
    val dbHelper = new StoptimeOpenHelper(this)
    val db = dbHelper.getWritableDatabase
    db.execSQL("insert into scene default values")
    val cursor = db.rawQuery("select id from scene where rowid=last_insert_rowid()", null)
    cursor.moveToNext
    val newSceneId = cursor.getInt(0)
    cursor.close
    db.close
    newSceneId
  }
}
