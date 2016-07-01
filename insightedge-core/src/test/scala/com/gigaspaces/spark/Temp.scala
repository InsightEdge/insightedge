package com.gigaspaces.spark

import com.gigaspaces.spark.fixture.GigaSpaces
import com.gigaspaces.spark.model.GridModel
import com.gigaspaces.spark.rdd.{Data, JData}
import com.gigaspaces.spark.utils.GigaSpaceUtils
import org.openspaces.core.{GigaSpace, GigaSpaceConfigurer}
import org.openspaces.core.space.UrlSpaceConfigurer

import scala.util.Random

/**
  * @author Oleksiy_Dyagilev
  */
object Temp extends App {

  val gs = new GigaSpaceConfigurer(new UrlSpaceConfigurer("jini://*/*/insightedge-space?groups=insightedge&locators=127.0.0.1:4174").space()).gigaSpace()

  writeDataSeqToDataGrid(100)


  protected def dataSeq(count: Int): Seq[Data] = (1L to count).map(i => new Data(i, "data" + i))

  protected def jDataSeq(count: Int): Seq[JData] = (1L to count).map(i => new JData(i, "data" + i))

  protected def writeDataSeqToDataGrid(data: Seq[GridModel]): Unit = gs.writeMultiple(randomBucket(data).toArray)

  protected def writeDataSeqToDataGrid(count: Int): Unit = writeDataSeqToDataGrid(dataSeq(count))

  protected def writeJDataSeqToDataGrid(count: Int): Unit = writeDataSeqToDataGrid(jDataSeq(count))

  protected def randomBucket(seq: Seq[GridModel]): Seq[GridModel] = {
    seq.foreach(data => data.metaBucketId = Random.nextInt(GigaSpaceUtils.BucketsCount))
    seq
  }

}
