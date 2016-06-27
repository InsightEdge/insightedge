package com.gigaspaces.spark.python

import com.gigaspaces.spark.context.GigaSpacesSparkContext
import org.apache.spark.SparkContext

/**
  * A helper class to instantiate PythonGigaSpacesSparkContext from python using Py4J.
  *
  * @author Oleksiy_Dyagilev
  */
class PythonHelper {

  def createPythonGigaSpacesSparkContext(sc: SparkContext): PythonGigaSpacesSparkContext = {
    val gsSparkContext = com.gigaspaces.spark.implicits.gigaSpacesSparkContext(sc)
    new PythonGigaSpacesSparkContext(gsSparkContext)
  }

}
