
lazy val root = project
  .in(file("."))
  .aggregate(bot, web, jsui)
  .settings(
    scalaVersion := scalaV,
    name := "root"
  )

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
resolvers += "Twitter Maven" at "http://maven.twttr.com"

// (5) shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtcrossproject.{crossProject, CrossType}


name := "TiniBot2.0"
version := "1.0"

scalaVersion := scalaV
lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full) // [Pure, Full, Dummy], default: CrossType.Full.in(file("shared"))
  .in(file("."))
  .settings(
    name := "tini-shared",
    scalaVersion := scalaV,
    libraryDependencies ++= Seq()
  )
lazy val sharedJvm = shared.jvm
  //.in(file("sharedJvm"))
  .settings(
  scalaVersion := scalaV,
  name := "tini-sharedJvm"
)
lazy val sharedJs = shared.js
  //.in(file("sharedJs"))
  .settings(
  scalaVersion := scalaV,
  name := "tini-sharedJs"
)
lazy val bot = project
  .in(file("bot"))
  .settings(
    scalaVersion := scalaV,
    name := "tini-bot")
  .dependsOn(sharedJvm)
lazy val web = project
  .in(file("web"))
  .settings(
    scalaVersion := scalaV,
    name := "tini-web")
  .dependsOn(sharedJvm)
lazy val jsui = project
  .in(file("jsui"))
  .settings(
    scalaVersion := scalaV,
    name := "tini-jsui")
  .dependsOn(sharedJs)
val scalaV = "2.12.2"







