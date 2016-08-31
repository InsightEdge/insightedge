package org.insightedge.spark.fixture

import org.apache.spark.sql.{SQLContext, SparkSession}
import org.apache.spark.{SparkConf, SparkContext}
import org.insightedge.spark.implicits.basic._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

/**
  * Suite mixin that starts and stops Spark before and after each test
  *
  * @author Oleksiy_Dyagilev
  */
trait Spark extends BeforeAndAfterAll with BeforeAndAfterEach {
  self: Suite with IEConfig with InsightEdge =>

  var spark: SparkSession = _
  var sc: SparkContext = _

  def createSpark(): SparkSession = {
    SparkSession
      .builder()
      .appName("insightedge-test")
      .master("local[2]")
      .insightEdgeConfig(ieConfig)
      .getOrCreate()
  }

  override protected def beforeEach() = {
    spark = createSpark()
    sc = spark.sparkContext
    super.beforeEach()
  }

  override protected def afterEach() = {
    spark.stop()
    // TODO: move stopInsightEdgeContext() to SparkSession
    //    sc.stopInsightEdgeContext()
    super.afterEach()
  }

}
