val Http4sVersion = "0.20.0-M6"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core"           % "1.6.0",
  "org.typelevel" %% "cats-effect"         % "1.2.0",
  "org.http4s"    %% "http4s-blaze-server" % Http4sVersion,
  "org.http4s"    %% "http4s-blaze-client" % Http4sVersion,
  "org.http4s"    %% "http4s-circe"        % Http4sVersion,
  "org.http4s"    %% "http4s-dsl"          % Http4sVersion,
  "eu.timepit"    %% "refined"             % "0.9.4",
  "eu.timepit"    %% "refined-cats"        % "0.9.4",
  "org.typelevel" %% "spire"               % "0.14.1",
  "ch.qos.logback" % "logback-classic"     % "1.2.3" % Runtime,
  "org.scalatest" %% "scalatest"           % "3.0.4" % Test
)

scalacOptions += "-Ypartial-unification"
