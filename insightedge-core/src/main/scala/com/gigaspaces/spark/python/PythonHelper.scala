package com.gigaspaces.spark.python

import com.gigaspaces.spark.context.GigaSpacesSparkContext
import com.gigaspaces.spark.java.JavaGigaSpacesSparkContext
import org.apache.spark.SparkContext
import org.apache.spark.api.java.JavaSparkContext

/**
  * A helper class to instantiate PythonGigaSpacesSparkContext from python using Py4J.
  *
  * @author Oleksiy_Dyagilev
  */
class PythonHelper {

  def createPythonGigaSpacesSparkContext(sc: JavaSparkContext): PythonGigaSpacesSparkContext = {
    val javaGsSparkContext = new JavaGigaSpacesSparkContext(sc)
    new PythonGigaSpacesSparkContext(javaGsSparkContext)
  }

}
