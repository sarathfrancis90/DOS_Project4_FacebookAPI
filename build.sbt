name := "dos-project-4"

version := "1.0"

scalaVersion := "2.11.7"

resolvers += "Spray repository" at "http://repo.spray.io"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.0"

libraryDependencies += "io.spray" %% "spray-can" % "1.3.3"

libraryDependencies += "io.spray" %% "spray-json" % "1.3.2"

libraryDependencies += "io.spray" %% "spray-client" % "1.3.3"
