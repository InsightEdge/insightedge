package com.gigaspaces.spark.rdd

import com.gigaspaces.spark.context.GigaSpacesConfig
import com.gigaspaces.spark.model.GridModel
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.sql.Row
import org.apache.spark.{Partition, SparkContext, TaskContext}

import scala.reflect.ClassTag

class GigaSpacesClassDataFrameRDD[R <: GridModel : ClassTag](
                                                              gsConfig: GigaSpacesConfig,
                                                              sc: SparkContext,
                                                              query: String,
                                                              queryParams: Seq[Any],
                                                              queryFields: Seq[String],
                                                              converter: R => Row,
                                                              readRddBufferSize: Int
                                                            ) extends GigaSpacesAbstractRDD[Row](gsConfig, sc, None, readRddBufferSize) {

  @DeveloperApi
  override def compute(split: Partition, context: TaskContext): Iterator[Row] = {
    val gsQuery = createGigaSpacesQuery[R](bucketize(query, split), queryParams, queryFields)
    compute(split, gsQuery, converter, context)
  }

  @DeveloperApi
  override def supportsBuckets(): Boolean = true

}
