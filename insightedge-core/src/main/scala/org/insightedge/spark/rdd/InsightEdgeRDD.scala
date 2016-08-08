package org.insightedge.spark.rdd

import org.insightedge.spark.context.InsightEdgeConfig
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.{Partition, SparkContext, TaskContext}

import scala.reflect.ClassTag

class InsightEdgeRDD[R: ClassTag](
                                   ieConfig: InsightEdgeConfig,
                                   sc: SparkContext,
                                   splitCount: Option[Int],
                                   readRddBufferSize: Int
                                ) extends InsightEdgeAbstractRDD[R](ieConfig, sc, splitCount, readRddBufferSize) {

  @DeveloperApi
  override def compute(partition: Partition, context: TaskContext): Iterator[R] = {
    val sqlQuery = if (supportsBuckets()) bucketQuery(partition) else ""
    val gsQuery = createInsightEdgeQuery[R](sqlQuery)
    computeInternal[R](partition, gsQuery, context)
  }

}
