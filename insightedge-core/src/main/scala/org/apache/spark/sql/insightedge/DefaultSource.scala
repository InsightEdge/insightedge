package org.apache.spark.sql.insightedge

import org.apache.spark.Logging
import org.apache.spark.sql.insightedge.DefaultSource._
import org.apache.spark.sql.sources.{BaseRelation, CreatableRelationProvider, RelationProvider, SchemaRelationProvider}
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, SQLContext, SaveMode}

import scala.reflect.ClassTag

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

  override def createRelation(sqlContext: SQLContext, mode: SaveMode, parameters: Predef.Map[String, String], data: DataFrame): BaseRelation = {
    val relation = buildRelation(sqlContext, parameters, Some(data.schema))
    relation.save(data, mode)
    relation
  }

  private def buildRelation(sqlContext: SQLContext,
                            parameters: Predef.Map[String, String],
                            schema: Option[StructType] = None
                           ): GigaspacesRelation = {
    val readBufferSize = parameters.get(DefaultSource.InsightEdgeReadBufferSizeProperty).map(v => v.toInt).getOrElse(InsightEdgeReadBufferSizeDefault)
    val options = InsightEdgeSourceOptions(readBufferSize, schema)

    if (parameters.contains(InsightEdgeClassProperty)) {
      val tag = ClassTag[Any](this.getClass.getClassLoader.loadClass(parameters(InsightEdgeClassProperty)))
      new GigaspacesRelation(sqlContext, options.copy(clazz = Some(tag)))

    } else if (parameters.contains(InsightEdgeCollectionProperty) || parameters.contains("path")) {
      val collection = parameters.getOrElse(InsightEdgeClassProperty, parameters("path"))
      new GigaspacesRelation(sqlContext, options.copy(collection = Some(collection)))

    } else {
      throw new IllegalArgumentException("'path', 'collection' or 'class' must be specified")
    }
  }

}

case class InsightEdgeSourceOptions(
                                     readBufferSize: Int,
                                     schema: Option[StructType],
                                     clazz: Option[ClassTag[Any]] = None,
                                     collection: Option[String] = None
                                   )

object DefaultSource {
  val InsightEdgeClassProperty = "class"
  val InsightEdgeCollectionProperty = "collection"
  val InsightEdgeReadBufferSizeProperty = "readBufferSize"
  val InsightEdgeReadBufferSizeDefault = 1000
}