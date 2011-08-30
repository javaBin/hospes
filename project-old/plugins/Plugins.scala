import sbt._

class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
    val mpeltonen = "mpeltonen" at "http://mpeltonen.github.com/maven/"
    val sbtIdea = "com.github.mpeltonen" % "sbt-idea-plugin" % "0.3.0"
}
