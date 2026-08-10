import scala.scalanative.build.*

enablePlugins(ScalaNativePlugin)

ThisBuild / semanticdbEnabled := true
ThisBuild / scalafmtOnCompile := true

name         := "comport"
organization := "dev.capslock"
scalaVersion := "3.8.2"
scalacOptions ++= Seq("-no-indent", "-Wunused:all")

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
