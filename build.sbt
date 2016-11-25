name := "image-voter"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked", "-language:reflectiveCalls", "-language:postfixOps", "-language:implicitConversions")

doc in Compile <<= target.map(_ / "none")

scalariformSettings

libraryDependencies ++= Seq(
  ws,
  specs2 % Test,
	"org.specs2" %% "specs2-matcher-extra" % "3.8.5" % Test,
  "com.typesafe.akka" %% "akka-agent" % "2.4.14",
  "commons-io" % "commons-io" % "2.5"
)