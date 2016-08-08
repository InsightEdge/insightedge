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
    val gsQuery = createDocumentInsightEdgeQuery(typeName, query, queryParams, queryFields)
    computeInternal[SpaceDocument](split, gsQuery, context)
  }

}
