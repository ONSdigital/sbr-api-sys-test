import sbt.Keys.organizationName

/**
  * APP CONFIG
  */
lazy val applicationConfig: SettingKey[Map[String, String]] = settingKey[Map[String, String]]("config values")

/**
  * HARD VARS
  */
lazy val Versions = new {
  val scala = "2.11.11"
  val appVersion = "0.1"
  val scapegoatVersion = "1.1.0"
}

lazy val Constant = new {
  val projectStage = "alpha"
  val team = "sbr"
  val local = "mac"
  val repoName = "sbr-apis-sys-test"
}


lazy val devDeps = Seq(
  "com.typesafe.play"   %% "play-ws"    % "2.5.18",
  "com.typesafe"        %  "config"     % "1.3.1",
  "org.scalatest"       %% "scalatest"  % "3.0.3"     % Test
)

lazy val Resolvers = Seq(
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)

lazy val commonSettings: Seq[Def.Setting[_]] = Seq (
  scalacOptions in ThisBuild ++= Seq(
    "-language:experimental.macros",
    "-target:jvm-1.8",
    "-encoding", "UTF-8",
    "-language:reflectiveCalls",
    "-language:experimental.macros",
    "-language:implicitConversions",
    "-language:higherKinds",
    "-language:postfixOps",
    "-deprecation", // warning and location for usages of deprecated APIs
    "-feature", // warning and location for usages of features that should be imported explicitly
    "-unchecked", // additional warnings where generated code depends on assumptions
    "-Xlint", // recommended additional warnings
    "-Xcheckinit", // runtime error when a val is not initialized due to trait hierarchies (instead of NPE somewhere else)
    "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver
    //"-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver
    "-Ywarn-value-discard", // Warn when non-Unit expression results are unused
    "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures
    "-Ywarn-dead-code", // Warn when dead code is identified
    "-Ywarn-unused", // Warn when local and private vals, vars, defs, and types are unused
    "-Ywarn-unused-import", //  Warn when imports are unused (don't want IntelliJ to do it automatically)
    "-Ywarn-numeric-widen" // Warn when numerics are widened
  ),
  libraryDependencies ++= devDeps,
  resolvers ++= Resolvers
)


lazy val root = (project in file("."))
  .settings(commonSettings:_*)
  .settings(
    inThisBuild(List(
      scalaVersion := Versions.scala,
      version      := (version in ThisBuild).value
    )),
    organizationName := "ons",
    organization := "uk.gov.ons",
    developers := List(Developer("Adrian Harris (Tech Lead)", "SBR", "ons-sbr-team@ons.gov.uk", new java.net.URL(s"https:///v1/home"))),
    licenses := Seq("MIT-License" -> url("https://github.com/ONSdigital/sbr-control-api/blob/master/LICENSE")),
    startYear := Some(2017),
    name := s"${organizationName.value}-${Constant.team}-${Constant.repoName}"
  )
