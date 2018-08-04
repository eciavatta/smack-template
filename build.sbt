import sbt.Keys.libraryDependencies
import sbtassembly.AssemblyKeys

// Constants
val akkaVersion = "2.5.14"
val akkaHttpVersion = "10.1.3"

// Managed dependencies
lazy val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
lazy val akkaCluster = "com.typesafe.akka" %% "akka-cluster" % akkaVersion
lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
lazy val akkaRemote = "com.typesafe.akka" %% "akka-remote" % akkaVersion
lazy val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
lazy val alpakka = "com.typesafe.akka" %% "akka-stream-kafka" % "0.22"
lazy val scalacheck = "org.scalacheck" %% "scalacheck" % "1.14.0"
lazy val scalatest = "org.scalatest" %% "scalatest" % "3.0.0" % "test"
lazy val sprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion
lazy val akkaZookeeper = "com.sclasen" %% "akka-zk-cluster-seed" % "0.1.10"
lazy val scalaPB = "com.trueaccord.scalapb" %% "scalapb-runtime" % "0.6.7" % "protobuf"



// add scalastyle to compile task
lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")
lazy val commonSettings = Seq(
  organization := "it.eciavatta",
  scalaVersion := "2.12.6",
  version := (version in ThisBuild).value,
  compileScalastyle := scalastyle.in(Compile).toTask("").value,
  (compile in Compile) := ((compile in Compile) dependsOn compileScalastyle).value,
  scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint"),
  test in assembly := {},
  conflictManager in ThisBuild := ConflictManager.all,
  updateOptions := updateOptions.value.withCachedResolution(true),
  libraryDependencies ++= Seq(akkaActor, akkaRemote, akkaCluster, scalaPB),

  PB.targets in Compile := Seq(
    scalapb.gen() -> (sourceManaged in Compile).value
  )
)

lazy val root = Project(
  id = "smack-template",
  base = file(".")
).enablePlugins(AssemblyPlugin)
  .enablePlugins(DockerPlugin)
  .dependsOn(frontend, cluster, model)
  .settings(commonSettings: _*)
  .settings(dockerSettings: _*)
  .settings(
    libraryDependencies ++= Seq(scalatest, akkaZookeeper),
    // mainClass in assembly := Some("smack.RunFrontend"),
    assemblyJarName in assembly := s"${name.value}-${version.value}.jar"
  )

// (scalastyleConfig in Compile) := baseDirectory.value /  "project/scalastyle-config.xml"
lazy val frontend = module("frontend")
  .dependsOn(model)
  .settings(
    libraryDependencies ++= Seq(akkaHttp, akkaStream, sprayJson, scalatest)
  )

lazy val model = module("model")
  .settings(
    libraryDependencies ++= Seq(scalatest)
  )

lazy val cluster = module("cluster")
  .dependsOn(model)
  .settings(
    libraryDependencies ++= Seq(scalatest)
  )

lazy val dockerSettings = Seq(
  docker := (docker dependsOn assembly).value,
  imageNames in docker := Seq(ImageName(s"${name.value}:latest")),
  dockerfile in docker := {
    val artifact = (AssemblyKeys.assemblyOutputPath in assembly).value
    val artifactTargetPath = s"/app/${artifact.name}"
    new sbtdocker.mutable.Dockerfile {
      from("java8:latest")
      copy(artifact, artifactTargetPath)
      entryPoint("java", "-cp", artifactTargetPath)
    }
  }
)

// add scalastyle to test task
// lazy val testScalastyle = taskKey[Unit]("testScalastyle")
// testScalastyle := scalastyle.in(Test).toTask("").value
// (test in Test) := ((test in Test) dependsOn testScalastyle).value
// (scalastyleConfig in Test) := baseDirectory.value /  "project/scalastyle-test-config.xml"

def module(name: String): Project =
  Project(id = name, base = file(name))
    .settings(commonSettings)

libraryDependencies ++= Seq(
  "io.netty" % "netty" % "3.7.0.Final"
)

parallelExecution in Test := false
