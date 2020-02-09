/*
 * Copyright (c) 2016, GigaSpaces Technologies, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.insightedge.spark.fixture

import com.j_spaces.core.client.SQLQuery
import org.apache.commons.lang3.RandomStringUtils
import org.apache.spark.sql.SparkSession
import org.apache.spark.SparkContext
import org.insightedge.spark.context.InsightEdgeConfig
import org.insightedge.spark.implicits.basic._
import org.insightedge.spark.model.BucketedGridModel
import org.insightedge.spark.rdd.{BucketedData, Data, JBucketedData, JData}
import org.insightedge.spark.utils.{GridProxyFactory, InsightEdgeConstants}
import org.openspaces.core.GigaSpace
import org.scalatest._
import org.springframework.context.support.ClassPathXmlApplicationContext

import scala.reflect.ClassTag
import scala.util.Random

/**
  * Suite mixin that starts and stops Spark before and after each test
  *
  * @author Oleksiy_Dyagilev
  */
trait InsightEdge extends fixture.FlatSpec {
  self: Suite =>

  val ieConfig = InsightEdgeConfig("test-space", Some("spark"), Some("localhost:4174"))

  // see configuration in cluster-test-config.xml
  val NumberOfGridPartitions = 2

  private val spaceProxy: GigaSpace = {
    val ctx = new ClassPathXmlApplicationContext("cluster-test-config.xml")
    GridProxyFactory.getOrCreateClustered(ieConfig)
  }

  case class FixtureParam(spark: SparkSession, sc: SparkContext, spaceProxy: GigaSpace)

  override def withFixture(test: OneArgTest): Outcome = {
    println("Before create Spark... ")
    val spark = createSpark()
    val sc = spark.sparkContext
    val theFixture = FixtureParam(spark, sc, spaceProxy)
    try {
      println("Before invoking test... ")
      withFixture(test.toNoArgTest(theFixture))
    } finally{
      println("Before stopping Insight Edge... ")
      spaceProxy.clear(new Object())
      spark.stopInsightEdgeContext()
    }
  }

  def createSpark(): SparkSession = {
    SparkSession
      .builder()
      .appName("insightedge-test")
      .master("local[2]")
      .insightEdgeConfig(ieConfig)
      .getOrCreate()
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
    data.metaBucketId = Random.nextInt(InsightEdgeConstants.BucketsCount)
    data
  }

  def dataQuery(query: String = "", params: Seq[Object] = Seq()): SQLQuery[Data] = new SQLQuery[Data](classOf[Data], query, params.toArray)

  def randomString() = RandomStringUtils.random(10, "abcdefghijklmnopqrst")

}