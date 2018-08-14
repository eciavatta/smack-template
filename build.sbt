import sbt.file
import sbtassembly.{AssemblyKeys, MergeStrategy}

val projectName = "smack-template"
val projectVersion = "0.2.0-SNAPSHOT"
val projectOrganization = "it.eciavatta"

val akkaVersion = "2.5.14"
val akkaHttpVersion = "10.1.3"

// Managed dependencies
lazy val dependencies = Seq(
  // project direct dependencies
  "com.datastax.cassandra" % "cassandra-driver-core" % "3.5.1" exclude("com.google.guava", "guava"),
  "com.github.scopt" %% "scopt" % "3.7.0",
  "com.sclasen" %% "akka-zk-cluster-seed" % "0.1.10" exclude("com.google.guava", "guava") exclude("com.typesafe", "ssl-config-core_2.12")
    exclude("com.typesafe.akka", "akka-actor_2.12") exclude("com.typesafe.akka", "akka-cluster_2.12") exclude("com.typesafe.akka", "akka-protobuf_2.12")
    exclude("com.typesafe.akka", "akka-remote_2.12") exclude("com.typesafe.akka", "akka-stream_2.12") exclude("io.netty", "netty")
    exclude("org.slf4j", "slf4j-api"),
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion exclude("com.typesafe", "config"),
  "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion exclude("com.typesafe", "config"),
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion exclude("com.typesafe", "config"),
  "com.typesafe.akka" %% "akka-stream" % akkaVersion exclude("com.typesafe", "config"),
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.22" exclude("com.typesafe", "config") exclude("com.typesafe.akka", "akka-actor_2.12")
    exclude("com.typesafe.akka", "akka-protobuf_2.12") exclude("com.typesafe.akka", "akka-stream_2.12") exclude("org.apache.kafka", "kafka-clients"),
  "io.kamon" %% "kamon-akka-remote-2.5" % "1.1.0" exclude("com.lihaoyi", "sourcecode_2.12") exclude("com.typesafe", "config")
    exclude("com.typesafe.akka", "akka-actor_2.12") exclude("com.typesafe.akka", "akka-cluster_2.12") exclude("com.typesafe.akka", "akka-protobuf_2.12")
    exclude("com.typesafe.akka", "akka-remote_2.12") exclude("com.typesafe.akka", "akka-stream_2.12"),
  "io.kamon" %% "kamon-datadog" % "1.0.0" exclude("com.typesafe", "config") exclude("io.kamon", "kamon-core_2.12"),
  "io.kamon" %% "kamon-executors" % "1.0.1" exclude("com.typesafe", "config") exclude("com.lihaoyi", "sourcecode_2.12"),
  "io.sentry" % "sentry" % "1.7.5" exclude("org.slf4j", "slf4j-api") exclude("com.fasterxml.jackson.core", "jackson-core"),

  // project test dependencies
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test exclude("com.typesafe", "config"),
  "net.manub" %% "scalatest-embedded-kafka" % "1.1.1" % Test exclude("org.slf4j", "slf4j-api") exclude("org.xerial.snappy", "snappy-java")
    exclude("org.apache.zookeeper", "zookeeper") exclude("org.codehaus.jackson", "jackson-core-asl") exclude("org.codehaus.jackson", "jackson-mapper-asl")
    exclude("org.lz4", "lz4-java"),
  "org.cassandraunit" % "cassandra-unit" % "3.5.0.1" % Test exclude("ch.qos.logback", "logback-classic") exclude("ch.qos.logback", "logback-core")
    exclude("com.google.guava", "guava") exclude("io.dropwizard.metrics", "metrics-core") exclude("org.xerial.snappy", "snappy-java")
    exclude("io.netty", "netty-buffer") exclude("io.netty", "netty-codec") exclude("io.netty", "netty-common") exclude("io.netty", "netty-handler")
    exclude("io.netty", "netty-transport") exclude("org.ow2.asm", "asm") exclude("org.slf4j", "slf4j-api"),
  "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,

  // project compile dependencies
  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",

  // project transitive dependencies
  "com.fasterxml.jackson.core" % "jackson-core" % "2.9.6",
  "com.google.guava" % "guava" % "21.0",
  "org.apache.kafka" % "kafka-clients" % "1.1.1"
)

// add scalastyle to compile task
lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")
coverageEnabled := true

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
    parallelExecution in Test := true,
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

evictionWarningOptions in update := EvictionWarningOptions.default.withWarnTransitiveEvictions(false)
  .withWarnDirectEvictions(false).withWarnScalaVersionEviction(false)

lazy val buildInfoSettings = Seq(
  buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
  buildInfoPackage := "smack"
)

lazy val assemblySettings = Seq(
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
    case PathList("META-INF", "aop.xml") => aopMerge
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
      copy(file("agents/aspectjweaver-1.9.1.jar"), "/app/aspectjweaver.jar")
      copy(artifact, artifactTargetPath)
      entryPoint("java", "-javaagent:/app/aspectjweaver.jar", "-cp", artifactTargetPath, "smack.entrypoints.Main")
    }
  }
)

// Create a new MergeStrategy for aop.xml files
val aopMerge: MergeStrategy = new MergeStrategy {
  val name = "aopMerge"
  import scala.xml._
  import scala.xml.dtd._

  def apply(tempDir: File, path: String, files: Seq[File]): Either[String, Seq[(File, String)]] = {
    val dt = DocType("aspectj", PublicID("-//AspectJ//DTD//EN", "http://www.eclipse.org/aspectj/dtd/aspectj.dtd"), Nil)
    val file = MergeStrategy.createMergeTarget(tempDir, path)
    val xmls: Seq[Elem] = files.map(XML.loadFile)
    val aspectsChildren: Seq[Node] = xmls.flatMap(_ \\ "aspectj" \ "aspects" \ "_")
    val weaverChildren: Seq[Node] = xmls.flatMap(_ \\ "aspectj" \ "weaver" \ "_")
    val options: String = xmls.map(x => (x \\ "aspectj" \ "weaver" \ "@options").text).mkString(" ").trim
    val weaverAttr = if (options.isEmpty) Null else new UnprefixedAttribute("options", options, Null)
    val aspects = new Elem(null, "aspects", Null, TopScope, false, aspectsChildren: _*)
    val weaver = new Elem(null, "weaver", weaverAttr, TopScope, false, weaverChildren: _*)
    val aspectj = new Elem(null, "aspectj", Null, TopScope, false, aspects, weaver)
    XML.save(file.toString, aspectj, "UTF-8", xmlDecl = false, dt)
    IO.append(file, IO.Newline.getBytes(IO.defaultCharset))
    Right(Seq(file -> path))
  }
}
