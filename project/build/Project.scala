import sbt._

class Project(info: ProjectInfo) extends DefaultWebProject(info) {

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

  val jettyTest    = "org.mortbay.jetty" % "jetty" % "6.1.22" % "test" withSources
  val specs        = "org.scala-tools.testing" %% "specs" % "1.6.7" % "test" withSources

  override def jettyPort = 8090
  override def scanDirectories = Nil
  override def jettyWebappPath = webappPath
}
