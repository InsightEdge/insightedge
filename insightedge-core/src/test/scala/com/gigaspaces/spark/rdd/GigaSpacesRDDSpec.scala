package com.gigaspaces.spark.rdd

import com.gigaspaces.spark.implicits._
import com.gigaspaces.spark.utils.{GigaSpaceFactory, GigaSpaces, GsConfig, Spark}
import org.apache.spark.rdd.RDD
import org.scalatest._

class GigaSpacesRDDSpec extends FunSpec with GsConfig with GigaSpaces with Spark {

  it("should successfully store data to Data Grid") {
    val rdd: RDD[Data] = sc.parallelize(dataSeq(1000))
    rdd.saveToGrid()

    val dataCount = spaceProxy.count(new Data())
    assert(dataCount == 1000, "Data objects weren't written to the space")
    for (i <- 1 to 1000) {
      val template = new Data(routing = i)
      assert(spaceProxy.read(template).data == "data" + i, "Data objects weren't written to the space")
    }
  }

  it("should successfully read data from Data Grid") {
    writeDataSeqToDataGrid(1000)
    val rdd = sc.gridRdd[Data]()
    val sum = rdd.map(data => data.routing).sum()
    val expectedSum = (1 to 1000).sum
    assert(sum == expectedSum, "")
  }

  it("should read only data of RDD type") {
    writeDataSeqToDataGrid(10)

    val strings = (1 to 20).map(i => new GridString("string " + i))
    spaceProxy.writeMultiple(randomBucket(strings).toArray)

    val rdd = sc.gridRdd[GridString]()
    assert(rdd.count() == 20)
  }

  it("should have bucketed partitions set by user") {
    val rdd = sc.gridRdd[GridString](splitCount = Some(8))
    assert(rdd.getNumPartitions == 16)
    assert(GigaSpaceFactory.clusteredCacheSize() == 1)
    assert(GigaSpaceFactory.directCacheSize() == 2)
  }

  it("should have default partitions as 4x grid partitions") {
    val rdd = sc.gridRdd[GridString](None)
    assert(rdd.getNumPartitions == 4 * 2)
    assert(GigaSpaceFactory.clusteredCacheSize() == 1)
    assert(GigaSpaceFactory.directCacheSize() == 2)
  }

}
