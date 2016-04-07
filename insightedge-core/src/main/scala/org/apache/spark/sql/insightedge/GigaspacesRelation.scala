package org.apache.spark.sql.insightedge

import com.gigaspaces.spark.model.GridModel
import org.apache.spark.Logging
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql._
import org.apache.spark.sql.sources._

import com.gigaspaces.spark.implicits._

import scala.collection.mutable.ListBuffer
import scala.reflect._

private[insightedge] class GigaspacesRelation[T: ClassTag](
                                                            override val sqlContext: SQLContext)
  extends BaseRelation
    with InsertableRelation
    with PrunedFilteredScan
    with Logging {

  private val wipDataframe = sqlContext.sparkContext.gridDataFrame[T]()

  override def schema: StructType = wipDataframe.schema

  override def insert(data: DataFrame, overwrite: Boolean): Unit = {
    logInfo("trying to write")
  }

  override def buildScan(requiredColumns: Array[String], filters: Array[Filter]): RDD[Row] = {
    wipDataframe.rdd
  }

  override def unhandledFilters(filters: Array[Filter]): Array[Filter] = {
    filters.filterNot(GigaspacesRelation.canHandleFilter)
  }

}

private[insightedge] object GigaspacesRelation {
  def canHandleFilter(filter: Filter): Boolean = {
    filter match {
      case _: EqualTo => true
      case _: EqualNullSafe => true
      case _: GreaterThan => true
      case _: GreaterThanOrEqual => true
      case _: LessThan => true
      case _: LessThanOrEqual => true
      case _: In => true
      case _: IsNull => true
      case _: IsNotNull => true
      case f: And => canHandleFilter(f.left) && canHandleFilter(f.right)
      case f: Or => canHandleFilter(f.left) && canHandleFilter(f.right)
      case f: Not => false
      case _: StringStartsWith => false
      case _: StringEndsWith => false
      case _: StringContains => false
      case other => false
    }
  }

  def filtersToSql(filters: Array[Filter]): (String, Seq[Any]) = {
    val parameters = ListBuffer.empty[Any]
    val builder = filters
      .filter(canHandleFilter)
      .foldLeft(new StringBuilder) { (builder, filter) => builder -> (if (builder.isEmpty) "(" else " and (") ->(filter, parameters) -> ")" }
    (builder.toString(), parameters.toSeq)
  }

  def appendFilter(filter: Filter, builder: StringBuilder, params: ListBuffer[Any]): Unit = {
    filter match {
      case f: EqualTo =>
        builder -> f.attribute -> " = ?"
        params += f.value

      case f: EqualNullSafe =>
        builder -> f.attribute -> " = ?"
        params += f.value

      case f: GreaterThan =>
        builder -> f.attribute -> " > ?"
        params += f.value

      case f: GreaterThanOrEqual =>
        builder -> f.attribute -> " >= ?"
        params += f.value

      case f: LessThan =>
        builder -> f.attribute -> " < ?"
        params += f.value

      case f: LessThanOrEqual =>
        builder -> f.attribute -> " <= ?"
        params += f.value

      case f: In =>
        builder -> f.attribute
        f.values.map(value => "?").addString(builder, " in (", ",", ")")
        params ++= f.values

      case f: IsNull => true
        builder -> f.attribute -> " is null"

      case f: IsNotNull => true
        builder -> f.attribute -> " is not null"

      case f: And =>
        builder -> "(" ->(f.left, params) -> ") and (" ->(f.right, params) -> ")"

      case f: Or =>
        builder -> "(" ->(f.left, params) -> ") or (" ->(f.right, params) -> ")"
    }
  }

  implicit class BuilderExtension(val builder: StringBuilder) {
    def ->(any: Any): StringBuilder = {
      builder.append(any)
    }

    def ->(filter: Filter, params: ListBuffer[Any]): StringBuilder = {
      GigaspacesRelation.appendFilter(filter, builder, params)
      builder
    }
  }

}