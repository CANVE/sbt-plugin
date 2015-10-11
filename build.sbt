organization := "canve"

name := "sbt-plugin"

sbtPlugin := true

libraryDependencies ++= Seq("com.github.tototoshi" %% "scala-csv" % "1.3.0-SNAPSHOT",
                            "canve" %% "compiler-plugin" % "0.0.1",
                            "canve" %% "simplest" % "0.1-SNAPSHOT")
                            
