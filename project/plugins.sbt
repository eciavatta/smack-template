logLevel := Level.Info

// Examines your Scala code and indicates potential problems with it
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

// Deploy fat JARs. Restart processes.
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.7")

// Builds and pushes Docker images of the project.
addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.5.0")

// Generates Scala source from your build definitions
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")

// Visualize project's dependencies
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.1")

// Integrates the scoverage code coverage library
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

// Supports running applications and ScalaTest tests in multiple JVMs at the same time
addSbtPlugin("com.typesafe.sbt" % "sbt-multi-jvm" % "0.4.0")
