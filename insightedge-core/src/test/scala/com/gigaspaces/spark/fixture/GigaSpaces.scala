package com.gigaspaces.spark.fixture

import com.gigaspaces.spark.model.GridModel
import com.gigaspaces.spark.rdd.{Data, JData}
import com.gigaspaces.spark.utils.{GigaSpaceFactory, GigaSpaceUtils}
import org.openspaces.core.GigaSpace
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import org.springframework.context.support.ClassPathXmlApplicationContext

import scala.util.Random

/**
  * Suite mixin that starts GigaSpaces data grid
  *
  * @author Oleksiy_Dyagilev
  */
trait GigaSpaces extends BeforeAndAfterAll with BeforeAndAfterEach {
  self: Suite with GsConfig =>

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

  protected def dataSeq(count: Int): Seq[Data] = (1L to count).map(i => new Data(i, "data" + i))

  protected def jDataSeq(count: Int): Seq[JData] = (1L to count).map(i => new JData(i, "data" + i))

  protected def writeDataSeqToDataGrid(data: Seq[GridModel]): Unit = spaceProxy.writeMultiple(randomBucket(data).toArray)

  protected def writeDataSeqToDataGrid(count: Int): Unit = writeDataSeqToDataGrid(dataSeq(count))

  protected def writeJDataSeqToDataGrid(count: Int): Unit = writeDataSeqToDataGrid(jDataSeq(count))

  protected def randomBucket(seq: Seq[GridModel]): Seq[GridModel] = {
    seq.foreach(data => data.metaBucketId = Random.nextInt(GigaSpaceUtils.BucketsCount))
    seq
  }

  protected def randomBucket(data: GridModel): GridModel = {
    data.metaBucketId = Random.nextInt(GigaSpaceUtils.BucketsCount)
    data
  }
}
