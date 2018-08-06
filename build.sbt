name := "smack-template"
version := "0.1.0-SNAPSHOT"

// Constants
val akkaVersion = "2.5.14"
val akkaHttpVersion = "10.1.3"

// Managed dependencies
lazy val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
lazy val akkaCluster = "com.typesafe.akka" %% "akka-cluster" % akkaVersion
lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
lazy val akkaRemote = "com.typesafe.akka" %% "akka-remote" % akkaVersion
lazy val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
lazy val akkaZookeeper = "com.sclasen" %% "akka-zk-cluster-seed" % "0.1.10"
lazy val alpakka = "com.typesafe.akka" %% "akka-stream-kafka" % "0.22"
lazy val scalacheck = "org.scalacheck" %% "scalacheck" % "1.14.0"
lazy val scalaPB = "com.trueaccord.scalapb" %% "scalapb-runtime" % "0.6.7" % "protobuf"
lazy val scalatest = "org.scalatest" %% "scalatest" % "3.0.0" % "test"
lazy val sprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion
lazy val sentry = "io.sentry" % "sentry" % "1.7.5"

// add scalastyle to compile task
lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")
lazy val commonSettings = Seq(
  (compile in Compile) := ((compile in Compile) dependsOn compileScalastyle).value,
  compileScalastyle := scalastyle.in(Compile).toTask("").value,
  conflictManager in ThisBuild := ConflictManager.all,
  libraryDependencies ++= Seq(akkaActor, akkaRemote, akkaCluster, scalatest),
  organization := "it.eciavatta",
  parallelExecution in Test := false,
  scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint"),
  scalaVersion := "2.12.6",
  updateOptions := updateOptions.value.withCachedResolution(true),
  version := (version in ThisBuild).value,

  PB.targets in Compile := Seq(
    scalapb.gen() -> (sourceManaged in Compile).value
  )
)

lazy val root = Project(
  id = "smack-template",
  base = file(".")
).enablePlugins(PackPlugin)
  .enablePlugins(DockerPlugin)
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(frontend, cluster, model)
  .settings(commonSettings: _*)
  .settings(packSettings: _*)
  .settings(dockerSettings: _*)
  .settings(buildInfoSettings: _*)
  .settings(
    libraryDependencies ++= Seq(scalatest, akkaZookeeper, sentry)
  )

lazy val frontend = module("frontend")
  .dependsOn(model)
  .settings(
    libraryDependencies ++= Seq(akkaHttp, akkaStream, sprayJson)
  )

lazy val model = module("model")

lazy val cluster = module("cluster")
  .dependsOn(model)

lazy val packSettings = Seq(
  packMain := Map("smack-template" -> "smack.Main")
)

lazy val dockerSettings = Seq(
  docker := (docker dependsOn pack).value,
  imageNames in docker := Seq(ImageName(s"${name.value}:latest")),
  dockerfile in docker := {
    val outputPath = s"/app/${name.value}-${version.value}/"
    new sbtdocker.mutable.Dockerfile {
      from("java8:latest")
      copy(file("target/pack/"), outputPath)
      entryPoint(s"${outputPath}bin/smack-template")
    }
  }
)

lazy val buildInfoSettings = Seq(
  buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
  buildInfoPackage := "smack"
)

def module(name: String): Project =
  Project(id = name, base = file(name))
    .settings(commonSettings)

libraryDependencies ++= Seq(
  "io.netty" % "netty" % "3.7.0.Final",
  "org.slf4j" % "slf4j-api" % "1.6.1",
  "com.typesafe" % "config" % "1.2.0"
)
