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

import com.j_spaces.core.client.SQLQuery
import org.apache.spark.rdd.RDD
import org.insightedge.spark.implicits.basic._
import org.insightedge.spark.model.BucketedGridModel
import org.insightedge.spark.utils.{BucketIdSeq, GridProxyFactory}

import scala.reflect.{ClassTag, classTag}

/**
  * Extra functions available on RDD through an implicit conversion.
  *
  * @author Oleksiy_Dyagilev
  */
class InsightEdgeRDDFunctions[T: ClassTag](rdd: RDD[T]) extends Serializable {

  val DefaultWriteBatchSize = 1000

  val ieConfig = {
    rdd.sparkContext.ieConfig
  }

  /**
    * Saves values from given RDD into Data Grid
    *
    * @param saveBatchSize batch size used to write data to the grid
    */
  def saveToGrid(saveBatchSize: Int = DefaultWriteBatchSize) = {
    val assignBucketId = classOf[BucketedGridModel].isAssignableFrom(classTag[T].runtimeClass)
    val bucketIdSeq = new BucketIdSeq()

    rdd.foreachPartition { partition =>
      val space = GridProxyFactory.getOrCreateClustered(ieConfig)
      val batches = partition.grouped(saveBatchSize)

      batches.foreach { batch =>
        val batchArray = batch.asInstanceOf[Seq[Object]].toArray

        if (assignBucketId) {
          batchArray.foreach { bean =>
            val model = bean.asInstanceOf[BucketedGridModel]
            model.metaBucketId = bucketIdSeq.next()
          }
        }

        space.writeMultiple(batchArray)
      }
    }
  }

  /**
    * Executes a datagrid SQL query for each element in this RDD. Returns a new RDD with the tuple (element, query result items)
    *
    * @param query grid native SQL query
    * @param queryParamsConstructor function to construct SQL query parameters. Takes this RDD's element and returns a sequence of SQL parameters
    * @param projections SQL projections
    * @tparam U type of items SQL query executed on
    * @return a new RDD with the tuple (element, query result items)
    */
  def zipWithGridSql[U: ClassTag](query: String, queryParamsConstructor: T => Seq[Any], projections: Option[Seq[String]]): RDD[(T, Seq[U])] = {
    rdd.mapPartitions { partition =>
      val space = GridProxyFactory.getOrCreateClustered(ieConfig)
      partition.map { item =>
        val clazz = classTag[U].runtimeClass.asInstanceOf[Class[U]]
        val sqlQuery = new SQLQuery[U](clazz, query)
        val queryParams = queryParamsConstructor(item)
        sqlQuery.setParameters(queryParams.map(_.asInstanceOf[Object]): _*)
        projections.foreach(seq => sqlQuery.setProjections(seq: _*))
        val readItems = space.readMultiple(sqlQuery)
        (item, readItems.toSeq)
      }
    }
  }

}
