package org.insightedge.spark.fixture

import org.apache.spark.sql.SQLContext
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

  var sc: SparkContext = _
  var sql: SQLContext = _

  def createSparkConf(): SparkConf = {
    new SparkConf()
      .setAppName("insightedge-test")
      .setMaster("local[2]")
      .setInsightEdgeConfig(ieConfig)
  }

  override protected def beforeEach() = {
    val sparkConf = createSparkConf()
    sc = new SparkContext(sparkConf)
    sql = new SQLContext(sc)
    super.beforeEach()
  }

  override protected def afterEach() = {
    sc.stopInsightEdgeContext()
    super.afterEach()
  }

}
