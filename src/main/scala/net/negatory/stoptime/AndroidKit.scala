package net.negatory.stoptime

import android.database.Cursor


object AndroidKit {
  class CursorIterator(cursor: Cursor) extends Iterator[Cursor] {

    def hasNext = !(cursor isLast) && !(cursor isAfterLast)

    def next = {
      cursor.moveToNext
      cursor
    }

  }

  object CursorIterator {
    def apply(cursor: Cursor) = new CursorIterator(cursor)
  }
}