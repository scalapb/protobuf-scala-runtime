import sbtcrossproject.CrossPlugin.autoImport.crossProject

val Scala212 = "2.12.21"
val Scala213 = "2.13.18"
val Scala3 = "3.3.8"
val ProtobufJava = "4.36.2"

lazy val generateWireFixtures = taskKey[Seq[File]](
  "Generate shared wire-format test sources with Google's protobuf runtime"
)

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

lazy val root = project
  .in(file("."))
  .aggregate(runtimeJS, runtimeJVM, runtimeNative)
  .settings(
    publish / skip := true,
    generateWireFixtures := (wireFixtureGenerator / generateWireFixtures).value,
    generateWireFixtures / aggregate := false
  )

lazy val protobufRuntimeScala =
  crossProject(JSPlatform, JVMPlatform, NativePlatform)
    .crossType(CrossType.Full)
    .in(file("."))
    .settings(
      name := "protobuf-runtime-scala",
      libraryDependencies ++= Seq(
        "com.lihaoyi" %%% "utest" % "0.9.5" % "test"
      ),
      scalacOptions ++= Seq("-release:8"),
      Test / sourceGenerators +=
        (wireFixtureGenerator / generateWireFixtures).taskValue,
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

// Keep this helper outside the runtime aggregate and library classpaths:
// Google's classes have the same names as ours and run only in the generator JVM.
lazy val wireFixtureGenerator = project
  .in(file("tools/wire-fixtures"))
  .settings(
    scalaVersion := Scala212,
    autoScalaLibrary := false,
    scalacOptions := Nil,
    crossPaths := false,
    crossScalaVersions := Nil,
    publish / skip := true,
    libraryDependencies += "com.google.protobuf" % "protobuf-java" % ProtobufJava,
    generateWireFixtures := {
      val classpath = (Compile / fullClasspath).value.files
      val output =
        target.value / "generated-test-sources" / "WireFixtures.scala"
      val javaExecutable = javaHome.value
        .getOrElse(file(sys.props("java.home"))) / "bin" / "java"
      val log = streams.value.log
      val inputs = classpath.flatMap { entry =>
        if (entry.isDirectory) (entry ** "*.class").get else Seq(entry)
      }.toSet
      val generate = FileFunction.cached(
        streams.value.cacheDirectory / "wire-fixtures",
        FilesInfo.lastModified,
        FilesInfo.exists
      ) { _: Set[File] =>
        val source = sys.process
          .Process(
            Seq(
              javaExecutable.getAbsolutePath,
              "-cp",
              classpath.mkString(java.io.File.pathSeparator),
              "GenerateWireFixtures",
              ProtobufJava
            )
          )
          .!!
        IO.write(output, source)
        log.info(s"Generated wire fixtures: $output")
        Set(output)
      }
      generate(inputs).toSeq
    }
  )
