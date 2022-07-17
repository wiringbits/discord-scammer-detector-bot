name := """discord-scammer-detector-bot"""
organization := "net.wiringbits"

version := "1.0-SNAPSHOT"

scalaVersion := "2.13.8"

(Test / fork) := true

(assembly / assemblyMergeStrategy) := {
  case x if Assembly.isConfigFile(x) =>
    MergeStrategy.concat
  case PathList(ps @ _*) if Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last) =>
    MergeStrategy.rename
  case PathList("META-INF", xs @ _*) =>
    (xs map { _.toLowerCase }) match {
      case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
        MergeStrategy.discard
      case ps @ (x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
        MergeStrategy.discard
      case "plexus" :: xs =>
        MergeStrategy.discard
      case "services" :: xs =>
        MergeStrategy.filterDistinctLines
      case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
        MergeStrategy.filterDistinctLines
      case _ => MergeStrategy.deduplicate
    }
  case "module-info.class" => MergeStrategy.last
  case _ => MergeStrategy.deduplicate
}

resolvers += Resolver.JCenterRepository
libraryDependencies += "net.katsstuff" %% "ackcord" % "0.18.1"
libraryDependencies += "org.apache.commons" % "commons-text" % "1.9"
libraryDependencies += "com.typesafe" % "config" % "1.4.2"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.36"
libraryDependencies += "ch.qos.logback" % "logback-core" % "1.2.11"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.11"

libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test