package net.negatory.stoptime

import android.graphics.{BitmapFactory, Bitmap}

class Frame(val id: Int, val sceneId: Int, frameData: Array[Byte]) extends AnyRef with Logging {

   def createFrameBitmap: Bitmap = BitmapFactory.decodeByteArray(frameData, 0, frameData.size)
}