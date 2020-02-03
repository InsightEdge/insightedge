/*
 * Copyright (c) 2016, GigaSpaces Technologies, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.insightedge.relation

import java.beans.Introspector
import java.lang.reflect.Method

import com.gigaspaces.document.{DocumentProperties, SpaceDocument}
import org.apache.spark.SparkContext
import org.apache.spark.internal.Logging
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.catalyst.expressions.AttributeReference
import org.apache.spark.sql.insightedge.InsightEdgeSourceOptions
import org.apache.spark.sql.insightedge.filter.{GeoContains, GeoIntersects, GeoWithin}
import org.apache.spark.sql.insightedge.relation.InsightEdgeAbstractRelation._
import org.apache.spark.sql.insightedge.udt._
import org.apache.spark.sql.sources._
import org.apache.spark.sql.types.{StructType, UserDefinedType}
import org.apache.spark.sql.{DataFrame, Row, SQLContext, SaveMode}
import org.apache.spark.util.Utils
import org.insightedge.spark.context.InsightEdgeConfig
import org.insightedge.spark.implicits.basic._
import org.openspaces.core.GigaSpace
import org.openspaces.spatial.shapes._

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

abstract class InsightEdgeAbstractRelation(
                                           override val sqlContext: SQLContext,
                                           options: InsightEdgeSourceOptions
                                         )
  extends BaseRelation
    with InsertableRelation
    with GridPrunedFilteredScan
    with Logging
    with Serializable {

  protected def sc: SparkContext = sqlContext.sparkContext

  protected def gs: GigaSpace = sc.grid

  protected def ieConfig: InsightEdgeConfig = sc.ieConfig

  override def schema: StructType = {
    if (options.schema.nonEmpty) {
      options.schema.get
    } else {
      inferredSchema
    }
  }

  def inferredSchema: StructType

  override def buildScan(requiredColumns: Array[String], filters: Array[Filter]): RDD[Row] = {
    val fields = if (requiredColumns.nonEmpty) requiredColumns else schema.fieldNames
    val (query, params) = filtersToSql(filters)
    buildScan(query, params, fields.toSeq)
  }

  protected def buildScan(query: String, params: Seq[Any], fields: Seq[String]): RDD[Row]

  override def unhandledFilters(filters: Array[Filter]): Array[Filter] = unsupportedFilters(filters)

  def insert(data: DataFrame, mode: SaveMode): Unit
}


object InsightEdgeAbstractRelation {
  def unsupportedFilters(filters: Array[Filter]): Array[Filter] = {
    filters.filterNot(InsightEdgeAbstractRelation.canHandleFilter)
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

  implicit class BuilderExtension(val builder: StringBuilder) {
    def ->(any: Any): StringBuilder = {
      builder.append(any)
    }

    def ->(filter: Filter, params: ListBuffer[Any]): StringBuilder = {
      appendFilter(filter, builder, params)
      builder
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

  /**
    * Converts an iterator of Beans to Row using the provided class & schema.
    */
  def beansToRows(data: Iterator[_], clazzName: String, schema: StructType, fields: Seq[String]): Iterator[Row] = {
    val converter = buildBeanToRowConverter(clazzName, schema, fields)
    data.map { element => converter(element) }
  }

  /**
    * Returns a converter that converts any bean with given schema to the Row.
    *
    * Recursive for embedded properties (StructType).
    */
  def buildBeanToRowConverter(clazzName: String, schema: StructType, fields: Seq[String]): (Any => Row) = {
    val clazz = Utils.classForName(clazzName)

    val attributeNames = if (fields.isEmpty) schema.fields.map(f => f.name).toSeq else fields
    val schemaFieldsMap = schema.fields.map(f => (f.name, f)).toMap
    val attributeRefs = attributeNames
      .map { f => schemaFieldsMap(f) }
      .map { f => AttributeReference(f.name, f.dataType, f.nullable)() }

    val anyNestedClass = classOf[DocumentProperties].getName

    // Map of "elementName" -> (returnClassName, functionToExtract)
    val extractorsByClass = clazz match {

      // Getter methods for Product (case classes) have same names as attributes
      case c if classOf[Product].isAssignableFrom(clazz) =>
        attributeNames
          .map(a => a -> getterToClassAndExtractor(c.getMethod(a))).toMap

      // Getters for SpaceDocuments are document.getProperty[T](name), type is extracted from type descriptor
      case c if classOf[SpaceDocument].isAssignableFrom(clazz) =>
        attributeNames
          .map { a =>
            val meta = schemaFieldsMap(a).metadata
            val nestedClassName = if (meta.contains("class")) meta.getString("class") else anyNestedClass
            a ->(nestedClassName, (e: Any) => e.asInstanceOf[SpaceDocument].getProperty[Any](a))
          }.toMap

      // Getters for DocumentProperties are document.getProperty[T](name)
      case c if classOf[DocumentProperties].isAssignableFrom(clazz) =>
        attributeNames
          .map(a => a ->(anyNestedClass, (e: Any) => e.asInstanceOf[DocumentProperties].getProperty[Any](a))).toMap

      //extract name function from enum, ordinal not supported
      case c if clazz.isEnum =>
        attributeNames
                .map(a => a -> getterToClassAndExtractor(c.getMethod("name"))).toMap

      // Getters for Java classes are from bean info, which is not serializable so we must rediscover it remotely for each partition
      case _ =>
        val beanInfo = Introspector.getBeanInfo(clazz)
        beanInfo.getPropertyDescriptors
          .filter(f => attributeNames.contains(f.getName))
          .map(f => f.getName -> getterToClassAndExtractor(f.getReadMethod)).toMap
    }

    val converters = attributeRefs
      .map(attribute => attribute.dataType match {
        case dataType: StructType =>
          val (clazz, extractor) = extractorsByClass(attribute.name)
          val converter = buildBeanToRowConverter(clazz, dataType, Seq.empty[String])
          element: Any => converter(extractor(element))
        case _ =>
          val (_, extractor) = extractorsByClass(attribute.name)
          element: Any => extractor(element)
      })

    (element: Any) => if (element == null) null else Row.fromSeq(converters.map { converter => converter(element) })
  }

  def getterToClassAndExtractor(getter: Method): (String, Any => Any) = {
    (getter.getReturnType.getName, (e: Any) => getter.invoke(e))
  }

  /**
    * Converts an iterator of Rows to Document Properties using the provided schema.
    */
  def rowsToDocuments(data: Iterator[Row], schema: StructType): Iterator[DocumentProperties] = {
    val converter = buildRowToDocumentConverter(schema)
    data.map { element => converter(element) }
  }

  /**
    * Returns a converter that converts any row with given schema to the DocumentProperties.
    *
    * Recursive for embedded properties (StructType).
    */
  def buildRowToDocumentConverter(schema: StructType): (Row => DocumentProperties) = {
    // List of tuples (fieldName, converter)
    val converters = schema.fields
      .map(field => field.dataType match {
        case dataType: StructType =>
          val converter = buildRowToDocumentConverter(dataType)
          field.name -> ((row: Row) => converter(row.getAs[Row](field.name)))
        case _ =>
          field.name -> ((row: Row) => row.getAs[AnyRef](field.name))
      })

    (row: Row) => if (row == null) null else new DocumentProperties(converters.map { case (name, converter) => name -> converter(row) }.toMap)
  }

}

/**
  * Used to apply grid filters with custom strategy
  */
trait GridPrunedFilteredScan extends PrunedFilteredScan {}
