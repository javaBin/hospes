name := "hospes"

organization := "no.java"

version := "1-SNAPSHOT"

scalaVersion := "2.9.1"

transitiveClassifiers := Seq("sources")

//seq(webSettings :_*)

//Lift dependencies
def lift(module:String, scope:String = "compile") = "net.liftweb" %% ("lift-" + module) % "2.4" % scope

libraryDependencies ++= Seq(lift("webkit"), lift("wizard"), lift("mapper"), lift("proto"), lift("widgets"), lift("util"), lift("json"), lift("testkit", "test"))

//env in Compile := Some(file(".") / "jetty-env.xml" asFile)

libraryDependencies += "net.databinder" %% "dispatch-http" % "0.7.8"

libraryDependencies += "joda-time" % "joda-time" % "1.6.2"

libraryDependencies += "com.h2database" % "h2" % "1.2.138"

libraryDependencies += "postgresql" % "postgresql" % "8.4-702.jdbc3"

libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.6.1"

libraryDependencies += "org.slf4j" % "jcl-over-slf4j" % "1.6.1"

libraryDependencies += "log4j" % "log4j" % "1.2.16"

libraryDependencies += "org.openid4java" % "openid4java-nodeps" % "0.9.5"

libraryDependencies += "net.sourceforge.nekohtml" % "nekohtml" % "1.9.10"

libraryDependencies += "commons-httpclient" % "commons-httpclient" % "3.1"

//libraryDependencies += "org.eclipse.jetty" % "jetty-plus" % "7.4.5.v20110725" % "container"
jetty()


libraryDependencies += "org.scala-tools.testing" %% "specs" % "1.6.9" % "test"

libraryDependencies += "javax.servlet" % "servlet-api" % "2.5" % "test"

unmanagedClasspath in Test <+= (baseDirectory) map { bd => Attributed.blank(bd / "src/main/webapp") }


scalacOptions += "-deprecation"
