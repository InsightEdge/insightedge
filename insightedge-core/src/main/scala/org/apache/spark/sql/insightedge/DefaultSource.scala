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

package org.apache.spark.sql.insightedge

import org.apache.spark.internal.Logging
import org.apache.spark.sql._
import org.apache.spark.sql.execution.streaming.Sink
import org.apache.spark.sql.insightedge.DefaultSource._
import org.apache.spark.sql.insightedge.relation.{InsightEdgeAbstractRelation, InsightEdgeClassRelation, InsightEdgeDocumentRelation}
import org.apache.spark.sql.insightedge.udt.GeoUDTRegistration
import org.apache.spark.sql.sources._
import org.apache.spark.sql.streaming.OutputMode
import org.apache.spark.sql.types.StructType
import org.apache.spark.util.Utils
import org.insightedge.spark.utils.InsightEdgeConstants._

import scala.reflect._

class DefaultSource
  extends RelationProvider
    with SchemaRelationProvider
    with CreatableRelationProvider
    with StreamSinkProvider
    with DataSourceRegister
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

  def createSink(
                  sqlContext: SQLContext,
                  parameters: Map[String, String],
                  partitionColumns: Seq[String],
                  outputMode: OutputMode): Sink = {
    new MySink(sqlContext, parameters, partitionColumns, outputMode)
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

  override def shortName(): String = "MyIeSink"
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

class MySink(sqlContext: SQLContext,
             parameters: Map[String, String],
             partitionColumns: Seq[String],
             outputMode: OutputMode) extends Sink with Logging {
  override def addBatch(batchId: Long, data: DataFrame): Unit = {
    println("---- MySink ---")
    println("---------- sqlContext -------")
    println(sqlContext)
    println("----------")

    println("---------- parameters -------")
    println(parameters)
    println("----------")

    println("---------- partitionColumns -------")
    println(partitionColumns)
    println("----------")

    println("---------- outputMode -------")
    println(outputMode)
    println("----------")

    println("---------- data -------")
    println(data)


    println("printSchema:")
    data.printSchema()
    println("END - printSchema:")
    println("show: ")
    data.show()
    println("----------")
    println("##########")
    val collection = data.collect()
    println("collection: " + collection)
    println("new dataframe:")
    data.sparkSession.createDataFrame(data.sparkSession.sparkContext.parallelize(collection), data.schema)
      .show()
    println("##########")

  }
}