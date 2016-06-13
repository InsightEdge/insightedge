package com.gigaspaces.spark.fixture

import com.gigaspaces.spark.implicits._
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.scalatest.concurrent.Eventually
import org.scalatest.time.Span
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite, time}

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
    ssc.stop()
    ssc.sparkContext.stopGigaSpacesContext()
    super.afterEach()
  }

  def timeout(sec: Int) = Eventually.PatienceConfig(Span(sec, time.Seconds))

}
