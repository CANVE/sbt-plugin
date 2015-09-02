package canve.sbt

import sbt.Keys._
import sbt._

// TODO: add cleanup as per http://www.scala-sbt.org/0.13.5/docs/Getting-Started/More-About-Settings.html#appending-with-dependencies-and

object ExtractorSbtPlugin extends AutoPlugin {

  val compilerPluginOrg = "matanster"
  val compilerPluginVersion = "0.0.1"
  val compilerPluginArtifact = "extractor"

  val aggregateFilter = ScopeFilter( inAggregates(ThisProject), inConfigurations(Compile) ) // must be outside of the 'coverageAggregate' task (see: https://github.com/sbt/sbt/issues/1095 or https://github.com/sbt/sbt/issues/780)

  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  // global settings needed for the bootstrap
  override lazy val projectSettings = Seq(
    commands += Command.command("canve-enable", 
                                "Instruments all projects in the current build definition such that they run canve during compilation",
                                "Instrument all projects in the current build definition such that they run canve during compilation")
                                (instrument()),
    libraryDependencies += compilerPluginOrg % (compilerPluginArtifact + "_" + scalaBinaryVersion.value) % compilerPluginVersion % "provided" intransitive()
    
    // TODO: replace the following by project specific scalacOptions
    // scalacOptions in(Compile, compile) ++= ScalacOptions.value 
  )

  private def instrument(): State => State = { state =>
    val extracted = Project.extract(state)
    val newSettings = extracted.structure.allProjectRefs map { projRef =>
      val projectName = projRef.project
      println("CANVE instrumenting project " + projectName)
      
      lazy val ScalacOptions = Def.task {
        // search for the compiler plugin
        val deps: Seq[File] = update.value matching configurationFilter("provided")
        deps.find(_.getAbsolutePath.contains(compilerPluginArtifact)) match {
          case None => throw new Exception(s"Fatal: compilerPluginArtifact not in libraryDependencies")
          case Some(pluginPath) => 
            Seq(Some(s"-Xplugin:${pluginPath.getAbsolutePath}"),
                Some(s"-P:CANVE:projectName:$projectName")).flatten
        }
      }
      scalacOptions in projRef ++= ScalacOptions.value
    }
    extracted.append(newSettings, state)
  }

  println("Sbt CANVE plugin loaded - use canve-instrument to run CANVE during every compilation")
}
