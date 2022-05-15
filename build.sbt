ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.2"

Global / resolvers ++= Seq(
  "Osgeo" at "https://repo.osgeo.org/repository/release/"
)

Global / run / fork := true

lazy val gisImporter = (project in file("module/gis-importer"))
  .settings(
    scalaVersion := "3.1.2",
    name := "gis-importer",
    libraryDependencies ++= Seq(
      "org.locationtech.proj4j" % "proj4j" % "1.1.5",
      "dev.zio" %% "zio-interop-cats" % "3.3.0-RC6",
      "dev.zio" %% "zio-logging" % "2.0.0-RC8",
      "dev.zio" %% "zio-logging-slf4j" % "2.0.0-RC8",
      "dev.zio" %% "zio-nio" % "2.0.0-RC6" exclude ("org.scala-lang.modules", "scala-collection-compat_2.13"),
      "dev.zio" %% "zio-config" % "3.0.0-RC8",
      "dev.zio" %% "zio-config-magnolia" % "3.0.0-RC8",
      "dev.zio" %% "zio-config-typesafe" % "3.0.0-RC8",
      "org.http4s" %% "http4s-client" % "1.0.0-M32",
      "org.http4s" %% "http4s-blaze-client" % "1.0.0-M32",
      "org.http4s" %% "http4s-circe" % "1.0.0-M32",
      "io.circe" %% "circe-core" % "0.14.1",
      "io.circe" %% "circe-generic" % "0.14.1",
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.17.2",
      "org.openpnp" % "opencv" % "4.5.1-2",
      "io.github.vigoo" %% "clipp-zio-2" % "0.6.6",
      "org.apache.logging.log4j" % "log4j-api" % "2.17.2",
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.17.2",
      "org.apache.logging.log4j" % "log4j-core" % "2.17.2"
    ),
    dependencyOverrides += "dev.zio" %% "zio" % "2.0.0-RC5",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-test" % "2.0.0-RC5" % "test",
      "dev.zio" %% "zio-test-sbt" % "2.0.0-RC5" % "test",
      "dev.zio" %% "zio-mock" % "1.0.0-RC5" % "test"
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
