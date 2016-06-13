package com.gigaspaces.spark.fixture

import org.apache.spark.SparkConf
import org.scalatest.Suite

/**
  * Suite mixin that enables GigaSpaces off-heap external block manager
  *
  * @author Oleksiy_Dyagilev
  */
trait OffHeap extends Spark {
  self: Suite with GsConfig with GigaSpaces =>

  override def createSparkConf(): SparkConf = {
    val sparkConf = super.createSparkConf()
    sparkConf.set("spark.externalBlockStore.blockManager", "org.apache.spark.storage.GigaSpacesBlockManager")
  }

}
