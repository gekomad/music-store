enablePlugins(JavaServerAppPackaging)

name := "music-store"

version := "1.0.0"

organization := "com.github.gekomad"
scalaVersion := "2.12.8"

scalacOptions := Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-Ypartial-unification"
)


val http4sVersion = "0.19.0"
val circeVersion = "0.10.0"
val slickVersion = "3.2.3"
val kafkaVersion = "1.0.0"
val avroVersion = "1.9.0"

val h2Version = "1.4.197"
val logbackVersion = "1.2.3"
val scalaTestVersion = "3.0.5"
val scalaCheckVersion = "1.14.0"
val scalaMeterVersion = "0.10.1"
val postgresVersion = "42.2.5"
val mysqlVersion = "8.0.14"

//http4s
libraryDependencies += "org.http4s" %% "http4s-blaze-server" % http4sVersion
libraryDependencies += "org.http4s" %% "http4s-dsl" % http4sVersion
libraryDependencies += "org.http4s" %% "http4s-blaze-client" % http4sVersion

//circe
libraryDependencies += "io.circe" %% "circe-java8" % circeVersion
libraryDependencies += "org.http4s" %% "http4s-circe" % http4sVersion
libraryDependencies += "io.circe" %% "circe-optics" % circeVersion

// Optional for auto-derivation of JSON codecs
libraryDependencies += "io.circe" %% "circe-generic" % circeVersion
// Optional for string interpolation to JSON model
libraryDependencies += "io.circe" %% "circe-literal" % circeVersion
libraryDependencies += "io.circe" %% "circe-parser" % circeVersion
libraryDependencies += "io.circe" %% "circe-core" % circeVersion
//slick
libraryDependencies += "com.typesafe.slick" %% "slick" % slickVersion
libraryDependencies += "com.typesafe.slick" %% "slick-hikaricp" % slickVersion

//kafka
libraryDependencies += "org.apache.kafka" %% "kafka" % kafkaVersion exclude("org.slf4j", "slf4j-log4j12")
libraryDependencies += "net.cakesolutions" %% "scala-kafka-client" % kafkaVersion
resolvers += Resolver.bintrayRepo("cakesolutions", "maven")

//avro
libraryDependencies += "com.sksamuel.avro4s" %% "avro4s-core" % avroVersion

//driver
libraryDependencies += "com.h2database" % "h2" % h2Version
libraryDependencies += "mysql" % "mysql-connector-java" % mysqlVersion
libraryDependencies += "org.postgresql" % "postgresql" % postgresVersion

//log
libraryDependencies += "ch.qos.logback" % "logback-classic" % logbackVersion

//test
libraryDependencies += "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
libraryDependencies += "org.scalacheck" %% "scalacheck" % scalaCheckVersion % "test"
libraryDependencies += "com.storm-enroute" %% "scalameter" % scalaMeterVersion % "test"

libraryDependencies += "net.cakesolutions" %% "scala-kafka-client-testkit" % kafkaVersion % "test"

testOptions += Tests.Setup (_ => sys.props ("testing") = "application_IT.conf")
parallelExecution in Test := false

