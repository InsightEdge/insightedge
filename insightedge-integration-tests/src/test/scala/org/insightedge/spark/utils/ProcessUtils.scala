package org.insightedge.spark.utils

import sys.process._

/**
  * @author Oleksiy_Dyagilev
  */
object ProcessUtils {


  /**
    * Executes given command, blocks until it exits, asserts zero exit code
    */
  def execAssertSucc(cmd: String) = {
    println(s"Executing: $cmd")
    val exitCode = cmd.!
    assert(exitCode == 0, s"Non zero exit code executing $cmd")
  }

}
