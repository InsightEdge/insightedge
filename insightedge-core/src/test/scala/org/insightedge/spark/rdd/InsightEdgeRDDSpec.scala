package org.insightedge.spark.rdd

import org.insightedge.spark.fixture.{InsightEdge, IEConfig}
import org.insightedge.spark.implicits
import implicits.basic._
import org.insightedge.spark.utils.InsightEdgeConstants.DefaultSplitCount
import org.insightedge.spark.utils._
import com.j_spaces.core.client.SQLQuery
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.insightedge.JSpatialData
import org.apache.spark.sql.insightedge.model.SpatialData
import org.insightedge.spark.fixture.Spark
import org.insightedge.spark.utils.{JavaSpaceClass, ScalaSpaceClass}
import org.openspaces.spatial.ShapeFactory
import org.openspaces.spatial.ShapeFactory._
import org.scalatest._

class InsightEdgeRDDSpec extends FlatSpec with IEConfig with InsightEdge with Spark {

  it should "store data to Data Grid" taggedAs ScalaSpaceClass in {
    val rdd: RDD[Data] = sc.parallelize(dataSeq(1000))
    rdd.saveToGrid()

    val dataCount = spaceProxy.readMultiple(dataQuery()).length
    assert(dataCount == 1000, "Data objects weren't written to the space")
    for (i <- 1 to 1000) {
      assert(spaceProxy.read(dataQuery(s"routing = $i")).data == "data" + i, "Data objects weren't written to the space")
    }
  }

  it should "store data to Data Grid [java]" taggedAs JavaSpaceClass in {
    val rdd = parallelizeJavaSeq(sc, () => (1L to 1000).map(i => new JData(i, "data" + i)))
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

  it should "read bucketed data from Data Grid" taggedAs ScalaSpaceClass in {
    writeBucketedDataSeqToDataGrid(1000)
    val rdd = sc.gridRdd[BucketedData]()
    val sum = rdd.map(data => data.routing).sum()
    val expectedSum = (1 to 1000).sum
    assert(sum == expectedSum, "")
  }

  it should "read bucketed data from Data Grid [java]" taggedAs JavaSpaceClass in {
    writeJBucketedDataSeqToDataGrid(1000)
    val rdd = sc.gridRdd[JBucketedData]()
    val sum = rdd.map(data => data.getRouting.toLong).sum()
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
    spaceProxy.writeMultiple(strings.toArray)

    val rdd = sc.gridRdd[GridString]()
    assert(rdd.count() == 20)
  }

  it should "read only data of RDD type [java]" taggedAs JavaSpaceClass in {
    writeJDataSeqToDataGrid(10)

    val strings = (1 to 20).map(i => new GridString("string " + i))
    spaceProxy.writeMultiple(strings.toArray)

    val rdd = sc.gridRdd[GridString]()
    assert(rdd.count() == 20)
  }

  it should "have bucketed partitions set by user" taggedAs ScalaSpaceClass in {
    val rdd = sc.gridRdd[BucketedGridString](splitCount = Some(8))
    assert(rdd.supportsBuckets())
    assert(rdd.getNumPartitions == 8 * NumberOfGridPartitions)
    assert(GridProxyFactory.clusteredCacheSize() == 1)
    assert(GridProxyFactory.directCacheSize() == NumberOfGridPartitions)
  }

  it should "have bucketed partitions set by user [java]" taggedAs JavaSpaceClass in {
    val rdd = sc.gridRdd[JBucketedData](splitCount = Some(8))
    assert(rdd.supportsBuckets())
    assert(rdd.getNumPartitions == 8 * NumberOfGridPartitions)
    assert(GridProxyFactory.clusteredCacheSize() == 1)
    assert(GridProxyFactory.directCacheSize() == NumberOfGridPartitions)
  }

  it should "have default bucketed partitions as 4x grid partitions" taggedAs ScalaSpaceClass in {
    val rdd = sc.gridRdd[BucketedGridString](None)
    assert(rdd.supportsBuckets())
    assert(rdd.getNumPartitions == DefaultSplitCount * NumberOfGridPartitions)
    assert(GridProxyFactory.clusteredCacheSize() == 1)
    assert(GridProxyFactory.directCacheSize() == NumberOfGridPartitions)
  }

  it should "have default bucketed partitions as 4x grid partitions [java]" taggedAs JavaSpaceClass in {
    val rdd = sc.gridRdd[JBucketedData](None)
    assert(rdd.supportsBuckets())
    assert(rdd.getNumPartitions == DefaultSplitCount * NumberOfGridPartitions)
    assert(GridProxyFactory.clusteredCacheSize() == 1)
    assert(GridProxyFactory.directCacheSize() == NumberOfGridPartitions)
  }

  it should "ignore splitCount for non-bucketed models" taggedAs ScalaSpaceClass in {
    val rdd = sc.gridRdd[GridString](None)
    assert(!rdd.supportsBuckets())
    assert(rdd.getNumPartitions == NumberOfGridPartitions)
    assert(GridProxyFactory.clusteredCacheSize() == 1)
    assert(GridProxyFactory.directCacheSize() == NumberOfGridPartitions)
  }

  it should "ignore splitCount for non-bucketed models [java]" taggedAs JavaSpaceClass in {
    val rdd = sc.gridRdd[JData](None)
    assert(!rdd.supportsBuckets())
    assert(rdd.getNumPartitions == NumberOfGridPartitions)
    assert(GridProxyFactory.clusteredCacheSize() == 1)
    assert(GridProxyFactory.directCacheSize() == NumberOfGridPartitions)
  }

  it should "read spatial data" taggedAs ScalaSpaceClass in {
    val searchedPoint = point(0, 0)
    spaceProxy.write(SpatialData(id = null, routing = 1, null, null, searchedPoint))

    assert(sc.gridSql[SpatialData]("point spatial:within ?", Seq(rectangle(-1, 1, -1, 1))).count() == 1)
    assert(sc.gridSql[SpatialData]("point spatial:within ?", Seq(rectangle(1, 2, -1, 1))).count() == 0)
  }

  it should "read spatial data [java]" taggedAs JavaSpaceClass in {
    val searchedPoint = point(0, 0)
    spaceProxy.write(new JSpatialData(1L, searchedPoint))

    assert(sc.gridSql[JSpatialData]("point spatial:within ?", Seq(rectangle(-1, 1, -1, 1))).count() == 1)
    assert(sc.gridSql[JSpatialData]("point spatial:within ?", Seq(rectangle(1, 2, -1, 1))).count() == 0)
  }

}
