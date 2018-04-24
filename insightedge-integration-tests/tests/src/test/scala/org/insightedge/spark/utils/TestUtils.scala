package org.insightedge.spark.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object TestUtils{

  def printLnWithTimestamp(x: Any): Unit = {
    printf(s"${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS"))}    ")
    println(x)
  }

}
