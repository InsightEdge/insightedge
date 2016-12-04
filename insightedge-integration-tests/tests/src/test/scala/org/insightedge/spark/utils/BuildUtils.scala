package org.insightedge.spark.utils

/**
  * @author Oleksiy_Dyagilev
  */
object BuildUtils {

  val BuildVersion = Option(System.getProperty("dist.version", "1.1.0-SNAPSHOT")).getOrElse(throw new Exception("System property 'dist.version' is not set"))
  val BuildEdition = Option(System.getProperty("dist.edition", "premium")).getOrElse(throw new Exception("System property 'dist.edition' is not set"))
  val GitBranch = Option(System.getProperty("git.branch", "ffffff")).getOrElse(throw new Exception("System property 'git.branch' is not set"))
  val IEHome = Option(System.getProperty("dist.dir", "/home/kobi/opt/insightedge/1.0/gigaspaces-insightedge-1.1.0-SNAPSHOT-premium")).getOrElse(throw new Exception("System property 'dist.dir' is not set"))
}