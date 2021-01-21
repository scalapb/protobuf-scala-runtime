import ReleaseTransformations._
import sbtcrossproject.CrossPlugin.autoImport.crossProject

val Scala211 = "2.11.12"

val Scala212 = "2.12.10"

val Scala213 = "2.13.1"

scalaVersion in ThisBuild := Scala212

organization in ThisBuild := "com.thesamet.scalapb"

scalacOptions in ThisBuild ++= Seq(
  "-Xfuture"
)

scalacOptions in ThisBuild ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, v)) if v <= 11 => List("-target:jvm-1.7")
    case _                       => Nil
  }
}

releaseCrossBuild := true

releasePublishArtifactsAction := PgpKeys.publishSigned.value

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining(
    s"+publishSigned;++${Scala211};protobufRuntimeScalaNative/publishSigned"
  ),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

publishTo in ThisBuild := sonatypePublishToBundle.value

lazy val root = project
  .in(file("."))
  .aggregate(runtimeJS, runtimeJVM)
  .settings(
    publish := {},
    publishLocal := {},
    publishArtifact := false
  )

lazy val protobufRuntimeScala =
  crossProject(JSPlatform, JVMPlatform, NativePlatform)
    .in(file("."))
    .settings(
      name := "protobuf-runtime-scala",
      libraryDependencies ++= {
        Seq(
          "com.lihaoyi" %%% "utest" % "0.7.6" % "test"
        ),
      },
      unmanagedSourceDirectories in Compile += {
        val base =
          (baseDirectory in LocalRootProject).value / "shared" / "src" / "main"
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, v)) if v >= 13 =>
            base / s"scala-2.13+"
          case _ =>
            base / s"scala-2.13-"
        }
      }
    )
    .jvmSettings(
      // Add JVM-specific settings here
      crossScalaVersions in ThisBuild := Seq(Scala212, Scala213)
    )
    .jsSettings(
      // Add JS-specific settings here
      crossScalaVersions in ThisBuild := Seq(Scala212, Scala213),
      scalacOptions += {
        val a = (baseDirectory in LocalRootProject).value.toURI.toString
        val g =
          "https://raw.githubusercontent.com/scalapb/protobuf-scala-runtime/" + sys.process
            .Process("git rev-parse HEAD")
            .lineStream_!
            .head
        s"-P:scalajs:mapSourceURI:$a->$g/"
      }
    )
    .nativeSettings(
      scalaVersion := Scala211,
      nativeLinkStubs := true // for utest
    )

testFrameworks in ThisBuild += new TestFramework("utest.runner.Framework")

lazy val runtimeJS = protobufRuntimeScala.js
lazy val runtimeJVM = protobufRuntimeScala.jvm
lazy val runtimeNative = protobufRuntimeScala.native
