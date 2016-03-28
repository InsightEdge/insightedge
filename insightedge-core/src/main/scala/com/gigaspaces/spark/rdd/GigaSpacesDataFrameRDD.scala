package com.gigaspaces.spark.rdd

import com.gigaspaces.spark.context.GigaSpacesConfig
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.sql.Row
import org.apache.spark.{Partition, SparkContext, TaskContext}

import scala.reflect.ClassTag

class GigaSpacesDataFrameRDD[T: ClassTag](
                                           gsConfig: GigaSpacesConfig,
                                           sc: SparkContext,
                                           sqlQuery: String,
                                           convertFunc: T => Row,
                                           readRddBufferSize: Int,
                                           args: Any*
                                         ) extends GigaSpacesAbstractRDD[Row](gsConfig, sc, None, readRddBufferSize) {

  @DeveloperApi
  override def compute(split: Partition, context: TaskContext): Iterator[Row] = {
    val gsQuery = createGigaSpacesQuery[T](sqlQuery, args: _*)
    compute(split, gsQuery, convertFunc, context)
  }

}
