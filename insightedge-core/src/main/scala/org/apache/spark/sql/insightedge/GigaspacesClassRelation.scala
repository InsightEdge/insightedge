package org.apache.spark.sql.insightedge

import com.gigaspaces.spark.rdd.GigaSpacesClassDataFrameRDD
import org.apache.spark.rdd.RDD
import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.ScalaReflection
import org.apache.spark.sql.types._

import scala.reflect._
import scala.reflect.runtime.universe._

private[insightedge] case class GigaspacesClassRelation(
                                                         context: SQLContext,
                                                         clazz: ClassTag[Any],
                                                         options: InsightEdgeSourceOptions
                                                       )
  extends GigaspacesAbstractRelation(context, options) with Serializable {

  override def buildSchema(): StructType = {
    val reflectionType = runtimeMirror(this.getClass.getClassLoader).classSymbol(clazz.runtimeClass).toType
    ScalaReflection.schemaFor(reflectionType).dataType.asInstanceOf[StructType]
  }

  override def insert(data: DataFrame, overwrite: Boolean): Unit = throw new UnsupportedOperationException("saving classes is unsupported")

  override def insert(data: DataFrame, mode: SaveMode): Unit = throw new UnsupportedOperationException("saving classes is unsupported")

  override def buildScan(query: String, params: Seq[Any], fields: Seq[String]): RDD[Row] = {
    def converter(element: Any): Row = {
      Row.fromSeq(fields.map(getValueByName(element, _)))
    }
    new GigaSpacesClassDataFrameRDD(gsConfig, sc, query, params, fields, converter, options.readBufferSize)(clazz)
  }

  private def getValueByName[R](element: R, fieldName: String): AnyRef = {
    element.getClass.getMethod(fieldName).invoke(element)
  }

}
