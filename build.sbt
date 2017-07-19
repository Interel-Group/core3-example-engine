import sbt.Keys._
import ReleaseTransformations._
import sbtrelease.{Version, versionFormatError}

lazy val appVendor = "com.interelgroup"
lazy val appName = "core3-example-engine"

organization := appVendor
name := appName

scalaVersion in ThisBuild := "2.12.2"

lazy val defaultResolvers = Seq(
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/releases",
  "lightshed-maven" at "http://dl.bintray.com/content/lightshed/maven"
)

lazy val core3_example_engine = (project in file("."))
  .settings(
    organization := appVendor,
    name := appName,
    resolvers ++= defaultResolvers,
    libraryDependencies ++= Seq(
      guice,
      "org.jline" % "jline" % "3.2.0",
      "com.github.scopt" %% "scopt" % "3.5.0",
      "com.github.etaty" %% "rediscala" % "1.8.0",
      "com.interelgroup" %% "core3" % "2.1.0",
      "org.scalatest" %% "scalatest" % "3.0.3" % Test
    ),
    buildInfoKeys := Seq[BuildInfoKey](organization, name, version),
    buildInfoPackage := "core3_example_engine",
    buildInfoObject := "BuildInfo",
    logBuffered in Test := false,
    parallelExecution in Test := false
  )
  .enablePlugins(PlayScala, BuildInfoPlugin)

//loads the Play project at sbt startup
onLoad in Global := (Command.process("project core3_example_engine", _: State)) compose (onLoad in Global).value
scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")

//Release Config
releaseVersion := {
  v =>
    Version(v).map {
      version =>
        val next = System.getProperty("release-version-bump", "bugfix") match {
          case "major" => version.withoutQualifier.bump(sbtrelease.Version.Bump.Major)
          case "minor" => version.withoutQualifier.bump(sbtrelease.Version.Bump.Minor)
          case "bugfix" => version.withoutQualifier
        }

        next.string
    }.getOrElse(versionFormatError)
}

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion,
  pushChanges
)
