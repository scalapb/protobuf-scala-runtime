import ReleaseTransformations._
import sbtcrossproject.CrossPlugin.autoImport.crossProject

val Scala211 = "2.11.12"

crossScalaVersions in ThisBuild := Seq("2.10.6", Scala211, "2.12.1", "2.13.0-M2")

scalaVersion in ThisBuild := Scala211

organization in ThisBuild := "com.thesamet.scalapb"

scalacOptions in ThisBuild ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, v)) if v <= 11 => List("-target:jvm-1.7")
    case _ => Nil
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
  releaseStepCommandAndRemaining(s";++${Scala211};protobufRuntimeScalaNative/publishSigned"),
  ReleaseStep(action = Command.process("publishSigned", _), enableCrossBuild = true),
  setNextVersion,
  commitNextVersion,
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _), enableCrossBuild = true),
  pushChanges
)

lazy val root = project.in(file(".")).
  aggregate(runtimeJS, runtimeJVM).
  settings(
    publish := {},
    publishLocal := {},
    publishArtifact := false
  )

lazy val protobufRuntimeScala = crossProject(JSPlatform, JVMPlatform, NativePlatform).in(file("."))
  .settings(
    name := "protobuf-runtime-scala",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "utest" % "0.6.3" % "test"
    )
  )
  .jvmSettings(
    // Add JVM-specific settings here
  )
  .jsSettings(
    // Add JS-specific settings here
    scalacOptions += {
      val a = (baseDirectory in LocalRootProject).value.toURI.toString
      val g = "https://raw.githubusercontent.com/scalapb/protobuf-scala-runtime/" + sys.process.Process("git rev-parse HEAD").lines_!.head
      s"-P:scalajs:mapSourceURI:$a->$g/"
    }
  )
  .nativeSettings(
    nativeLinkStubs := true // for utest
  )
  
testFrameworks in ThisBuild += new TestFramework("utest.runner.Framework")

lazy val runtimeJS = protobufRuntimeScala.js
lazy val runtimeJVM = protobufRuntimeScala.jvm
lazy val runtimeNative = protobufRuntimeScala.native
