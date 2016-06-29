package com.gigaspaces.spark.python

import com.gigaspaces.spark.java.JavaGigaSpacesSparkContext
import com.gigaspaces.spark.model.GridModel
import com.gigaspaces.spark.utils.ClassLoadUtils
import org.apache.spark.api.java.JavaRDD
import org.apache.spark.python.SerDeUtilDelegate

/**
  * @author Oleksiy_Dyagilev
  */
class PythonGigaSpacesSparkContext(javaGsSparkContext: JavaGigaSpacesSparkContext) {

  // TODO: parameters like splitCount
  def gridRdd(fullClassName: String): JavaRDD[Array[Byte]] = {
    val classTag = ClassLoadUtils.loadClass(fullClassName)
    val clazz: Class[GridModel] = classTag.runtimeClass.asInstanceOf[Class[GridModel]]
    val jRDD: JavaRDD[GridModel] = javaGsSparkContext.gridRdd(clazz)
    SerDeUtilDelegate.javaToPython(jRDD)
  }

}
