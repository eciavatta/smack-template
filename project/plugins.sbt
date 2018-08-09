logLevel := Level.Warn

// Examines your Scala code and indicates potential problems with it
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

// Builds and pushes Docker images of the project.
addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.5.0")

// Creates distributable Scala packages that include dependent jars and launch scripts.
addSbtPlugin("org.xerial.sbt" % "sbt-pack" % "0.11")

// Generates Scala source from your build definitions
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")

// Visualize project's dependencies
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.1")
