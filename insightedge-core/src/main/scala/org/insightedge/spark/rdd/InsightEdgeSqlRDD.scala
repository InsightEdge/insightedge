package org.insightedge.spark.rdd

import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.{Partition, SparkContext, TaskContext}
import org.insightedge.spark.context.InsightEdgeConfig

import scala.reflect.ClassTag

class InsightEdgeSqlRDD[R: ClassTag](
                                     gsConfig: InsightEdgeConfig,
                                     sc: SparkContext,
                                     query: String,
                                     queryParams: Seq[Any],
                                     queryFields: Seq[String],
                                     splitCount: Option[Int],
                                     readRddBufferSize: Int
                                   ) extends InsightEdgeAbstractRDD[R](gsConfig, sc, splitCount, readRddBufferSize) {

  @DeveloperApi
  override def compute(partition: Partition, context: TaskContext): Iterator[R] = {
    val sqlQuery = if (supportsBuckets()) bucketize(query, partition) else query
    val gsQuery = createInsightEdgeQuery[R](sqlQuery, queryParams, queryFields)
    computeInternal[R](partition, gsQuery, context)
  }

}
