val projectName = "smack-template"
val projectVersion = "0.2.0-SNAPSHOT"
val projectOrganization = "it.eciavatta"

val akkaVersion = "2.5.13"
val akkaHttpVersion = "10.1.3"

// Managed dependencies
lazy val dependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.sclasen" %% "akka-zk-cluster-seed" % "0.1.10" exclude("com.typesafe", "ssl-config-core_2.12")
    exclude("com.typesafe.akka", "akka-actor_2.12") exclude("com.typesafe.akka", "akka-cluster_2.12")
    exclude("com.typesafe.akka", "akka-protobuf_2.12") exclude("com.typesafe.akka", "akka-remote_2.12")
    exclude("com.typesafe.akka", "akka-stream_2.12") exclude("io.netty", "netty") exclude("org.slf4j", "slf4j-api"),
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.22",
  "org.scalacheck" %% "scalacheck" % "1.14.0",
  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "io.sentry" % "sentry" % "1.7.5" exclude("org.slf4j", "slf4j-api"))

// add scalastyle to compile task
lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")

enablePlugins(BuildInfoPlugin)

lazy val root = Project(
  id = projectName,
  base = file(".")
).enablePlugins(BuildInfoPlugin)
  .enablePlugins(PackPlugin)
  .enablePlugins(DockerPlugin)
  .settings(buildInfoSettings: _*)
  .settings(packSettings: _*)
  .settings(dockerSettings: _*)
  .settings(
    name := projectName,
    version := projectVersion,
    organization := projectOrganization,

    (compile in Compile) := ((compile in Compile) dependsOn compileScalastyle).value,
    compileScalastyle := scalastyle.in(Compile).toTask("").value,
    conflictManager in ThisBuild := ConflictManager.all,
    parallelExecution in Test := false,
    scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint"),
    updateOptions := updateOptions.value.withCachedResolution(true),
    libraryDependencies ++= dependencies,
    scalaVersion := "2.12.6",

    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value / "protobuf"
    ),
    PB.includePaths in Compile += file("model/src/main/protobuf"),

    unmanagedSourceDirectories in Compile += baseDirectory.value / "project"
  )

lazy val buildInfoSettings = Seq(
  buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
  buildInfoPackage := "smack"
)

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
