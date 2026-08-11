import scala.scalanative.build.*

enablePlugins(ScalaNativePlugin, BuildInfoPlugin)

ThisBuild / semanticdbEnabled := true
ThisBuild / scalafmtOnCompile := true

name         := "comport"
organization := "dev.capslock"
scalaVersion := "3.8.2"

// Release builds set COMPORT_VERSION from the git tag (see release.yml).
// Local builds report a placeholder so `--version` never claims a release.
// The container build always passes COMPORT_VERSION through, so an empty
// value has to be treated the same as an unset one.
version := sys.env
  .get("COMPORT_VERSION")
  .filter(_.nonEmpty)
  .getOrElse("0.0.0-SNAPSHOT")

scalacOptions ++= Seq("-no-indent", "-Wunused:all")

buildInfoPackage := "dev.capslock.comport"
buildInfoKeys    := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)

libraryDependencies ++= Seq(
  "com.lihaoyi"   %% "mainargs"   % "0.7.8",
  "com.lihaoyi"   %% "os-lib"     % "0.11.8",
  "org.virtuslab" %% "scala-yaml" % "0.3.3"
)

nativeConfig ~= { cfg =>
  val extraLinkOpts =
    sys.env.getOrElse("LINKFLAGS", "").split("\\s+").filter(_.nonEmpty).toSeq
  cfg
    .withMode(Mode.releaseFast)
    .withLTO(LTO.full)
    .withGC(GC.commix)
    .withLinkingOptions(cfg.linkingOptions ++ extraLinkOpts)
}
