package org.apache.spark.sql.insightedge

import org.apache.spark.Logging
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
    buildRelation(sqlContext, parameters, mode = mode, data = Some(data))
  }

  private def buildRelation(sqlContext: SQLContext,
                            parameters: Predef.Map[String, String],
                            mode: SaveMode = SaveMode.Append,
                            data: Option[DataFrame] = None,
                            schema: Option[StructType] = None
                           ): BaseRelation = {
    if (parameters.contains("class")) {
      val tag = ClassTag[AnyRef](this.getClass.getClassLoader.loadClass(parameters("class")))
      new GigaspacesRelation(sqlContext, Some(tag), None)
    } else if (parameters.contains("collection")) {
      new GigaspacesRelation(sqlContext, None, Some(parameters("collection")))
    } else {
      throw new Exception("'class' or 'collection' must be specified")
    }
  }

}
