package com.gigaspaces.spark.rdd

import com.gigaspaces.document.SpaceDocument
import com.gigaspaces.spark.context.GigaSpacesConfig
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.{Partition, SparkContext, TaskContext}

class GigaSpacesDocumentRDD(
                             gsConfig: GigaSpacesConfig,
                             sc: SparkContext,
                             typeName: String,
                             query: String,
                             queryParams: Seq[Any],
                             queryFields: Seq[String],
                             readRddBufferSize: Int
                           ) extends GigaSpacesAbstractRDD[SpaceDocument](gsConfig, sc, None, readRddBufferSize) {

  @DeveloperApi
  override def compute(split: Partition, context: TaskContext): Iterator[SpaceDocument] = {
    val gsQuery = createDocumentGigaSpacesQuery(typeName, query, queryParams, queryFields)

    computeInternal[SpaceDocument](split, gsQuery, d => d, context)
  }

}
