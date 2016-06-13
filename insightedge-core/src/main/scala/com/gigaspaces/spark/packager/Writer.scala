package com.gigaspaces.spark.packager

import com.gigaspaces.spark.packager.model.Instance

/**
  * @author Danylo_Hurin.
  */
object Writer {

  val ERROR = "ERROR"

  /** Format:
    * 10.0.0.1:id=1;id=2,backup_id=1 10.0.0.2:id=2;id=1,backup_id=1
    */
  def writeToConsole(instancesToHosts: SpaceInstancesToHosts): Unit = {
    val stringResult = instancesToHosts.instancesToHosts.map { t =>
      val host = t._1
      val instances = t._2.mkString(";")
      s"$host:$instances"
    }.mkString(" ")
    println(stringResult)
  }

  def writeToConsole(instances: Set[Instance]): Unit = {
    val stringResult = instances.mkString(";")
    println(stringResult)
  }

  def writeErrorAndExit(msg: String): Unit = {
    println(s"$ERROR: $msg")
    sys.exit(1)
  }

  def writeErrorAndExit(msg: String, t: Throwable): Unit = {
    println(s"$ERROR: $msg")
    t.printStackTrace()
    sys.exit(1)
  }

  def writeErrorAndExit(t: Throwable): Unit = {
    println(s"$ERROR:")
    t.printStackTrace()
    sys.exit(1)
  }

}
