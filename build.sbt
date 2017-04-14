import sbt.Keys._
import ReleaseTransformations._
import sbtrelease.{Version, versionFormatError}

lazy val appVendor = "com.interelgroup"
lazy val appName = "core3-example-engine"

organization := appVendor
name := appName

lazy val scalaV = "2.11.8"

lazy val nettyOverrides = Set(
  "io.netty" % "netty-codec-http" % "4.0.41.Final",
  "io.netty" % "netty-handler" % "4.0.41.Final",
  "io.netty" % "netty-codec" % "4.0.41.Final",
  "io.netty" % "netty-transport" % "4.0.41.Final",
  "io.netty" % "netty-buffer" % "4.0.41.Final",
  "io.netty" % "netty-common" % "4.0.41.Final",
  "io.netty" % "netty-transport-native-epoll" % "4.0.41.Final"
)

lazy val defaultResolvers = Seq(
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/releases",
  "lightshed-maven" at "http://dl.bintray.com/content/lightshed/maven"
)

lazy val core3_example_engine = (project in file("."))
  .settings(
    organization := appVendor,
    name := appName,
    scalaVersion := scalaV,
    resolvers ++= defaultResolvers,
    libraryDependencies ++= Seq(
      "org.jline" % "jline" % "3.2.0",
      "com.github.scopt" %% "scopt" % "3.5.0",
      "com.github.etaty" %% "rediscala" % "1.8.0",
      "com.interelgroup" %% "core3" % "1.1.0",
      "net.codingwell" %% "scala-guice" % "4.0.1",
      "org.scalatest" %% "scalatest" % "3.0.0" % Test
    ),
    dependencyOverrides ++= nettyOverrides,
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
