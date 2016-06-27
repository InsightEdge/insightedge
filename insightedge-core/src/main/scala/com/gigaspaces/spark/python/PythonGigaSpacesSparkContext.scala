package com.gigaspaces.spark.python

import com.gigaspaces.spark.context.GigaSpacesSparkContext
import com.gigaspaces.spark.java.JavaGigaSpacesSparkContext
import com.gigaspaces.spark.model.GridModel
import com.gigaspaces.spark.rdd.GigaSpacesRDD
import com.gigaspaces.spark.utils.ClassLoadUtils

import scala.reflect.ClassTag

/**
  * @author Oleksiy_Dyagilev
  */
class PythonGigaSpacesSparkContext(gsSparkContext: GigaSpacesSparkContext) extends JavaGigaSpacesSparkContext(gsSparkContext) {

  def gridRdd(fullClassName: String): GigaSpacesRDD[GridModel] = {
    val classTag = ClassLoadUtils.loadClass(fullClassName).asInstanceOf[ClassTag[GridModel]]
    gsSparkContext.gridRdd()(classTag)
  }

}
