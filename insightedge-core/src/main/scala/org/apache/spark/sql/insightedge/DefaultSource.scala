package org.apache.spark.sql.insightedge

import org.apache.spark.internal.Logging
import org.apache.spark.sql.insightedge.DefaultSource._
import org.apache.spark.sql.insightedge.relation.{InsightEdgeAbstractRelation, InsightEdgeClassRelation, InsightEdgeDocumentRelation}
import org.apache.spark.sql.insightedge.udt.GeoUDTRegistration
import org.apache.spark.sql.sources._
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, SQLContext, SaveMode}
import org.apache.spark.util.Utils
import org.insightedge.spark.utils.InsightEdgeConstants._

import scala.reflect._

class DefaultSource
  extends RelationProvider
    with SchemaRelationProvider
    with CreatableRelationProvider
    with Logging {

  override def createRelation(sqlContext: SQLContext, parameters: Map[String, String]): BaseRelation = {
    buildRelation(sqlContext, parameters)
  }

  override def createRelation(sqlContext: SQLContext, parameters: Map[String, String], schema: StructType): BaseRelation = {
    buildRelation(sqlContext, parameters, schema = Some(schema))
  }

  /**
    * This actually must save given df to the source and create relation on top of saved data
    */
  override def createRelation(sqlContext: SQLContext, mode: SaveMode, parameters: Map[String, String], data: DataFrame): BaseRelation = {
    val relation = buildRelation(sqlContext, parameters, Some(data.schema))
    relation.insert(data, mode)
    relation
  }

  private def buildRelation(sqlContext: SQLContext,
                            parameters: Map[String, String],
                            schema: Option[StructType] = None
                           ): InsightEdgeAbstractRelation = synchronized {

    GeoUDTRegistration.registerIfNotAlready()

    val readBufferSize = parameters.get(InsightEdgeReadBufferSizeProperty).map(_.toInt).getOrElse(DefaultReadBufferSize)
    val splitCount = parameters.get(InsightEdgeSplitCountProperty).map(_.toInt)
    val options = InsightEdgeSourceOptions(splitCount, readBufferSize, schema)

    if (parameters.contains(InsightEdgeClassProperty)) {
      val tag = loadClass(parameters(InsightEdgeClassProperty)).asInstanceOf[ClassTag[AnyRef]]
      InsightEdgeClassRelation(sqlContext, tag, options)
    } else if (parameters.contains(InsightEdgeCollectionProperty) || parameters.contains("path")) {
      val collection = parameters.getOrElse(InsightEdgeCollectionProperty, parameters("path"))
      InsightEdgeDocumentRelation(sqlContext, collection, options)

    } else {
      throw new IllegalArgumentException("'path', 'collection' or 'class' must be specified")
    }
  }

  private def loadClass(path: String): ClassTag[Any] = {
    ClassTag[Any](Utils.classForName(path))
  }

}

case class InsightEdgeSourceOptions(
                                     splitCount: Option[Int],
                                     readBufferSize: Int,
                                     schema: Option[StructType]
                                   )

object DefaultSource {
  val InsightEdgeClassProperty = "class"
  val InsightEdgeCollectionProperty = "collection"
  val InsightEdgeReadBufferSizeProperty = "readBufferSize"
  val InsightEdgeSplitCountProperty = "splitCount"
}