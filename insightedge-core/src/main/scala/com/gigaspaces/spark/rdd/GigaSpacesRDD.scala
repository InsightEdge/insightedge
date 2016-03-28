package com.gigaspaces.spark.rdd

import com.gigaspaces.spark.context.GigaSpacesConfig
import com.gigaspaces.spark.impl.GigaSpacesPartition
import com.gigaspaces.spark.model.GridModel
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.{Partition, SparkContext, TaskContext}

import scala.reflect.ClassTag

class GigaSpacesRDD[R <: GridModel : ClassTag](
                                                gsConfig: GigaSpacesConfig,
                                                sc: SparkContext,
                                                splitCount: Option[Int],
                                                readRddBufferSize: Int
                                              ) extends GigaSpacesAbstractRDD[R](gsConfig, sc, splitCount, readRddBufferSize) {

  @DeveloperApi
  override def compute(split: Partition, context: TaskContext): Iterator[R] = {
    val gsQuery = createGigaSpacesQuery[R](bucketQuery(split))
    compute[R](split, gsQuery, R => R, context)
  }

  @DeveloperApi
  override def supportsBuckets(): Boolean = true

}
