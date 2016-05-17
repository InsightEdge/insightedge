package com.gigaspaces.spark.storage

import com.gigaspaces.spark.utils._
import org.apache.spark.storage.StorageLevel
import org.scalatest.FlatSpec

/**
  * @author Oleksiy_Dyagilev
  */
class OffHeapSpec extends FlatSpec with GsConfig with GigaSpaces with Spark with OffHeap {

  it should "work with GigaSpaces Off-Heap" taggedAs ScalaSpaceClass in {
    val rdd = sc.parallelize(dataSeq(100))
    assert(rdd.count == 100)
    rdd.persist(StorageLevel.OFF_HEAP)
    assert(rdd.count == 100)
  }

  it should "work with GigaSpaces Off-Heap [java]" taggedAs JavaSpaceClass in {
    val rdd = sc.parallelize(jDataSeq(100))
    assert(rdd.count == 100)
    rdd.persist(StorageLevel.OFF_HEAP)
    assert(rdd.count == 100)
  }

}
