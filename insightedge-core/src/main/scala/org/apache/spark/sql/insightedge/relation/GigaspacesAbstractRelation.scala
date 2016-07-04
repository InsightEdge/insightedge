package org.apache.spark.sql.insightedge.relation

import java.beans.Introspector

import com.gigaspaces.spark.context.GigaSpacesConfig
import com.gigaspaces.spark.implicits.basic._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.insightedge.InsightEdgeSourceOptions
import org.apache.spark.sql.insightedge.filter.{GeoContains, GeoIntersects, GeoWithin}
import org.apache.spark.sql.insightedge.relation.GigaspacesAbstractRelation._
import org.apache.spark.sql.insightedge.udt._
import org.apache.spark.sql.sources._
import org.apache.spark.sql.types.{DataType, StructField, StructType, UserDefinedType}
import org.apache.spark.sql.{DataFrame, Row, SQLContext, SaveMode}
import org.apache.spark.{Logging, SparkContext}
import org.openspaces.core.GigaSpace
import org.openspaces.spatial.shapes._

import scala.collection.mutable.ListBuffer

abstract class GigaspacesAbstractRelation(
                                           override val sqlContext: SQLContext,
                                           options: InsightEdgeSourceOptions
                                         )
  extends BaseRelation
    with InsertableRelation
    with GridPrunedFilteredScan
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
      case _: GeoIntersects => true
      case _: GeoWithin => true
      case _: GeoContains => true
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

      case f: GeoIntersects =>
        builder -> f.attribute -> " spatial:intersects ?"
        params += f.value

      case f: GeoContains =>
        builder -> f.attribute -> " spatial:contains ?"
        params += f.value

      case f: GeoWithin =>
        builder -> f.attribute -> " spatial:within ?"
        params += f.value
    }
  }

  def udtFor(clazz: Class[_]): Option[UserDefinedType[_]] = {
    clazz match {
      case c if classOf[Point].isAssignableFrom(c) => Some(new PointUDT())
      case c if classOf[Circle].isAssignableFrom(c) => Some(new CircleUDT())
      case c if classOf[Rectangle].isAssignableFrom(c) => Some(new RectangleUDT())
      case c if classOf[Polygon].isAssignableFrom(c) => Some(new PolygonUDT())
      case c if classOf[LineString].isAssignableFrom(c) => Some(new LineStringUDT())
      case _ => None
    }
  }

  def enhanceWithUdts(dataType: DataType, clazz: Class[_]): DataType = {
    udtFor(clazz) match {
      case Some(udt) =>
        udt

      case None =>
        dataType match {
          case struct: StructType =>
            // Product bean info does not have any fields, for some reason
            val fieldTypeMap: Map[String, Class[_]] = clazz match {
              case c if classOf[Product].isAssignableFrom(c) =>
                clazz.getDeclaredFields.map(f => f.getName -> f.getType).toMap
              case _ =>
                val beanInfo = Introspector.getBeanInfo(clazz)
                beanInfo.getPropertyDescriptors.map(d => d.getName -> d.getPropertyType).toMap
            }

            StructType(
              struct.fields.map(f => {
                val maybeNewType = enhanceWithUdts(f.dataType, fieldTypeMap(f.name))
                StructField(f.name, maybeNewType, f.nullable, f.metadata)
              })
            )
          case other => other
        }
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

/**
  * Used to apply grid filters with custom strategy
  */
trait GridPrunedFilteredScan extends PrunedFilteredScan {}
