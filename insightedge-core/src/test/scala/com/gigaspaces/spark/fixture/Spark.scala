package com.gigaspaces.spark.fixture

import com.gigaspaces.spark.implicits.basic._
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

/**
  * Suite mixin that starts and stops Spark before and after each test
  *
  * @author Oleksiy_Dyagilev
  */
trait Spark extends BeforeAndAfterAll with BeforeAndAfterEach {
  self: Suite with GsConfig with GigaSpaces =>

  var sc: SparkContext = _
  var sql: SQLContext = _

  def createSparkConf(): SparkConf = {
    new SparkConf()
      .setAppName("gigaspaces-test")
      .setMaster("local[2]")
      .setGigaSpaceConfig(gsConfig)
  }

  override protected def beforeEach() = {
    val sparkConf = createSparkConf()
    sc = new SparkContext(sparkConf)
    sql = new SQLContext(sc)
    super.beforeEach()
  }

  override protected def afterEach() = {
    sc.stopGigaSpacesContext()
    super.afterEach()
  }

}
