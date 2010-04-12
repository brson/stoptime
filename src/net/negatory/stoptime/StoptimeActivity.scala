package net.negatory.stoptime;

import android.app.ListActivity
import android.os.Bundle
import android.widget.{ListView, ArrayAdapter}
import android.content.Intent
import android.view.{MenuItem, Menu, View}

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
    case 0 =>
      val intent = new Intent(this, classOf[EditorActivity])
      startActivity(intent)
      true
    case _ => false
  }
}
