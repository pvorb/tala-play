name := "tala"

version := "0.0.1"

scalaVersion := "2.11.5"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
    cache,
    jdbc,
    "org.xerial" % "sqlite-jdbc" % "3.8.7",
    "com.googlecode.owasp-java-html-sanitizer" % "owasp-java-html-sanitizer" % "r239",
    "org.owasp.encoder" % "encoder" % "1.1.1"
)
