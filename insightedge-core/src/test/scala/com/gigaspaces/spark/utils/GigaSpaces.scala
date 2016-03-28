package com.gigaspaces.spark.utils

import com.gigaspaces.spark.model.GridModel
import com.gigaspaces.spark.rdd.Data
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


  override protected def afterEach() = {
    super.afterEach()
    spaceProxy.clear(new Object())
  }

  override protected def beforeAll() = {
    super.beforeAll()
    val ctx = new ClassPathXmlApplicationContext("cluster-test-config.xml")
    spaceProxy = GigaSpaceFactory.getOrCreateClustered(gsConfig)
  }

  protected def dataSeq(count: Int): Seq[Data] = (1 to count).map(i => new Data(i, "data" + i))

  protected def writeDataSeqToDataGrid(count: Int) = randomBucket(dataSeq(count)).foreach(data => spaceProxy.write(data))

  protected def randomBucket(seq: Seq[GridModel]): Seq[GridModel] = {
    seq.foreach(data => data.metaBucketId = Random.nextInt(GigaSpaceUtils.BucketsCount))
    seq
  }
}
