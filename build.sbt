import sbtcrossproject.CrossPlugin.autoImport.crossProject

val Scala212 = "2.12.21"
val Scala213 = "2.13.18"
val Scala3 = "3.3.7"

ThisBuild / scalaVersion := Scala212
ThisBuild / crossScalaVersions := Seq(Scala212, Scala213, Scala3)

ThisBuild / organization := "com.thesamet.scalapb"

ThisBuild / scalacOptions ++= {
  if (scalaBinaryVersion.value == "2.12") {
    Seq("-Xfuture")
  } else {
    Nil
  }
}

ThisBuild / scalacOptions ++= Seq(
  "-deprecation"
)

ThisBuild / publishTo := sonatypePublishToBundle.value

lazy val root = project
  .in(file("."))
  .aggregate(runtimeJS, runtimeJVM, runtimeNative)
  .settings(
    publish := {},
    publishLocal := {},
    publishArtifact := false
  )

lazy val protobufRuntimeScala =
  crossProject(JSPlatform, JVMPlatform, NativePlatform)
    .crossType(CrossType.Full)
    .in(file("."))
    .settings(
      name := "protobuf-runtime-scala",
      libraryDependencies ++= Seq(
        "com.lihaoyi" %%% "utest" % "0.8.9" % "test"
      ),
      scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v <= 11 => List("-target:jvm-1.7")
        case _                       => Nil
      }),
      Compile / unmanagedSourceDirectories += {
        val base =
          (LocalRootProject / baseDirectory).value / "shared" / "src" / "main"
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, 13) | (3, _)) => base / s"scala-2.13+"
          case _                      => base / s"scala-2.13-"
        }
      }
    )
    .jsSettings(
      scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((version, _)) =>
          val a = (LocalRootProject / baseDirectory).value.toURI.toString
          val g =
            "https://raw.githubusercontent.com/scalapb/protobuf-scala-runtime/" + sys.process
              .Process("git rev-parse HEAD")
              .lineStream_!
              .head
          val flag =
            if (version == 3) "-scalajs-mapSourceURI"
            else "-P:scalajs:mapSourceURI"
          List(s"$flag:$a->$g/")
        case _ => Nil
      })
    )

ThisBuild / testFrameworks += new TestFramework("utest.runner.Framework")

lazy val runtimeJS = protobufRuntimeScala.js
lazy val runtimeJVM = protobufRuntimeScala.jvm
lazy val runtimeNative = protobufRuntimeScala.native
