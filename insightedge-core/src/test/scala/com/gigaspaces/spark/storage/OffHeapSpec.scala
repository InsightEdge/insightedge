package com.gigaspaces.spark.storage

import com.gigaspaces.spark.utils.{OffHeap, Spark, GigaSpaces, GsConfig}
import org.apache.spark.storage.StorageLevel
import org.scalatest.FunSpec

/**
  * @author Oleksiy_Dyagilev
  */
class OffHeapSpec extends FunSpec with GsConfig with GigaSpaces with Spark with OffHeap {

  it("should work with GigaSpaces Off-Heap") {
    val rdd = sc.parallelize(dataSeq(100))
    assert(rdd.count == 100)
    rdd.persist(StorageLevel.OFF_HEAP)
    assert(rdd.count == 100)
  }

}
