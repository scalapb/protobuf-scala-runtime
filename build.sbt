import ReleaseTransformations._

scalaVersion in ThisBuild := "2.11.7"

crossScalaVersions in ThisBuild := Seq("2.10.6", "2.11.7", "2.12.0-M2")

organization in ThisBuild := "com.trueaccord.scalapb"

scalacOptions in ThisBuild ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, v)) if v <= 11 => List("-target:jvm-1.7")
    case _ => Nil
  }
}

releaseCrossBuild := true

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
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

lazy val protobufRuntimeScala = crossProject.crossType(CrossType.Pure).in(file("."))
  .settings(
    name := "protobuf-runtime-scala"
  )
  .jvmSettings(
    // Add JVM-specific settings here
  )
  .jsSettings(
    // Add JS-specific settings here
  )
  
lazy val runtimeJS = protobufRuntimeScala.js
lazy val runtimeJVM = protobufRuntimeScala.jvm
