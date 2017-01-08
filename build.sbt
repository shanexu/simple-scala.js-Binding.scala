lazy val commonSettings = Seq(
  version := "0.0.1",
  scalaVersion := "2.11.8",
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
  scalacOptions ++= Seq("-feature", "-language:implicitConversions")
)

lazy val AkkaVersion = "2.4.16"

lazy val AkkaHttpVersion = "10.0.1"

lazy val BindingScalaVersion = "10.0.1"

lazy val CirceVersion = "0.6.1"

lazy val ScalazVersion = "7.2.8"

lazy val compileCopyTask = taskKey[Unit]("compile and copy")


lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  aggregate(server, js)

lazy val server = (project in file("server")).
  settings(commonSettings: _*).
  settings(
    cancelable in Global := true,
    fork in run := true,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
      "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
      "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
      "ch.qos.logback" %  "logback-classic" % "1.1.7",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
    )
  ).
  settings(
    compileCopyTask := {
      val mainVersion = scalaVersion.value.split("""\.""").take(2).mkString(".")
      val to = target.value / ("scala-" + mainVersion) / "classes" / "static" / "js"
      to.mkdirs()
      val fastJs = (fastOptJS in Compile in js).value.data
      val fastJsSourceMap = fastJs.getParentFile / (fastJs.getName + ".map")
      val fastJsLauncher = (packageScalaJSLauncher in Compile in js).value.data
      val fastJsDeps = (packageJSDependencies in Compile in js).value
      val fullJs = (fullOptJS in Compile in js).value.data
      val fullJsSourceMap = fullJs.getParentFile / (fullJs.getName + ".map")
      val fullJsDeps = (packageMinifiedJSDependencies in Compile in js).value

      for(f <- Seq(fastJs, fastJsSourceMap, fastJsLauncher, fastJsDeps, fullJs, fullJsSourceMap, fullJsDeps)) {
        IO.copyFile(f, to / f.getName)
      }
    }
  ).
  settings(
    compile in Compile := {
      compileCopyTask.value
      (compile in Compile).value
    }
  ).
  settings(
    mainClass in assembly := Some("Server"),
    assemblyJarName in assembly := "server.jar"
  )

lazy val js = (project in file("js")).
  settings(commonSettings: _*).
  settings(
    mainClass in (Compile) := Some("Application"),
    persistLauncher := true,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.1",
      "com.thoughtworks.binding" %%% "dom" % BindingScalaVersion,
      "com.thoughtworks.binding" %%% "route" % BindingScalaVersion,
      "io.circe" %%% "circe-core" % CirceVersion,
      "io.circe" %%% "circe-parser" % CirceVersion,
      "io.circe" %%% "circe-generic" % CirceVersion,
      "org.scalaz" %%% "scalaz-core" % ScalazVersion
    )
  ).
  enablePlugins(ScalaJSPlugin)
