package org.apache.spark.sql.insightedge

import com.gigaspaces.spark.model.GridModel
import org.apache.spark.Logging
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{SQLContext, DataFrame, Row}
import org.apache.spark.sql.sources.{Filter, PrunedFilteredScan, InsertableRelation, BaseRelation}

import com.gigaspaces.spark.implicits._

import scala.reflect.ClassTag

private[insightedge] class GigaspacesRelation[T: ClassTag](
                                                            override val sqlContext: SQLContext)
  extends BaseRelation
    with InsertableRelation
    with PrunedFilteredScan
    with Logging {

  private val wipDataframe = sqlContext.sparkContext.gridDataFrame[T]()

  override def schema: StructType = wipDataframe.schema

  override def insert(data: DataFrame, overwrite: Boolean): Unit = {
  }

  override def buildScan(requiredColumns: Array[String], filters: Array[Filter]): RDD[Row] = {
    wipDataframe.rdd
  }

}
