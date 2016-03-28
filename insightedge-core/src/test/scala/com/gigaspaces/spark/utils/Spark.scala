package com.gigaspaces.spark.utils

import com.gigaspaces.spark.implicits._
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

/**
  * Suite mixin that starts and stops Spark
  *
  * @author Oleksiy_Dyagilev
  */
trait Spark extends BeforeAndAfterAll with BeforeAndAfterEach {
  self: Suite with GsConfig with GigaSpaces =>

  var sc: SparkContext = _

  def createSparkConf(): SparkConf = {
    new SparkConf()
      .setAppName("gigaspaces-test")
      .setMaster("local[2]")
      .setGigaSpaceConfig(gsConfig)
  }

  override protected def beforeEach() = {
    super.beforeEach()
    val sparkConf = createSparkConf()
    sc = new SparkContext(sparkConf)
  }

  override protected def afterEach() = {
    super.afterEach()
    sc.stopGigaSpacesContext()
  }

}
