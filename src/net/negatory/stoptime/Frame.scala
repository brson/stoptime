package net.negatory.stoptime

import android.graphics.{BitmapFactory, Bitmap}

class Frame(val sceneId: Int, val id: Int, frameData: Array[Byte]) extends AnyRef with Logging {

   val frameBitmap: Bitmap = BitmapFactory.decodeByteArray(frameData, 0, frameData.size)
}