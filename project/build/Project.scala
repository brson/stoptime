import sbt._
import java.io.File

class Project(info: ProjectInfo) extends AndroidProject(info) {
  override def androidPlatformName = "android-8"
}
