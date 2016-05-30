package com.gigaspaces.spark.utils

import scala.language.postfixOps
import sys.process._

/**
  * @author Oleksiy_Dyagilev
  */
object DockerUtils {

  /**
    * runs command in container, blocks until process is finished and returns the exit code
    */
  def dockerExec(containerId: String, command: String): Int = {
    val processCommand = s"docker exec $containerId $command"
    println(s"running command: $processCommand")
    processCommand !
  }

  /**
    * runs command in container, blocks until process is finished and returns stdout as a string
    */
  def dockerExecAndOutput(containerId: String, command: String): String = {
    s"docker exec $containerId $command" !!
  }


}
