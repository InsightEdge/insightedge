package org.insightedge.spark.utils

/**
  * @author Oleksiy_Dyagilev
  */
object BuildUtils {

  val BuildVersion = Option(System.getProperty("dist.version")).getOrElse(throw new Exception("System property 'dist.version' is not set"))
  val BuildEdition = Option(System.getProperty("dist.edition")).getOrElse(throw new Exception("System property 'dist.edition' is not set"))

}
