ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.7"

lazy val root = (project in file("."))
  .settings(
    name := "todo-list"
  )

lazy val tapirVersion        = "0.19.1"
lazy val catsEffectVersion   = "3.3.0"
lazy val doobieVersion       = "1.0.0-RC1"
lazy val http4sVersion       = "0.23.6"
lazy val logBackVersion      = "1.2.3"
lazy val scalaLoggingVersion = "3.9.4"
lazy val flywayVersion       = "7.5.2"
lazy val pureconfigVersion   = "0.17.1"
lazy val tsecVersion         = "0.4.0"
lazy val chimneyVersion      = "0.6.1"
lazy val scalatestVersion    = "3.2.10"
lazy val scalamockVersion    = "5.1.0"

libraryDependencies ++= Seq(
  "org.http4s"                  %% "http4s-dsl"               % http4sVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-core"               % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe"         % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"      % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs"       % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle"  % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server"   % tapirVersion,
  "org.typelevel"               %% "cats-effect"              % catsEffectVersion,
  "org.tpolecat"                %% "doobie-core"              % doobieVersion,
  "org.tpolecat"                %% "doobie-postgres"          % doobieVersion,
  "org.tpolecat"                %% "doobie-h2"                % doobieVersion,
  "org.tpolecat"                %% "doobie-hikari"            % doobieVersion,
  "org.tpolecat"                %% "doobie-specs2"            % doobieVersion,
  "org.tpolecat"                %% "doobie-specs2"            % doobieVersion % "test",
  "org.tpolecat"                %% "doobie-scalatest"         % doobieVersion % "test",
  "ch.qos.logback"              % "logback-classic"           % logBackVersion,
  "com.typesafe.scala-logging"  %% "scala-logging"            % scalaLoggingVersion,
  "org.flywaydb"                % "flyway-core"               % flywayVersion,
  "com.github.pureconfig"       %% "pureconfig"               % pureconfigVersion,
  "com.github.pureconfig"       %% "pureconfig-cats-effect"   % pureconfigVersion,
  "io.github.jmcardon"          %% "tsec-common"              % tsecVersion,
  "io.github.jmcardon"          %% "tsec-password"            % tsecVersion,
  "io.scalaland"                %% "chimney"                  % chimneyVersion,
  "org.scalactic"               %% "scalactic"                % scalatestVersion,
  "org.scalatest"               %% "scalatest"                % scalatestVersion % "test",
  "org.scalamock"               %% "scalamock"                % scalamockVersion % "test"
)
