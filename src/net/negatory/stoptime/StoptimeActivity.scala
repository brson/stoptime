package net.negatory.stoptime;

import android.app.ListActivity
import android.os.Bundle
import android.view.View
import android.widget.{ListView, ArrayAdapter}
import android.content.Intent

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
    val intent = new Intent(this, classOf[EditorActivity])
    startActivity(intent)
  }
}
