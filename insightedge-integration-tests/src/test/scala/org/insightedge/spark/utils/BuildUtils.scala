package org.insightedge.spark.utils

/**
  * @author Oleksiy_Dyagilev
  */
object BuildUtils {

  val BuildVersion = Option(System.getProperty("dist.version")).getOrElse(throw new Exception("System property 'dist.version' is not set"))
  val BuildEdition = Option(System.getProperty("dist.edition")).getOrElse(throw new Exception("System property 'dist.edition' is not set"))
  val GitBranch = Option(System.getProperty("git.branch")).getOrElse(throw new Exception("System property 'git.branch' is not set"))
  val IEHome = Option(System.getProperty("dist.dir")).getOrElse(throw new Exception("System property 'dist.dir' is not set"))

}