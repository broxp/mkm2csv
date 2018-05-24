name := "mkm2csv"

version := "1.0"

// lazy val `mkm2csv` = (project in file(".")).enablePlugins(PlayScala)

dependencyOverrides += "org.scala-sbt" % "sbt" % "0.13.1"

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

scalaVersion := "2.12.2"

// libraryDependencies ++= Seq(jdbc, ehcache, ws, specs2 % Test, guice)

// val stage = taskKey[Unit]("Stage task")

// val Stage = config("stage")


unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")