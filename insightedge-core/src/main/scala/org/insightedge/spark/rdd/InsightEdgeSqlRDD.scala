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

import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.{Partition, SparkContext, TaskContext}
import org.insightedge.spark.context.InsightEdgeConfig

import scala.reflect.ClassTag

class InsightEdgeSqlRDD[R: ClassTag](
                                      ieConfig: InsightEdgeConfig,
                                      sc: SparkContext,
                                      query: String,
                                      queryParams: Seq[Any],
                                      queryFields: Seq[String],
                                      splitCount: Option[Int],
                                      readRddBufferSize: Int
                                   ) extends InsightEdgeAbstractRDD[R](ieConfig, sc, splitCount, readRddBufferSize) {

  @DeveloperApi
  override def compute(partition: Partition, context: TaskContext): Iterator[R] = {
    val sqlQuery = if (supportsBuckets()) bucketize(query, partition) else query
    val gsQuery = createInsightEdgeQuery[R](sqlQuery, partition, queryParams, queryFields)
    computeInternal[R](partition, gsQuery, context)
  }

}
