import sbt._

class Project(info: ProjectInfo) extends DefaultWebProject(info) with IdeaProject {

  def lift(module:String) = "net.liftweb" %% ("lift-" + module) % "2.2" withSources()

  val liftWebkit   = lift("webkit")
  val liftTestKit  = lift("testkit") % "test" withSources()
  val liftWizard   = lift("wizard")
  val liftMapper   = lift("mapper")
  val liftProto    = lift("proto")
  val liftWidgets  = lift("widgets")
  val liftUtil     = lift("util")
  val liftJson     = lift("json")

  val httpDispatch = "net.databinder" %% "dispatch-http" % "0.7.8" withSources
  val jodaTime     = "joda-time" % "joda-time" % "1.6.2" withSources
  val h2           = "com.h2database" % "h2" % "1.2.138" withSources
  val postgresql   = "postgresql" % "postgresql" % "8.4-702.jdbc3" withSources
  val slf4j        = "org.slf4j" % "slf4j-log4j12" % "1.6.1" withSources
  val jclOverSlf4j = "org.slf4j" % "jcl-over-slf4j" % "1.6.1" withSources
  val log4j        = "log4j" % "log4j" % "1.2.16" withSources

  val openid4java  = "org.openid4java" % "openid4java-nodeps" % "0.9.5"

  val jettyTest    = "org.mortbay.jetty" % "jetty" % "6.1.22" % "test" withSources
  val specs        = "org.scala-tools.testing" %% "specs" % "1.6.7" % "test" withSources

  override def jettyPort = 8105
  override def scanDirectories = Nil
  override def jettyWebappPath = webappPath

  override def ivyXML =
    <dependencies>
      <exclude module="commons-logging"/>
    </dependencies>
}
