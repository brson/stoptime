import sbt._
import java.io.File

class Project(info: ProjectInfo) extends AndroidTestProject(info) {
  override def androidPlatformName = "android-8"
}
