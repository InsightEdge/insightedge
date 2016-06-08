package com.gigaspaces.spark.utils

import sys.process._

/**
  * @author Oleksiy_Dyagilev
  */
object ProcessUtils {


  def execAssertSucc(cmd: String): Int = {
    println(s"Executing: $cmd")
    val exitCode = cmd.!
    assert(exitCode == 0, s"Non zero exit code executing $cmd")
    exitCode
  }

}
