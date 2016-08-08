package org.insightedge.spark.fixture

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.insightedge.spark.implicits.basic._
import org.scalatest.concurrent.Eventually
import org.scalatest.time.Span
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite, time}

/**
  * @author Oleksiy_Dyagilev
  */
trait SparkStreaming extends BeforeAndAfterAll with BeforeAndAfterEach {
  self: Suite with IEConfig =>

  var ssc: StreamingContext = _

  override protected def beforeEach() = {
    super.beforeEach()

    val sparkConf = new SparkConf()
      .setAppName("insightedge-streaming-test")
      .setMaster("local[2]")
      .setInsightEdgeConfig(ieConfig)

    ssc = new StreamingContext(sparkConf, Seconds(1))
  }

  override protected def afterEach() = {
    ssc.stop()
    ssc.sparkContext.stopInsightEdgeContext()
    super.afterEach()
  }

  def timeout(sec: Int) = Eventually.PatienceConfig(Span(sec, time.Seconds))

}
