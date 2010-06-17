package net.negatory.stoptime

import junit.framework.Assert.fail

object TestTools {
  val timeout = 1000

  def unexpectedMsg(doingWhat: String, msg: Any): Nothing = {
    fail("Unexpected message %s: %s".format(doingWhat, msg))
    throw new RuntimeException
  }
  def msgTimeout(doingWhat: String): Nothing = {
    fail("Timeout while " + doingWhat)
    throw new RuntimeException
  }
}
