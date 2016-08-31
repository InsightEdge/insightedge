package org.insightedge.spark

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.insightedge.{DataFrameImplicits, GeospatialImplicits}
import org.apache.spark.{SparkConf, SparkContext}
import org.insightedge.spark.context.{InsightEdgeConfig, InsightEdgeSparkContext}
import org.insightedge.spark.ml.MLImplicits
import org.insightedge.spark.mllib.MLlibImplicits
import org.insightedge.spark.rdd.InsightEdgeRDDFunctions
import org.insightedge.spark.streaming.StreamingImplicits
import org.insightedge.spark.utils.LocalCache

import scala.language.implicitConversions
import scala.reflect.ClassTag

/**
  * Enables Spark with InsightEdge connector API
  *
  * @author Oleksiy_Dyagilev
  */
object implicits {

  object basic extends BasicImplicits

  object streaming extends StreamingImplicits

  object mllib extends MLlibImplicits

  object ml extends MLImplicits

  object dataframe extends DataFrameImplicits

  object geospatial extends GeospatialImplicits

  object all extends BasicImplicits
    with MLlibImplicits
    with MLImplicits
    with StreamingImplicits
    with DataFrameImplicits
    with GeospatialImplicits


  /** this is to not create a new instance of InsightEdgeSparkContext every time implicit conversion fired **/
  private val insightEdgeSparkContextCache = new LocalCache[SparkContext, InsightEdgeSparkContext]()

  trait BasicImplicits {

    implicit def insightEdgeSparkContext(sc: SparkContext): InsightEdgeSparkContext = {
      insightEdgeSparkContextCache.getOrElseUpdate(sc, new InsightEdgeSparkContext(sc))
    }

    implicit def saveToDataGridExtension[R: ClassTag](rdd: RDD[R]): InsightEdgeRDDFunctions[R] = {
      new InsightEdgeRDDFunctions[R](rdd)
    }

    implicit class SparkConfExtension(sparkConf: SparkConf) {
      def setInsightEdgeConfig(ieConfig: InsightEdgeConfig): SparkConf = {
        ieConfig.populateSparkConf(sparkConf)
        sparkConf
      }
    }

    implicit class SparkSessionBuilderExtension(builder: SparkSession.Builder) {
      def insightEdgeConfig(ieConfig: InsightEdgeConfig): SparkSession.Builder = {
        ieConfig.populateSparkSessionBuilder(builder)
        builder
      }
    }

  }

}

