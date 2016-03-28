package com.gigaspaces.spark

import com.gigaspaces.spark.context.{GigaSpacesConfig, GigaSpacesSparkContext}
import com.gigaspaces.spark.rdd.SaveRddToGridExtension
import com.gigaspaces.spark.utils.LocalCache
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

/**
  * Enables Spark with GigaSpaces connector API
  *
  * @author Oleksiy_Dyagilev
  */
object implicits {

  /** this is to not create a new instance of GigaSpacesSparkContext every time implicit conversion fired **/
  val gigaSpacesSparkContextCache = new LocalCache[SparkContext, GigaSpacesSparkContext]()

  implicit def gigaSpacesSparkContext(sc: SparkContext): GigaSpacesSparkContext = {
    gigaSpacesSparkContextCache.getOrElseUpdate(sc, new GigaSpacesSparkContext(sc))
  }

  implicit def saveToDataGridExtension[T: ClassTag](rdd: RDD[T]): SaveRddToGridExtension[T] = {
    new SaveRddToGridExtension[T](rdd)
  }

  implicit class SparkConfExtension(sparkConf: SparkConf) {
    def setGigaSpaceConfig(gsConfig: GigaSpacesConfig): SparkConf = {
      gsConfig.populateSparkConf(sparkConf)
      sparkConf
    }
  }

}
