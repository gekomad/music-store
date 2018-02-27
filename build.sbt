import sbt.Keys.libraryDependencies

//sbt-native-packager
enablePlugins(JavaServerAppPackaging)

name := "music-store"

version := "0.0.4-SNAPSHOT"
organization := "com.github.gekomad"
scalaVersion := "2.12.4"

val Http4sVersion = "0.17.6"
val circeVersion = "0.8.0"
val slickVersion = "3.2.1"
val kafkaVersion = "1.0.0"
val dockerTestkitVersion =  "0.9.5"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature")

//http4s
libraryDependencies += "org.http4s" %% "http4s-blaze-server" % Http4sVersion
libraryDependencies += "org.http4s" %% "http4s-dsl" % Http4sVersion
libraryDependencies += "org.http4s" %% "http4s-blaze-client" % Http4sVersion

//circe
libraryDependencies += "io.circe" %% "circe-java8" % circeVersion
libraryDependencies += "org.http4s" %% "http4s-circe" % Http4sVersion
libraryDependencies += "io.circe" %% "circe-optics" % circeVersion

// Optional for auto-derivation of JSON codecs
libraryDependencies += "io.circe" %% "circe-generic" % circeVersion
// Optional for string interpolation to JSON model
libraryDependencies += "io.circe" %% "circe-literal" % circeVersion
libraryDependencies += "io.circe" %% "circe-parser" % circeVersion

//slick
libraryDependencies += "com.typesafe.slick" %% "slick" % slickVersion
libraryDependencies += "com.typesafe.slick" %% "slick-hikaricp" % slickVersion

//kafka
libraryDependencies += "org.apache.kafka" %% "kafka" % kafkaVersion exclude("org.slf4j", "slf4j-log4j12")
libraryDependencies += "net.cakesolutions" %% "scala-kafka-client" % kafkaVersion
resolvers += Resolver.bintrayRepo("cakesolutions", "maven")

//avro
libraryDependencies += "com.sksamuel.avro4s" %% "avro4s-core" % "1.8.0"

//driver
libraryDependencies += "com.h2database" % "h2" % "1.4.196"
libraryDependencies += "mysql" % "mysql-connector-java" % "6.0.6"
libraryDependencies += "org.postgresql" % "postgresql" % "42.2.1"

//log
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

//test
//libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % "test"
libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.13.5" % "test"
libraryDependencies += "com.storm-enroute" %% "scalameter" % "0.8.2" % "test"
libraryDependencies += "net.cakesolutions" %% "scala-kafka-client-testkit" % kafkaVersion % "test"

libraryDependencies += "com.whisk" %% "docker-testkit-scalatest" % "0.9.5" % IntegrationTest
libraryDependencies += "com.whisk" %% "docker-testkit-impl-spotify" % "0.9.5" % IntegrationTest


testOptions += Tests.Setup(_ => sys.props("testing") = "application_INTEGRATION_TEST.conf")
parallelExecution in Test := false


