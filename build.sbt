import sbtassembly.AssemblyKeys

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
  "com.lightbend.akka" %% "akka-stream-alpakka-cassandra" % "0.20" exclude("com.google.guava", "guava"),
"org.scalacheck" %% "scalacheck" % "1.14.0",
  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "io.sentry" % "sentry" % "1.7.5" exclude("org.slf4j", "slf4j-api"))

// add scalastyle to compile task
lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")

lazy val root = Project(
  id = projectName,
  base = file(".")
).enablePlugins(BuildInfoPlugin)
  .enablePlugins(AssemblyPlugin)
  .enablePlugins(DockerPlugin)
  .settings(buildInfoSettings: _*)
  .settings(assemblySettings: _*)
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
    test in assembly := {},

    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value / "protobuf"
    ),
    PB.includePaths in Compile += file("model/src/main/protobuf"),

    mainClass in assembly := Some("smack.entrypoints.Main"),
    assemblyJarName in assembly := s"${name.value}-${version.value}.jar"
  )

lazy val buildInfoSettings = Seq(
  buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
  buildInfoPackage := "smack"
)

lazy val assemblySettings = Seq(
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
    case oldStrategy => (assemblyMergeStrategy in assembly).value(oldStrategy)
  }
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
