package com.gigaspaces.spark.fixture

import com.gigaspaces.spark.model.BucketedGridModel
import com.gigaspaces.spark.rdd.{BucketedData, Data, JBucketedData, JData}
import com.gigaspaces.spark.utils.{GigaSpaceConstants, GigaSpaceFactory}
import com.j_spaces.core.client.SQLQuery
import org.apache.commons.lang3.RandomStringUtils
import org.apache.spark.SparkContext
import org.openspaces.core.GigaSpace
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import org.springframework.context.support.ClassPathXmlApplicationContext

import scala.reflect.ClassTag
import scala.util.Random

/**
  * Suite mixin that starts GigaSpaces data grid
  *
  * @author Oleksiy_Dyagilev
  */
trait GigaSpaces extends BeforeAndAfterAll with BeforeAndAfterEach {
  self: Suite with GsConfig =>

  // see configuration in cluster-test-config.xml
  val NumberOfGridPartitions = 2

  var spaceProxy: GigaSpace = _

  override protected def beforeAll() = {
    val ctx = new ClassPathXmlApplicationContext("cluster-test-config.xml")
    spaceProxy = GigaSpaceFactory.getOrCreateClustered(gsConfig)
    super.beforeAll()
  }

  override protected def afterEach() = {
    spaceProxy.clear(new Object())
    super.afterEach()
  }

  def dataSeq(count: Int): Seq[Data] = (1L to count).map(i => new Data(i, "data" + i))

  def jDataSeq(count: Int): Seq[JData] = (1L to count).map(i => new JData(i, "data" + i))

  def bucketedDataSeq(count: Int): Seq[BucketedData] = (1L to count).map(i => new BucketedData(i, "data" + i))

  def bucketedJDataSeq(count: Int): Seq[JBucketedData] = (1L to count).map(i => new JBucketedData(i, "data" + i))

  def writeDataSeqToDataGrid(data: Seq[AnyRef]): Unit = spaceProxy.writeMultiple(bucketizeIfPossible(data).toArray)

  def writeDataSeqToDataGrid(count: Int): Unit = writeDataSeqToDataGrid(dataSeq(count))

  def writeBucketedDataSeqToDataGrid(count: Int): Unit = writeDataSeqToDataGrid(bucketedDataSeq(count))

  def writeJBucketedDataSeqToDataGrid(count: Int): Unit = writeDataSeqToDataGrid(bucketedJDataSeq(count))

  def writeJDataSeqToDataGrid(count: Int): Unit = writeDataSeqToDataGrid(jDataSeq(count))

  def parallelizeJavaSeq[T: ClassTag](sc: SparkContext, createSeqFn: () => Seq[T]) = {
    // our test java models are not Serializable
    // we cannot sc.parallelize() non serializable objects, so we create them on executor
    sc.parallelize(Seq(1)).flatMap(_ => createSeqFn())
  }

  def bucketizeIfPossible(seq: Seq[AnyRef]): Seq[AnyRef] = {
    seq.map {
      case data: BucketedGridModel => bucketize(data)
      case any => any
    }
  }

  def bucketize(data: BucketedGridModel): BucketedGridModel = {
    data.metaBucketId = Random.nextInt(GigaSpaceConstants.BucketsCount)
    data
  }

  def dataQuery(query: String = "", params: Seq[Object] = Seq()): SQLQuery[Data] = new SQLQuery[Data](classOf[Data], query, params.toArray)

  def randomString() = RandomStringUtils.random(10, "abcdefghijklmnopqrst")

}
