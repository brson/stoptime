package net.negatory.stoptime;

import android.app.ListActivity
import android.os.Bundle
import android.widget.{ListView, ArrayAdapter}
import android.view.{MenuItem, Menu, View}
import android.content.{Intent}
import android.net.Uri


class ScenePickActivity extends ListActivity {
  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    setListAdapter (
      // todo: Create ListAdapter
      new ArrayAdapter [String] (
        this,
        android.R.layout.simple_list_item_1,
        getScenes
        )
      )
  }

  private def getScenes: Array[String] = {
    val dao = new DAO(this)
    for (scene <- dao.loadAllScenes.toArray) yield scene.id.toString
  }

  override def onListItemClick(l: ListView, v: View, position: Int, id: Long) {
    val intent = new Intent("edit", Uri.fromParts("stoptime", "scene", getScenes apply(position) toString), this, classOf[EditorActivity])
    startActivity(intent)
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

    val intent = new Intent(this, classOf[EditorActivity])
    startActivity(intent)
  }

}
