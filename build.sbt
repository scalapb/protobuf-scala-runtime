import SonatypeKeys._

scalaVersion in ThisBuild := "2.11.7"

crossScalaVersions in ThisBuild := Seq("2.10.6", "2.11.7", "2.12.0-M2")

organization in ThisBuild := "com.trueaccord.scalapb"

profileName in ThisBuild := "com.trueaccord"

scalacOptions in ThisBuild ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, v)) if v <= 11 => List("-target:jvm-1.7")
    case _ => Nil
  }
}

sonatypeSettings

lazy val projectReleaseSettings = sonatypeSettings ++ Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value
)

lazy val root = project.in(file(".")).
  aggregate(runtimeJS, runtimeJVM).
  settings(
    publish := {},
    publishLocal := {},
    projectReleaseSettings
  )

lazy val protobufRuntimeScala = crossProject.crossType(CrossType.Pure).in(file(".")).
  settings(
    name := "protobuf-runtime-scala"
  ).
  jvmSettings(
    // Add JVM-specific settings here
  ).
  jsSettings(
    // Add JS-specific settings here
  )
  
lazy val runtimeJS = protobufRuntimeScala.js
lazy val runtimeJVM = protobufRuntimeScala.jvm

pomExtra in ThisBuild := {
  <url>https://github.com/trueaccord/ScalaPB</url>
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
    </license>
  </licenses>
  <scm>
    <connection>scm:git:github.com:trueaccord/ScalaPB.git</connection>
    <developerConnection>scm:git:git@github.com:trueaccord/protobuf-runtime-scala.git</developerConnection>
    <url>github.com/trueaccord/ScalaPB</url>
  </scm>
  <developers>
    <developer>
      <id>thesamet</id>
      <name>Nadav S. Samet</name>
      <url>http://www.thesamet.com/</url>
    </developer>
  </developers>
}

