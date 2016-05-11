package com.gigaspaces.spark

import com.gigaspaces.spark.context.{GigaSpacesConfig, GigaSpacesSparkContext}
import com.gigaspaces.spark.mllib.MllibImplicits
import com.gigaspaces.spark.rdd.SaveRddToGridExtension
import com.gigaspaces.spark.streaming.StreamingImplicits
import com.gigaspaces.spark.utils.LocalCache
import org.apache.spark.sql.insightedge.DataFrameImplicits
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

/**
  * Enables Spark with GigaSpaces connector API
  *
  * @author Oleksiy_Dyagilev
  */
object implicits {

  object basic extends BasicImplicits

  object streaming extends StreamingImplicits

  object mllib extends MllibImplicits

  object datafarame extends DataFrameImplicits

  // Mix all
  object all extends BasicImplicits
    with MllibImplicits
    with StreamingImplicits
    with DataFrameImplicits

}

trait BasicImplicits {
  /** this is to not create a new instance of GigaSpacesSparkContext every time implicit conversion fired **/
  val gigaSpacesSparkContextCache = new LocalCache[SparkContext, GigaSpacesSparkContext]()

  implicit def gigaSpacesSparkContext(sc: SparkContext): GigaSpacesSparkContext = {
    gigaSpacesSparkContextCache.getOrElseUpdate(sc, new GigaSpacesSparkContext(sc))
  }

  implicit def saveToDataGridExtension[R : ClassTag](rdd: RDD[R]): SaveRddToGridExtension[R] = {
    new SaveRddToGridExtension[R](rdd)
  }

  implicit class SparkConfExtension(sparkConf: SparkConf) {
    def setGigaSpaceConfig(gsConfig: GigaSpacesConfig): SparkConf = {
      gsConfig.populateSparkConf(sparkConf)
      sparkConf
    }
  }

}
