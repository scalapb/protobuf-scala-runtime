import sbtcrossproject.CrossPlugin.autoImport.crossProject

val Scala212 = "2.12.21"
val Scala213 = "2.13.18"
val Scala3 = "3.3.8"
val ProtobufJava = "4.36.2"

lazy val generateWireFixtures = taskKey[Seq[File]](
  "Generate shared wire-format test sources with Google's protobuf runtime"
)

scalaVersion := Scala212
crossScalaVersions := Seq(Scala212, Scala213, Scala3)

organization := "com.thesamet.scalapb"

scalacOptions ++= {
  if (scalaBinaryVersion.value == "2.12") {
    Seq("-Xfuture")
  } else {
    Nil
  }
}

scalacOptions ++= Seq(
  "-deprecation"
)

lazy val root = project
  .in(file("."))
  .aggregate(
    runtimeJS,
    runtimeJVM,
    runtimeNative,
    wireFixtureGenerator,
    binaryConsumer,
    integrationJVM,
    integrationJS,
    integrationNative
  )
  .settings(
    publish / skip := true,
    generateWireFixtures := Def.uncached {
      (wireFixtureGenerator / generateWireFixtures).value
    },
    generateWireFixtures / aggregate := false
  )

lazy val protobufRuntimeScala =
  crossProject(JSPlatform, JVMPlatform, NativePlatform)
    .crossType(CrossType.Full)
    .nativeSettings(nativeTestSettings)
    .in(file("."))
    .settings(
      name := "protobuf-runtime-scala",
      libraryDependencies ++= Seq(
        "com.lihaoyi" %% "utest" % "0.9.5" % "test"
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
              .lazyLines_!
              .head
          val flag =
            if (version == 3) "-scalajs-mapSourceURI"
            else "-P:scalajs:mapSourceURI"
          List(s"$flag:$a->$g/")
        case _ => Nil
      })
    )

testFrameworks += new TestFramework("utest.runner.Framework")

lazy val runtimeJS = protobufRuntimeScala.js
lazy val runtimeJVM = protobufRuntimeScala.jvm
lazy val runtimeNative =
  protobufRuntimeScala.native

// Aggregate this helper so Scala Steward discovers its dependency, but do not
// add it to runtime classpaths: Google's classes run only in the generator JVM.
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
    generateWireFixtures := Def.uncached {
      implicit val converter: xsbti.FileConverter = fileConverter.value
      val classpath = (Compile / fullClasspath).value.files.map(_.toFile)
      val output =
        target.value / "generated-test-sources" / "WireFixtures.scala"
      val javaExecutable = javaHome.value
        .getOrElse(file(sys.props("java.home"))) / "bin" / "java"
      val log = streams.value.log
      val inputs = classpath.flatMap { entry =>
        if (entry.isDirectory) (entry ** "*.class").get() else Seq(entry)
      }.toSet
      val generate = FileFunction.cached(
        streams.value.cacheDirectory / "wire-fixtures",
        FilesInfo.lastModified,
        FilesInfo.exists
      ) { (_: Set[File]) =>
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

// Compile a real downstream caller against the last release, then execute its
// unchanged class files with this build. JAVA8_HOME also tests the Java 8 floor.
lazy val compatibilitySmoke =
  taskKey[Unit]("Run a previously compiled consumer with the current runtime")
lazy val binaryConsumer = project
  .in(file("tools/binary-consumer"))
  .settings(
    publish / skip := true,
    scalacOptions += "-release:8",
    libraryDependencies += "com.thesamet.scalapb" %% "protobuf-runtime-scala" % "0.8.16",
    compatibilitySmoke := Def.uncached {
      implicit val converter: xsbti.FileConverter = fileConverter.value
      val oldClasspath = (Compile / fullClasspath).value.files.map(_.toFile)
      val currentJar =
        converter.toPath((runtimeJVM / Compile / packageBin).value).toFile
      val currentClasspath =
        (runtimeJVM / Compile / dependencyClasspath).value.files
          .map(_.toFile) :+ currentJar
      require(
        oldClasspath
          .count(_.getName.startsWith("protobuf-runtime-scala_")) == 1,
        "Expected exactly one previous-release runtime"
      )
      val classpath = oldClasspath.filterNot(
        _.getName.startsWith("protobuf-runtime-scala_")
      ) ++ currentClasspath
      val home = sys.env
        .get("JAVA8_HOME")
        .map(file)
        .getOrElse(file(sys.props("java.home")))
      val result = sys.process
        .Process(
          Seq(
            (home / "bin" / "java").getAbsolutePath,
            "-cp",
            classpath.distinct.mkString(java.io.File.pathSeparator),
            "compatibility.PreviousReleaseConsumer"
          )
        )
        .!
      if (result != 0)
        sys.error("Binary compatibility / Java runtime smoke test failed")
    }
  )

lazy val integration = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .nativeSettings(nativeTestSettings)
  .in(file("integration"))
  .dependsOn(protobufRuntimeScala)
  .settings(
    publish / skip := true,
    libraryDependencies ++= Seq(
      ("com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % Test)
        .excludeAll(
          ExclusionRule(organization = "com.google.protobuf"),
          ExclusionRule(
            organization = "com.thesamet.scalapb",
            name = "protobuf-runtime-scala*"
          )
        ),
      "com.lihaoyi" %% "utest" % "0.9.5" % Test
    ),
    Test / fullClasspath := {
      val classpath = (Test / fullClasspath).value
      require(
        !(Test / externalDependencyClasspath).value.exists { entry =>
          val name = entry.data.name
          name.startsWith("protobuf-java-") || name
            .startsWith("protobuf-runtime-scala_")
        },
        "Integration tests must use only this checkout's wire runtime"
      )
      classpath
    },
    Test / PB.protoSources := Seq(
      (LocalRootProject / baseDirectory).value / "integration" / "shared" / "src" / "test" / "protobuf"
    ),
    Test / PB.targets := Seq(
      scalapb.gen() -> (Test / sourceManaged).value / "scalapb"
    ),
    // Recompile the same consumer against the new API as a source-compatibility check.
    Test / unmanagedSourceDirectories += (LocalRootProject / baseDirectory).value / "tools" / "binary-consumer" / "src" / "main" / "scala"
  )
lazy val integrationJVM = integration.jvm
lazy val integrationJS = integration.js
lazy val integrationNative = integration.native

runtimeJVM / mimaPreviousArtifacts := Set(
  organization.value %% "protobuf-runtime-scala" % "0.8.16"
)

// uTest uses an older, binary-compatible Native 0.5 test interface.
lazy val nativeTestSettings = Seq(
  libraryDependencySchemes += "org.scala-native" % s"test-interface_native0.5_${scalaBinaryVersion.value}" % "early-semver"
)
