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

package org.insightedge.spark.rdd

import com.gigaspaces.document.SpaceDocument
import com.gigaspaces.query.QueryResultType
import com.j_spaces.core.client.SQLQuery
import org.apache.spark.rdd.RDD
import org.apache.spark.{Partition, SparkContext, TaskContext}
import org.insightedge.spark.context.InsightEdgeConfig
import org.insightedge.spark.impl.{InsightEdgePartition, InsightEdgeQueryIterator, ProfilingIterator}
import org.insightedge.spark.model.BucketedGridModel
import org.insightedge.spark.utils.{GridProxyFactory, GridProxyUtils, Profiler}
import org.openspaces.core.GigaSpace

import scala.reflect._

abstract class InsightEdgeAbstractRDD[R: ClassTag](
                                                    ieConfig: InsightEdgeConfig,
                                                    sc: SparkContext,
                                                    splitCount: Option[Int],
                                                    readRddBufferSize: Int
                                                 ) extends RDD[R](sc, deps = Nil) {

  /**
    * Reads rdd data from Data Grid for given partition(split)
    *
    * @param split         partition
    * @param dataGridQuery Data Grid query to read data
    * @tparam T type of Data Grid query
    * @return iterator over Data Grid
    */
  protected def computeInternal[T](split: Partition, dataGridQuery: SQLQuery[T], context: TaskContext): Iterator[T] = {
    val startTime = System.currentTimeMillis()
    val gsPartition = split.asInstanceOf[InsightEdgePartition]
    logInfo(s"Reading partition $gsPartition")

    val proxy = createProxy()

    val iterator = profileWithInfo("createIterator") {
      new ProfilingIterator(new InsightEdgeQueryIterator[T](proxy.iterator(dataGridQuery, readRddBufferSize)))
    }

    context.addTaskCompletionListener { _ =>
      val endTime = System.currentTimeMillis()
      val duration = (endTime - startTime) / 1000.0
      logInfo(f"Fetched ${iterator.count()} rows for partition $gsPartition in $duration%.3f s. May include time of pipelined operation.")
    }

    iterator
  }

  /**
    * @return if RDD implementation supports bucketing or not
    */
  private[rdd] def supportsBuckets(): Boolean = {
    classOf[BucketedGridModel].isAssignableFrom(classTag[R].runtimeClass)
  }

  /**
    * Create GigaSpaces Query
    *
    * @param sqlQuery query statement
    * @param params   bounded parameters
    * @param fields   projected fields
    * @tparam T type of query
    * @return GigaSpaces sql query
    */
  protected def createInsightEdgeQuery[T: ClassTag](sqlQuery: String, partition: Partition, params: Seq[Any] = Seq(), fields: Seq[String] = Seq()): SQLQuery[T] = {
    val clazz = classTag[T].runtimeClass
    val query = new SQLQuery[T](clazz.asInstanceOf[Class[T]], sqlQuery)
    query.setRouting(partition.index)
    query.setParameters(params.map(_.asInstanceOf[Object]): _*)
    if (fields.nonEmpty) {
      query.setProjections(fields.toArray: _*)
    }
    query
  }

  /**
    * Create GigaSpaces Query for SpaceDocuments
    *
    * @param typeName name of the documents type
    * @param sqlQuery query statement
    * @param params   bounded parameters
    * @param fields   projected fields
    * @return GigaSpaces sql query
    */
  protected def createDocumentInsightEdgeQuery(typeName: String, partition: Partition, sqlQuery: String, params: Seq[Any] = Seq(), fields: Seq[String] = Seq()): SQLQuery[SpaceDocument] = {
    val query = new SQLQuery[SpaceDocument](typeName, sqlQuery, QueryResultType.DOCUMENT)
    query.setRouting(partition.index)
    query.setParameters(params.map(_.asInstanceOf[Object]): _*)
    if (fields.nonEmpty) {
      query.setProjections(fields.toArray: _*)
    }
    query
  }

  /**
    * Create a query by metaBucketId for given bucketed partition or empty query for non-bucketed partition
    *
    * @param split the partition bean
    * @return sql query string with bucket range
    */
  protected def bucketQuery(split: Partition): String = {
    val partition = split.asInstanceOf[InsightEdgePartition]
    val rangeQuery = for {
      bottom <- partition.bucketRangeBottom
      top <- partition.bucketRangeTop
    } yield s"metaBucketId >= $bottom and metaBucketId < $top"

    rangeQuery.getOrElse("")
  }

  /**
    * Wraps given query into (...) and appends 'and `bucketQuery`' in the end.
    *
    * @param query     given query
    * @param partition given partition
    * @return query appended with bucket ids
    */
  protected def bucketize(query: String, partition: Partition): String = {
    if (query.trim.isEmpty) {
      bucketQuery(partition)
    } else {
      s"($query) and ${bucketQuery(partition)}"
    }
  }

  /**
    * Gets partitions for the given cache RDD.
    *
    * @return Partitions
    */
  override protected def getPartitions: Array[Partition] = {
    profileWithInfo("getPartitions") {
      val dataGridPartitions = GridProxyUtils.buildGridPartitions[R](ieConfig, splitCount, supportsBuckets()).sortBy(_.id)
      logInfo(s"Found data grid partitions $dataGridPartitions")

      dataGridPartitions.toArray
    }
  }

  /**
    * Gets preferred locations for the given partition.
    *
    * @param split Split partition.
    * @return
    */
  override protected def getPreferredLocations(split: Partition): Seq[String] = {
    profileWithInfo("getPreferredLocations") {
      val gridPartition = split.asInstanceOf[InsightEdgePartition]
      val preferredHost = gridPartition.hostName
      logInfo(s"Preferred location for partition ${split.index} is $preferredHost")
      Seq(preferredHost)
    }
  }

  protected def createProxy(): GigaSpace = {
    GridProxyFactory.getOrCreateClustered(ieConfig)
  }


  protected def profileWithInfo[T](message: String)(block: => T): T = Profiler.profile(message)(logInfo(_))(block)

}

