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
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.{Partition, SparkContext, TaskContext}
import org.insightedge.spark.context.InsightEdgeConfig

class InsightEdgeDocumentRDD(
                              ieConfig: InsightEdgeConfig,
                              sc: SparkContext,
                              typeName: String,
                              query: String,
                              queryParams: Seq[Any],
                              queryFields: Seq[String],
                              readRddBufferSize: Int
                           ) extends InsightEdgeAbstractRDD[SpaceDocument](ieConfig, sc, None, readRddBufferSize) {

  @DeveloperApi
  override def compute(split: Partition, context: TaskContext): Iterator[SpaceDocument] = {
    val gsQuery = createDocumentInsightEdgeQuery(typeName, split, query, queryParams, queryFields)
     computeInternal[SpaceDocument](split, gsQuery, context)
  }

}
