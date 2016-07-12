package com.gigaspaces.spark.rdd

import com.gigaspaces.spark.context.GigaSpacesConfig
import com.gigaspaces.spark.impl.GigaSpacesPartition
import com.gigaspaces.spark.model.BucketedGridModel
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.{Partition, SparkContext, TaskContext}

import scala.reflect.ClassTag

class GigaSpacesRDD[R : ClassTag](
                                                gsConfig: GigaSpacesConfig,
                                                sc: SparkContext,
                                                splitCount: Option[Int],
                                                readRddBufferSize: Int
                                              ) extends GigaSpacesAbstractRDD[R](gsConfig, sc, splitCount, readRddBufferSize) {

  @DeveloperApi
  override def compute(partition: Partition, context: TaskContext): Iterator[R] = {
    val sqlQuery = if (supportsBuckets()) bucketQuery(partition) else ""

    val gsQuery = createGigaSpacesQuery[R](sqlQuery)
    computeInternal[R](partition, gsQuery, R => R, context)
  }

}
