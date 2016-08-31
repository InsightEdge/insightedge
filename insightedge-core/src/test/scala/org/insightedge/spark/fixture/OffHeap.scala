package org.insightedge.spark.fixture

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.scalatest.Suite

/**
  * Suite mixin that enables InsightEdge off-heap external block manager
  *
  * @author Oleksiy_Dyagilev
  */
trait OffHeap extends Spark {
  self: Suite with IEConfig with InsightEdge =>

  override def createSpark(): SparkSession = {
    SparkSession
      .builder()
      .appName("insightedge-test")
      .config("spark.externalBlockStore.blockManager", "org.apache.spark.storage.InsightEdgeBlockManager")
      .master("local[2]")
      .getOrCreate()
  }

}
