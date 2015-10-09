package canve.sbt

import sbt.Keys._
import sbt._

// TODO: add cleanup as per http://www.scala-sbt.org/0.13.5/docs/Getting-Started/More-About-Settings.html#appending-with-dependencies-and

object ExtractorSbtPlugin extends AutoPlugin {

  val compilerPluginOrg = "canve"
  val compilerPluginVersion = "0.0.1"
  val compilerPluginArtifact = "compiler-plugin"
  val compilerPluginNameProperty = "canve" // this is defined in the compiler plugin's code
    
  val sbtLegacyCommandName = "canve"

  val sbtCommandName = "canve"

  // see: https://github.com/sbt/sbt/issues/1095 or https://github.com/sbt/sbt/issues/780
  val aggregateFilter: ScopeFilter.ScopeFilter = ScopeFilter( inAggregates(ThisProject), inConfigurations(Compile) ) 

  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  // global settings needed for the bootstrap
  override lazy val projectSettings = Seq(
    commands += Command.command(sbtLegacyCommandName,
                                "Instruments all projects in the current build definition such that they run canve during compilation",
                                "Instrument all projects in the current build definition such that they run canve during compilation")
                                (instrument()),

    libraryDependencies += compilerPluginOrg % (compilerPluginArtifact + "_" + scalaBinaryVersion.value) % compilerPluginVersion % "provided"
    
  )

  private def instrument(): State => State = { state =>
    val extracted: Extracted = Project.extract(state)
    val originalSettings = extracted.structure.allProjectRefs
    val newSettings: Seq[Def.Setting[Task[Seq[String]]]] = extracted.structure.allProjectRefs map { projRef =>
      val projectName = projRef.project
      println("canve instrumenting project " + projectName)
      
      lazy val ScalacOptions = Def.task {
        // search for the compiler plugin
        val deps: Seq[File] = update.value matching configurationFilter("provided")
        deps.find(_.getAbsolutePath.contains(compilerPluginArtifact)) match {
          case None => throw new Exception(s"Fatal: compilerPluginArtifact not in libraryDependencies")
          case Some(pluginPath) => 
            Seq(
              Some(s"-Yrangepos"),                                             // enables obtaining source ranges in the compiler plugin
              Some(s"-Xplugin:${pluginPath.getAbsolutePath}"),                 // hooks in the compiler plugin
              Some(s"-P:$compilerPluginNameProperty:projectName:$projectName") // passes the project name
            ).flatten
        }
      }
      scalacOptions in projRef ++= ScalacOptions.value
    }
    val newState = extracted.append(newSettings, state)

    val structure = extracted.structure
    
    extracted.structure.allProjectRefs map { projRef =>
      println(projRef)
      EvaluateTask(structure, clean, newState, projRef)
      EvaluateTask(structure, compile in Test, newState, projRef)
    }

    println("canve task done")

    newState
    
  }
  
  println("sbt canve plugin loaded - use " + sbtLegacyCommandName + " to run canve as part of every compilation")
}
