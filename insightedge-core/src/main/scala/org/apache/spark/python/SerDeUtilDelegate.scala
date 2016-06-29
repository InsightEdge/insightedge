package org.apache.spark.python

import org.apache.spark.api.java.JavaRDD
import org.apache.spark.api.python.SerDeUtil

/**
  * Exposes private[spark] SerDeUtils
  *
  * @author Oleksiy_Dyagilev
  */
object SerDeUtilDelegate {

  def javaToPython(jRDD: JavaRDD[_]): JavaRDD[Array[Byte]] = {
    SerDeUtil.javaToPython(jRDD)
  }


}
