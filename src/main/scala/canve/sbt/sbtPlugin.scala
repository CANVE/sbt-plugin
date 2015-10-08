package canve.sbt

import sbt.Keys._
import sbt._

// TODO: add cleanup as per http://www.scala-sbt.org/0.13.5/docs/Getting-Started/More-About-Settings.html#appending-with-dependencies-and

object ExtractorSbtPlugin extends AutoPlugin {

  val compilerPluginOrg = "canve"
  val compilerPluginVersion = "0.0.1"
  val compilerPluginArtifact = "compiler-plugin"
  val compilerPluginNameProperty = "canve" // this is defined in the compiler plugin's code
    
  val sbtCommandName = "canve-enable"

  val aggregateFilter = ScopeFilter( inAggregates(ThisProject), inConfigurations(Compile) ) // must be outside of the 'coverageAggregate' task (see: https://github.com/sbt/sbt/issues/1095 or https://github.com/sbt/sbt/issues/780)

  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements
  
  // global settings needed for the bootstrap
  override lazy val projectSettings = Seq(
    commands += Command.command(sbtCommandName, 
                                "Instruments all projects in the current build definition such that they run canve during compilation",
                                "Instrument all projects in the current build definition such that they run canve during compilation")
                                (instrument()),

    libraryDependencies += compilerPluginOrg % (compilerPluginArtifact + "_" + scalaBinaryVersion.value) % compilerPluginVersion % "provided",
    
    compile in Compile <<= (compile in Compile) map { compileAnalysis =>
      println("has run after compile")
      compileAnalysis
    }
  )

  private def instrument(): State => State = { state =>
    val extracted = Project.extract(state)
    val newSettings = extracted.structure.allProjectRefs map { projRef =>
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
    extracted.append(newSettings, state)
  }

  println("sbt canve plugin loaded - use " + sbtCommandName + " to run canve as part of every compilation")
}
