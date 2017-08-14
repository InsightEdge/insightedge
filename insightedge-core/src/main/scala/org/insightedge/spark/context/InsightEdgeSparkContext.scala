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

package org.insightedge.spark.context

import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext
import org.insightedge.spark.mllib.MLInstance
import org.insightedge.spark.model.BucketedGridModel
import org.insightedge.spark.rdd.{InsightEdgeRDD, InsightEdgeSqlRDD}
import org.insightedge.spark.utils.InsightEdgeConstants._
import org.insightedge.spark.utils.{BucketIdSeq, GridProxyFactory}

import scala.reflect._
import scala.util.Random

class InsightEdgeSparkContext(@transient val sc: SparkContext, val ieConfig: InsightEdgeConfig) extends Serializable {

  lazy val gridSqlContext = new SQLContext(sc)

  def grid = GridProxyFactory.getOrCreateClustered(ieConfig)

  /**
    * Read dataset from GigaSpaces Data Grid.
    *
    * @tparam R GigaSpaces space class
    * @param splitCount        only applicable for BucketedGridModel, number of spark partitions per datagrid partition, defaults to x4. For non-bucketed types this parameter is ignored.
    * @param readRddBufferSize buffer size of the underlying iterator that reads from the grid
    * @return InsightEdge RDD
    */
  def gridRdd[R: ClassTag](splitCount: Option[Int] = Some(DefaultSplitCount), readRddBufferSize: Int = DefaultReadBufferSize): InsightEdgeRDD[R] = {
    new InsightEdgeRDD[R](ieConfig, sc, splitCount, readRddBufferSize)
  }

  /**
    * Read dataset from Data Grid with GigaSpaces SQL query
    *
    * @param sqlQuery          SQL query to be executed on Data Grid
    * @param queryParams       params for SQL quey
    * @param splitCount        only applicable for BucketedGridModel, number of spark partitions per datagrid partition, defaults to x4. For non-bucketed types this parameter is ignored.
    * @param readRddBufferSize buffer size of the underlying iterator that reads from the grid
    * @tparam R GigaSpaces space class
    * @return
    */
  def gridSql[R: ClassTag](sqlQuery: String, queryParams: Seq[Any] = Seq(), splitCount: Option[Int] = Some(DefaultSplitCount), readRddBufferSize: Int = DefaultReadBufferSize): InsightEdgeSqlRDD[R] = {
    new InsightEdgeSqlRDD[R](ieConfig, sc, sqlQuery, queryParams, Seq.empty[String], splitCount, readRddBufferSize)
  }

  /**
    * Load ml/mllib instance (model, pipeline, etc) from the Data Grid
    *
    * @param name name of ml/mllib instance
    * @tparam R instance class
    * @return loaded instance
    */
  def loadMLInstance[R: ClassTag](name: String): Option[R] = {
    val mlModel = grid.readById(classOf[MLInstance], name)
    mlModel match {
      case MLInstance(id, instance: R) => Some(instance)
      case _ => None
    }
  }

  /**
    * Save object to Data Grid.
    *
    * This is a method on SparkContext, so it can be called from Spark driver only.
    *
    * @param value object to save
    * @tparam R type of object
    */
  def saveToGrid[R: ClassTag](value: R): Unit = {
    if (classOf[BucketedGridModel].isAssignableFrom(classTag[R].runtimeClass)) {
      value.asInstanceOf[BucketedGridModel].metaBucketId = Random.nextInt(BucketsCount)
    }
    grid.write(value)
  }

  /**
    * Save objects to Data Grid.
    *
    * This is a method on SparkContext, so it can be called from Spark driver only.
    *
    * @param values    object to save
    * @param batchSize batch size for grid write operations
    * @tparam R type of object
    */
  def saveMultipleToGrid[R: ClassTag](values: Iterable[R], batchSize: Int = DefaultDriverWriteBatchSize): Unit = {
    val assignBucketId = classOf[BucketedGridModel].isAssignableFrom(classTag[R].runtimeClass)
    val bucketIdSeq = new BucketIdSeq()

    val batches = values.grouped(batchSize)
    batches.foreach { batch =>
      val batchArray = batch.asInstanceOf[Iterable[Object]].toArray

      if (assignBucketId) {
        batchArray.foreach { bean =>
          bean.asInstanceOf[BucketedGridModel].metaBucketId = bucketIdSeq.next()
        }
      }

      grid.writeMultiple(batchArray)
    }
  }

  /**
    * Stops internal Spark context and cleans all resources (connections to Data Grid, etc)
    */
  def stopInsightEdgeContext() = {
    sc.stop()
  }

}
