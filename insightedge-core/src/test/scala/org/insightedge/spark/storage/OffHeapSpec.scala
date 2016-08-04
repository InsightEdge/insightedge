package org.insightedge.spark.storage

import org.insightedge.spark.fixture.{InsightEdge, IEConfig, OffHeap}
import org.insightedge.spark.utils._
import org.apache.spark.storage.StorageLevel
import org.insightedge.spark.fixture.Spark
import org.insightedge.spark.rdd.JData
import org.insightedge.spark.utils.{JavaSpaceClass, ScalaSpaceClass}
import org.scalatest.FlatSpec

/**
  * @author Oleksiy_Dyagilev
  */
class OffHeapSpec extends FlatSpec with IEConfig with InsightEdge with Spark with OffHeap {

  it should "work with InsightEdge Off-Heap" taggedAs ScalaSpaceClass in {
    val rdd = sc.parallelize(dataSeq(100))
    assert(rdd.count == 100)
    rdd.persist(StorageLevel.OFF_HEAP)
    assert(rdd.count == 100)
  }

  it should "work with InsightEdge Off-Heap [java]" taggedAs JavaSpaceClass in {
    val dataSeqFn = () => (1 to 100).map(i => new JData(i.toLong, "data" + i) with Serializable)
    val rdd = parallelizeJavaSeq(sc, dataSeqFn)
    assert(rdd.count == 100)
    rdd.persist(StorageLevel.OFF_HEAP)
    assert(rdd.count == 100)
  }

}
