package com.gigaspaces.spark.rdd

import java.lang.reflect.Method

import com.gigaspaces.spark.context.GigaSpacesConfig
import com.gigaspaces.spark.model.GridModel
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.sql.Row
import org.apache.spark.sql.insightedge.{ClassDefLang, JavaClassDef, ScalaClassDef}
import org.apache.spark.{Partition, SparkContext, TaskContext}

import scala.reflect._

class GigaSpacesClassDataFrameRDD[R <: GridModel : ClassTag](
                                                              gsConfig: GigaSpacesConfig,
                                                              sc: SparkContext,
                                                              query: String,
                                                              queryParams: Seq[Any],
                                                              queryFields: Seq[String],
                                                              classDefLang: ClassDefLang,
                                                              readRddBufferSize: Int
                                                            ) extends GigaSpacesAbstractRDD[Row](gsConfig, sc, None, readRddBufferSize) {

  val clazz = classTag[R].runtimeClass

  @DeveloperApi
  override def compute(split: Partition, context: TaskContext): Iterator[Row] = {

    val gsQuery = createGigaSpacesQuery[R](bucketize(query, split), queryParams, queryFields)

    val methods: Seq[Method] = queryFields.map(fieldNameToMethod)

    def converter(element: Any): Row = {
      Row.fromSeq(methods.map(_.invoke(element)))
    }

    computeInternal(split, gsQuery, converter, context)
  }

  private def fieldNameToMethod(fieldName: String): Method = {
    val methodName = classDefLang match {
      case ScalaClassDef => fieldName
      case JavaClassDef => "get" + fieldName.capitalize
    }
    clazz.getMethod(methodName)
  }


  @DeveloperApi
  override def supportsBuckets(): Boolean = true

}
