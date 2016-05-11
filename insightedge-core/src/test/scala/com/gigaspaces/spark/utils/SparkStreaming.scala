package com.gigaspaces.spark.utils

import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.SparkConf
import org.scalatest.concurrent.Eventually
import org.scalatest.time.Span
import org.scalatest.{time, BeforeAndAfterEach, BeforeAndAfterAll, Suite}
import com.gigaspaces.spark.implicits.basic._

/**
  * @author Oleksiy_Dyagilev
  */
trait SparkStreaming extends BeforeAndAfterAll with BeforeAndAfterEach {
  self: Suite with GsConfig =>

  var ssc: StreamingContext = _

  override protected def beforeEach() = {
    super.beforeEach()

    val sparkConf = new SparkConf()
      .setAppName("gigaspaces-streaming-test")
      .setMaster("local[2]")
      .setGigaSpaceConfig(gsConfig)

    ssc = new StreamingContext(sparkConf, Seconds(1))
  }

  override protected def afterEach() = {
    super.afterEach()
    ssc.stop()
    ssc.sparkContext.stopGigaSpacesContext()
  }

  def timeout(sec: Int) = Eventually.PatienceConfig(Span(sec, time.Seconds))

}
