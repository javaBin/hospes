import sbt._

class Project(info: ProjectInfo) extends DefaultWebProject(info) {
  val liftVersion  = "2.1"
  val liftWebkit   = "net.liftweb" %% "lift-webkit" % liftVersion withSources
  val liftTestKit  = "net.liftweb" %% "lift-testkit" % liftVersion withSources
  val liftWizard   = "net.liftweb" %% "lift-wizard" % liftVersion withSources
  val liftMapper   = "net.liftweb" %% "lift-mapper" % liftVersion withSources
  val liftWidgets  = "net.liftweb" %% "lift-widgets" % liftVersion withSources
  val liftUtil     = "net.liftweb" %% "lift-util" % liftVersion withSources
  val liftJson     = "net.liftweb" %% "lift-json" % liftVersion withSources
  val httpDispatch = "net.databinder" %% "dispatch-http" % "0.7.8" withSources
  val h2           = "com.h2database" % "h2" % "1.2.138" withSources
  val jodaTime     = "joda-time" % "joda-time" % "1.6.2" withSources

  val jettyTest    = "org.mortbay.jetty" % "jetty" % "6.1.22" % "test->default" withSources
  val junit        = "junit" % "junit" % "4.5" % "test->default" withSources
  val specs        = "org.scala-tools.testing" %% "specs" % "1.6.5" % "test->default" withSources

  override def scanDirectories = Nil
  override def jettyWebappPath = webappPath
}
