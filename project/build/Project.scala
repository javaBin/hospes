import sbt._

class Project(info: ProjectInfo) extends DefaultWebProject(info) {
  val liftVersion = "2.1"
  val liftWebkit  = "net.liftweb" %% "lift-webkit" % liftVersion % "compile->default" withSources
  val liftTestKit = "net.liftweb" %% "lift-testkit" % liftVersion % "compile->default" withSources
  val liftWizard  = "net.liftweb" %% "lift-wizard" % liftVersion % "compile->default" withSources
  val liftMapper  = "net.liftweb" %% "lift-mapper" % liftVersion % "compile->default" withSources
  val liftOpenId  = "net.liftweb" %% "lift-openid" % liftVersion % "compile->default" withSources
  val h2          = "com.h2database" % "h2" % "1.2.138" withSources
  val jettyTest   = "org.mortbay.jetty" % "jetty" % "6.1.22" % "test->default" withSources
  val junit       = "junit" % "junit" % "4.5" % "test->default" withSources
  val specs       = "org.scala-tools.testing" %% "specs" % "1.6.5" % "test->default" withSources

  override def scanDirectories = Nil
  override def jettyWebappPath = webappPath
}
