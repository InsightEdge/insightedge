package org.insightedge.spark.utils

object Profiler {


  /**
    * Profiles your code work time.
    */
  def profile[R](message: String)(logFunction: String => Unit)(block: => R): R = {
    val start = System.nanoTime()
    val result = block
    val stop = System.nanoTime()
    val time = (BigDecimal(stop - start) / 1000000000).setScale(5, BigDecimal.RoundingMode.HALF_UP)
    logFunction(s"$message took " + time + " seconds")
    result
  }

}
