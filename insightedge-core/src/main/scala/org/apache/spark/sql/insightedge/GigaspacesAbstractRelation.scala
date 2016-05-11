package org.apache.spark.sql.insightedge

import com.gigaspaces.spark.context.GigaSpacesConfig
import com.gigaspaces.spark.implicits.basic._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.insightedge.GigaspacesAbstractRelation.{filtersToSql, unsupportedFilters}
import org.apache.spark.sql.sources._
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, Row, SQLContext, SaveMode}
import org.apache.spark.{Logging, SparkContext}
import org.openspaces.core.GigaSpace

import scala.collection.mutable.ListBuffer

abstract class GigaspacesAbstractRelation(
                                           override val sqlContext: SQLContext,
                                           options: InsightEdgeSourceOptions
                                         )
  extends BaseRelation
    with InsertableRelation
    with PrunedFilteredScan
    with Logging
    with Serializable {

  protected def sc: SparkContext = sqlContext.sparkContext

  protected def gs: GigaSpace = sc.gigaSpace

  protected def gsConfig: GigaSpacesConfig = GigaSpacesConfig.fromSparkConf(sc.getConf)

  override def schema: StructType = {
    if (options.schema.nonEmpty) {
      options.schema.get
    } else {
      buildSchema()
    }
  }

  protected def buildSchema(): StructType

  override def buildScan(requiredColumns: Array[String], filters: Array[Filter]): RDD[Row] = {
    val fields = if (requiredColumns.nonEmpty) requiredColumns else schema.fieldNames
    val (query, params) = filtersToSql(filters)
    buildScan(query, params, fields.toSeq)
  }

  protected def buildScan(query: String, params: Seq[Any], fields: Seq[String]): RDD[Row]

  override def unhandledFilters(filters: Array[Filter]): Array[Filter] = unsupportedFilters(filters)

  def insert(data: DataFrame, mode: SaveMode): Unit

}


object GigaspacesAbstractRelation {
  def unsupportedFilters(filters: Array[Filter]): Array[Filter] = {
    filters.filterNot(GigaspacesAbstractRelation.canHandleFilter)
  }

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

      case f: IsNull =>
        builder -> f.attribute -> " is null"

      case f: IsNotNull =>
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
      appendFilter(filter, builder, params)
      builder
    }
  }

}
