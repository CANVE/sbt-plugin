organization := "canve"

name := "sbt-plugin"

sbtPlugin := true

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

libraryDependencies ++= Seq("canve" %% "compiler-plugin" % "0.0.1")

resolvers ++= {
  if(isSnapshot.value) Seq(Resolver.mavenLocal, Resolver.sonatypeRepo("snapshots"))
  else Seq.empty
}

publishMavenStyle := false
publishArtifact in Test := false

licenses +=("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

ScriptedPlugin.scriptedSettings

scriptedLaunchOpts ++= Seq(
  "-Xmx1024M", "-XX:MaxPermSize=256M",
  "-Dplugin.version=" + version.value
)

releaseSettings

bintrayOrganization := None
bintrayRepository := "sbt-plugins"
