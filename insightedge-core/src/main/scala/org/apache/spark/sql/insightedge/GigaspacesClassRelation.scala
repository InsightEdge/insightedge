package org.apache.spark.sql.insightedge

import com.gigaspaces.spark.model.GridModel
import com.gigaspaces.spark.rdd.GigaSpacesClassDataFrameRDD
import org.apache.spark.rdd.RDD
import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.{JavaTypeInference, ScalaReflection}
import org.apache.spark.sql.types._

import scala.reflect.ClassTag
import scala.reflect.runtime.universe
import scala.util.Try

private[insightedge] case class GigaspacesClassRelation(
                                                         context: SQLContext,
                                                         clazz: ClassTag[GridModel],
                                                         options: InsightEdgeSourceOptions
                                                       )
  extends GigaspacesAbstractRelation(context, options) with Serializable {

  /** Language used to define Space Class **/
  sealed trait ClassDefLang
  case object Scala extends ClassDefLang
  case object Java extends ClassDefLang

  lazy val (structType: StructType, classDefLanguage: ClassDefLang) = {
    // we don't know if space class declared in Scala or Java. So we just try both. There might be a better way to infer that.
    Try {
      // try with Scala reflection
      val reflectionType = universe.runtimeMirror(this.getClass.getClassLoader).classSymbol(clazz.runtimeClass).toType
      val structType = ScalaReflection.schemaFor(reflectionType).dataType.asInstanceOf[StructType]
      (structType, Scala)
    } getOrElse {
      // fallback to Java
      val (dataType, _) = JavaTypeInference.inferDataType(clazz.runtimeClass)
      (dataType.asInstanceOf[StructType], Java)
    }
  }

  override def buildSchema(): StructType = structType

  override def insert(data: DataFrame, overwrite: Boolean): Unit = throw new UnsupportedOperationException("saving classes is unsupported")

  override def insert(data: DataFrame, mode: SaveMode): Unit = throw new UnsupportedOperationException("saving classes is unsupported")

  override def buildScan(query: String, params: Seq[Any], fields: Seq[String]): RDD[Row] = {
    def converter(element: Any): Row = {
      Row.fromSeq(fields.map(getValueByName(element, _)))
    }
    new GigaSpacesClassDataFrameRDD(gsConfig, sc, query, params, fields, converter, options.readBufferSize)(clazz)
  }

  private def getValueByName[R](element: R, fieldName: String): AnyRef = {
    val methodName = classDefLanguage match {
      case Scala => fieldName
      case Java => "get" + fieldName.capitalize
    }
    element.getClass.getMethod(methodName).invoke(element)
  }

}
