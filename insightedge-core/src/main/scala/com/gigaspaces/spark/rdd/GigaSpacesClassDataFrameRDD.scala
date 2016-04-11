package com.gigaspaces.spark.rdd

import com.gigaspaces.spark.context.GigaSpacesConfig
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.sql.Row
import org.apache.spark.{Partition, SparkContext, TaskContext}

import scala.reflect.ClassTag

class GigaSpacesClassDataFrameRDD[T: ClassTag](
                                                gsConfig: GigaSpacesConfig,
                                                sc: SparkContext,
                                                query: String,
                                                queryParams: Seq[Any],
                                                queryFields: Seq[String],
                                                converter: T => Row,
                                                readRddBufferSize: Int
                                              ) extends GigaSpacesAbstractRDD[Row](gsConfig, sc, None, readRddBufferSize) {

  @DeveloperApi
  override def compute(split: Partition, context: TaskContext): Iterator[Row] = {
    val gsQuery = createGigaSpacesQuery[T](query, queryParams, queryFields)
    compute(split, gsQuery, converter, context)
  }

}
