import sbt._
import java.io.File

class Project(info: ProjectInfo) extends AndroidTestProject(info) {
  override def androidPlatformName = "android-8"

  val scalaRoot = "/home/brian/Dev/scala"
  override def localScala = defineScala("2.8.0-local", new File(scalaRoot + "/build/pack")) :: Nil
}
