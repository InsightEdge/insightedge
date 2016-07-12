package com.gigaspaces.spark.storage

import com.gigaspaces.spark.fixture.{GigaSpaces, GsConfig, OffHeap, Spark}
import com.gigaspaces.spark.rdd.JData
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
    fail("wip")
  }

  it should "work with GigaSpaces Off-Heap [java]" taggedAs JavaSpaceClass in {
    val dataSeqFn = () => (1 to 100).map(i => new JData(i.toLong, "data" + i) with Serializable)
    val rdd = parallelizeJavaSeq(sc, dataSeqFn)
    assert(rdd.count == 100)
    rdd.persist(StorageLevel.OFF_HEAP)
    assert(rdd.count == 100)
    fail("wip")
  }

}
