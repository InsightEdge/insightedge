package com.gigaspaces.spark.rdd

import com.gigaspaces.spark.context.GigaSpacesConfig
import com.gigaspaces.spark.model.GridModel
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.{Partition, SparkContext, TaskContext}

import scala.reflect.ClassTag

class GigaSpacesSqlRDD[R <: GridModel : ClassTag](
                                     gsConfig: GigaSpacesConfig,
                                     sc: SparkContext,
                                     query: String,
                                     queryParams: Seq[Any],
                                     readRddBufferSize: Int
                                   ) extends GigaSpacesAbstractRDD[R](gsConfig, sc, None, readRddBufferSize) {

  @DeveloperApi
  override def compute(split: Partition, context: TaskContext): Iterator[R] = {
    val gsQuery = createGigaSpacesQuery[R](bucketize(query, split), queryParams)
    compute[R](split, gsQuery, R => R, context)
  }

  @DeveloperApi
  override def supportsBuckets(): Boolean = true

}
