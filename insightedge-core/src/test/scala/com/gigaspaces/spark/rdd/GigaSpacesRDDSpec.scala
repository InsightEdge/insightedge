package com.gigaspaces.spark.rdd

import com.gigaspaces.spark.fixture.{GigaSpaces, GsConfig, Spark}
import com.gigaspaces.spark.implicits._
import com.gigaspaces.spark.utils._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.insightedge.JSpatialData
import org.apache.spark.sql.insightedge.model.SpatialData
import org.openspaces.spatial.ShapeFactory
import org.openspaces.spatial.ShapeFactory._
import org.scalatest._

class GigaSpacesRDDSpec extends FlatSpec with GsConfig with GigaSpaces with Spark {

  it should "store data to Data Grid" taggedAs ScalaSpaceClass in {
    val rdd: RDD[Data] = sc.parallelize(dataSeq(1000))
    rdd.saveToGrid()

    val dataCount = spaceProxy.count(new Data())
    assert(dataCount == 1000, "Data objects weren't written to the space")
    for (i <- 1 to 1000) {
      val template = new Data(routing = i)
      assert(spaceProxy.read(template).data == "data" + i, "Data objects weren't written to the space")
    }
  }

  it should "store data to Data Grid [java]" taggedAs JavaSpaceClass in {
    val rdd: RDD[JData] = sc.parallelize(jDataSeq(1000))
    rdd.saveToGrid()

    val dataCount = spaceProxy.count(new JData())
    assert(dataCount == 1000, "Data objects weren't written to the space")
    for (i <- 1L to 1000) {
      val template = new JData()
      template.setRouting(i)
      assert(spaceProxy.read(template).getData == "data" + i, "Data objects weren't written to the space")
    }
  }

  it should "read data from Data Grid" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)
    val rdd = sc.gridRdd[Data]()
    val sum = rdd.map(data => data.routing).sum()
    val expectedSum = (1 to 1000).sum
    assert(sum == expectedSum, "")
  }

  it should "read data from Data Grid [java]" taggedAs JavaSpaceClass in {
    writeJDataSeqToDataGrid(1000)
    val rdd = sc.gridRdd[JData]()
    val sum = rdd.map(data => data.getRouting.toLong).sum()
    val expectedSum = (1 to 1000).sum
    assert(sum == expectedSum, "")
  }

  it should "read only data of RDD type" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(10)

    val strings = (1 to 20).map(i => new GridString("string " + i))
    spaceProxy.writeMultiple(randomBucket(strings).toArray)

    val rdd = sc.gridRdd[GridString]()
    assert(rdd.count() == 20)
  }

  it should "read only data of RDD type [java]" taggedAs JavaSpaceClass in {
    writeJDataSeqToDataGrid(10)

    val strings = (1 to 20).map(i => new GridString("string " + i))
    spaceProxy.writeMultiple(randomBucket(strings).toArray)

    val rdd = sc.gridRdd[GridString]()
    assert(rdd.count() == 20)
  }

  it should "have bucketed partitions set by user" taggedAs ScalaSpaceClass in {
    val rdd = sc.gridRdd[GridString](splitCount = Some(8))
    assert(rdd.getNumPartitions == 16)
    assert(GigaSpaceFactory.clusteredCacheSize() == 1)
    assert(GigaSpaceFactory.directCacheSize() == 2)
  }

  it should "have default partitions as 4x grid partitions" taggedAs ScalaSpaceClass in {
    val rdd = sc.gridRdd[GridString](None)
    assert(rdd.getNumPartitions == 4 * 2)
    assert(GigaSpaceFactory.clusteredCacheSize() == 1)
    assert(GigaSpaceFactory.directCacheSize() == 2)
  }

  it should "read spatial data" taggedAs ScalaSpaceClass in {
    val searchedPoint = point(0, 0)
    spaceProxy.write(randomBucket(SpatialData(id = null, routing = 1, null, null, searchedPoint)))

    assert(sc.gridSql[SpatialData]("point spatial:within ?", Seq(rectangle(-1, 1, -1, 1))).count() == 1)
    assert(sc.gridSql[SpatialData]("point spatial:within ?", Seq(rectangle(1, 2, -1, 1))).count() == 0)
  }

  it should "read spatial data [java]" taggedAs JavaSpaceClass in {
    val searchedPoint = point(0, 0)
    spaceProxy.write(randomBucket(new JSpatialData(1L, searchedPoint)))

    assert(sc.gridSql[JSpatialData]("point spatial:within ?", Seq(rectangle(-1, 1, -1, 1))).count() == 1)
    assert(sc.gridSql[JSpatialData]("point spatial:within ?", Seq(rectangle(1, 2, -1, 1))).count() == 0)
  }

}
