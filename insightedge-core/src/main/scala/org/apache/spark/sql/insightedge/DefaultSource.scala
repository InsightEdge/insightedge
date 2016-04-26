package org.apache.spark.sql.insightedge

import com.gigaspaces.spark.model.GridModel
import org.apache.spark.Logging
import org.apache.spark.sql.insightedge.DefaultSource._
import org.apache.spark.sql.sources._
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, SQLContext, SaveMode}

import scala.reflect._

class DefaultSource
  extends RelationProvider
    with SchemaRelationProvider
    with CreatableRelationProvider
    with Logging {

  override def createRelation(sqlContext: SQLContext, parameters: Predef.Map[String, String]): BaseRelation = {
    buildRelation(sqlContext, parameters)
  }

  override def createRelation(sqlContext: SQLContext, parameters: Predef.Map[String, String], schema: StructType): BaseRelation = {
    buildRelation(sqlContext, parameters, schema = Some(schema))
  }

  /**
    * This actually must save given df to the source and create relation on top of saved data
    */
  override def createRelation(sqlContext: SQLContext, mode: SaveMode, parameters: Predef.Map[String, String], data: DataFrame): BaseRelation = {
    val relation = buildRelation(sqlContext, parameters, Some(data.schema))
    relation.insert(data, mode)
    relation
  }

  private def buildRelation(sqlContext: SQLContext,
                            parameters: Predef.Map[String, String],
                            schema: Option[StructType] = None
                           ): GigaspacesAbstractRelation = {
    val readBufferSize = parameters.get(DefaultSource.InsightEdgeReadBufferSizeProperty).map(v => v.toInt).getOrElse(InsightEdgeReadBufferSizeDefault)
    val options = InsightEdgeSourceOptions(readBufferSize, schema)

    if (parameters.contains(InsightEdgeClassProperty)) {
      val tag = loadClass(parameters(InsightEdgeClassProperty))
      if (!classOf[GridModel].isAssignableFrom(tag.runtimeClass)) {
        throw new IllegalArgumentException(s"'class' must extend ${classOf[GridModel].getName}")
      }
      new GigaspacesClassRelation(sqlContext, tag.asInstanceOf[ClassTag[GridModel]], options)
    } else if (parameters.contains(InsightEdgeCollectionProperty) || parameters.contains("path")) {
      val collection = parameters.getOrElse(InsightEdgeClassProperty, parameters("path"))
      new GigaspacesDocumentRelation(sqlContext, collection, options)

    } else {
      throw new IllegalArgumentException("'path', 'collection' or 'class' must be specified")
    }
  }

  private def loadClass(path: String): ClassTag[Any] = {
    loadClass(Thread.currentThread().getContextClassLoader, path)
      .orElse(loadClass(this.getClass.getClassLoader, path))
      .getOrElse {
        throw new ClassNotFoundException(path)
      }
  }

  private def loadClass(classLoader: ClassLoader, path: String): Option[ClassTag[Any]] = {
    if (classLoader == null) {
      None
    }

    try {
      Some(ClassTag[Any](classLoader.loadClass(path)))
    } catch {
      case up: ClassNotFoundException => None
    }
  }

}

case class InsightEdgeSourceOptions(
                                     readBufferSize: Int,
                                     schema: Option[StructType]
                                   )

object DefaultSource {
  val InsightEdgeClassProperty = "class"
  val InsightEdgeCollectionProperty = "collection"
  val InsightEdgeReadBufferSizeProperty = "readBufferSize"
  val InsightEdgeReadBufferSizeDefault = 1000
}