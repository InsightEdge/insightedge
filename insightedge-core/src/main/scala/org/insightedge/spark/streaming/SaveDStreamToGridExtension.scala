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

package org.insightedge.spark.streaming

import org.apache.spark.streaming.dstream.DStream
import org.insightedge.spark.context.InsightEdgeConfig
import org.insightedge.spark.utils.GridProxyFactory

import scala.annotation.meta.param
import scala.reflect.ClassTag

/**
  * Extra functions available on DStream through an implicit conversion.
  *
  * @author Oleksiy_Dyagilev
  */
// compilation warning is due to https://issues.scala-lang.org/browse/SI-8813
class SaveDStreamToGridExtension[T: ClassTag](@transient dStream: DStream[T]) extends Serializable {

  /**
    * Saves DStream to Data Grid
    *
    * @param writeBatchSize batch size for grid write operations
    */
  def saveToGrid(writeBatchSize: Int = 1000) = {
    val sparkConfig = dStream.context.sparkContext.getConf
    val ieConfig = InsightEdgeConfig.fromSparkConf(sparkConfig)

    dStream.foreachRDD { rdd =>
      rdd.foreachPartition { partitionOfRecords =>
        val gridProxy = GridProxyFactory.getOrCreateClustered(ieConfig)
        val batches = partitionOfRecords.grouped(writeBatchSize)

        batches.foreach { batch =>
          val arr = batch.asInstanceOf[Iterable[Object]].toArray
          gridProxy.writeMultiple(arr)
        }
      }
    }
  }


}
