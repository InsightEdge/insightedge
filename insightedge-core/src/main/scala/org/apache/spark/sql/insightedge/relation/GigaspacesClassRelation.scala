package org.apache.spark.sql.insightedge.relation

import java.beans.Introspector

import com.gigaspaces.spark.model.GridModel
import com.gigaspaces.spark.rdd.GigaSpacesSqlRDD
import org.apache.spark.rdd.RDD
import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.expressions.AttributeReference
import org.apache.spark.sql.insightedge.InsightEdgeSourceOptions
import org.apache.spark.sql.types._
import org.apache.spark.util.Utils

import scala.reflect.ClassTag

private[insightedge] case class GigaspacesClassRelation(
                                                         context: SQLContext,
                                                         clazz: ClassTag[GridModel],
                                                         options: InsightEdgeSourceOptions
                                                       )
  extends GigaspacesAbstractRelation(context, options) with Serializable {

  lazy val structType: StructType = {
    val schema = SchemaInference.schemaFor(clazz.runtimeClass, (c: Class[_]) => GigaspacesAbstractRelation.udtFor(c))
    schema.dataType.asInstanceOf[StructType]
  }

  override def buildSchema(): StructType = structType

  override def insert(data: DataFrame, overwrite: Boolean): Unit = throw new UnsupportedOperationException("saving classes is unsupported")

  override def insert(data: DataFrame, mode: SaveMode): Unit = throw new UnsupportedOperationException("saving classes is unsupported")

  override def buildScan(query: String, params: Seq[Any], fields: Seq[String]): RDD[Row] = {
    val clazzName = clazz.runtimeClass.getName

    val rdd = new GigaSpacesSqlRDD(gsConfig, sc, query, params, fields, options.readBufferSize)(clazz)

    rdd.mapPartitions { data => beansToRows(data, clazzName, schema, fields) }
  }

  /**
    * Converts an iterator of Beans to Row using the provided bean info & schema.
    */
  private def beansToRows(data: Iterator[_], clazzName: String, schema: StructType, fields: Seq[String]): Iterator[Row] = {
    val converter = beanToRowConverter(clazzName, schema, fields)
    data.map { element => converter(element) }
  }

  /**
    * Returns a converter that converts any bean with given schema to the Row.
    *
    * Recursive for embedded properties (StructType).
    */
  private def beanToRowConverter(clazzName: String, schema: StructType, fields: Seq[String]): (Any => Row) = {
    // BeanInfo is not serializable so we must rediscover it remotely for each partition.
    val clazz = Utils.classForName(clazzName)
    val beanInfo = Introspector.getBeanInfo(clazz)

    val attributeNames = if (fields.isEmpty) schema.fields.map(f => f.name).toSeq else fields
    val schemaFieldsMap = schema.fields.map(f => (f.name, f)).toMap
    val attributeRefs = attributeNames
      .map { f => schemaFieldsMap(f) }
      .map { f => AttributeReference(f.name, f.dataType, f.nullable)() }

    // Getter methods for Product (case classes) have same names as attributes
    val gettersMap = clazz match {
      case c if classOf[Product].isAssignableFrom(clazz) =>
        attributeNames
          .map(a => a -> c.getMethod(a)).toMap
      case _ =>
        beanInfo.getPropertyDescriptors
          .filter { f => attributeNames.contains(f.getName) }
          .map(f => f.getName -> f.getReadMethod).toMap
    }

    val extractors = attributeRefs
      .map(attribute => attribute.dataType match {
        case dataType: StructType =>
          val getter = gettersMap(attribute.name)
          val converter = beanToRowConverter(getter.getReturnType.getName, dataType, Seq.empty[String])
          element: Any => converter(getter.invoke(element))
        case _ =>
          val getter = gettersMap(attribute.name)
          element: Any => getter.invoke(element)
      })

    (element: Any) => if (element == null) null else Row.fromSeq(extractors.map { extractor => extractor(element) })
  }

}

