package org.insightedge.spark.fixture

import org.apache.spark.SparkConf
import org.scalatest.Suite

/**
  * Suite mixin that enables InsightEdge off-heap external block manager
  *
  * @author Oleksiy_Dyagilev
  */
trait OffHeap extends Spark {
  self: Suite with IEConfig with InsightEdge =>

  override def createSparkConf(): SparkConf = {
    val sparkConf = super.createSparkConf()
    sparkConf.set("spark.externalBlockStore.blockManager", "org.apache.spark.storage.InsightEdgeBlockManager")
  }

}
