package com.gigaspaces.spark.rdd

import com.gigaspaces.spark.context.GigaSpacesConfig
import com.gigaspaces.spark.model.BucketedGridModel
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.{Partition, SparkContext, TaskContext}

import scala.reflect.ClassTag

class GigaSpacesSqlRDD[R : ClassTag](
                                     gsConfig: GigaSpacesConfig,
                                     sc: SparkContext,
                                     query: String,
                                     queryParams: Seq[Any],
                                     queryFields: Seq[String],
                                     splitCount: Option[Int],
                                     readRddBufferSize: Int
                                   ) extends GigaSpacesAbstractRDD[R](gsConfig, sc, splitCount, readRddBufferSize) {

  @DeveloperApi
  override def compute(partition: Partition, context: TaskContext): Iterator[R] = {
    val sqlQuery = if (supportsBuckets()) bucketize(query, partition) else query

    val gsQuery = createGigaSpacesQuery[R](sqlQuery, queryParams, queryFields)
    computeInternal[R](partition, gsQuery, R => R, context)
  }

}
