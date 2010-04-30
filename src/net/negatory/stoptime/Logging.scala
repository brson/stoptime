package net.negatory.stoptime

import android.util.{Log => AndroidLog}

trait Logging {

  val tag: String = getClass.getSimpleName

  object Log {
    def d(msg: String) = AndroidLog.d(tag, msg)
    def i(msg: String) = AndroidLog.i(tag, msg)
    def w(msg: String) = AndroidLog.w(tag, msg)
    def e(msg: String) = AndroidLog.e(tag, msg)
  }
}