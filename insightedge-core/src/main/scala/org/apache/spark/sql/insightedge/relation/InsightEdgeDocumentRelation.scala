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

import com.gigaspaces.document.SpaceDocument
import com.gigaspaces.metadata.{SpacePropertyDescriptor, SpaceTypeDescriptorBuilder}
import com.gigaspaces.query.IdQuery
import com.j_spaces.core.client.SQLQuery
import javax.activation.UnsupportedDataTypeException
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SaveMode._
import org.apache.spark.sql._
import org.apache.spark.sql.insightedge.{DataFrameSchema, InsightEdgeSourceOptions}
import org.apache.spark.sql.types._
import org.insightedge.spark.implicits.basic._
import org.insightedge.spark.rdd.InsightEdgeDocumentRDD

private[insightedge] case class InsightEdgeDocumentRelation(
                                                            context: SQLContext,
                                                            collection: String,
                                                            options: InsightEdgeSourceOptions
                                                          )
  extends InsightEdgeAbstractRelation(context, options) with Serializable {

  lazy val inferredSchema: StructType = {
    gs.read[DataFrameSchema](new IdQuery(classOf[DataFrameSchema], collection)) match {
      case null => getStructType(collection)
      case storedSchema => storedSchema.schema
    }
  }

  private def getStructType(collection : String): StructType = {
    val typeDescriptor = gs.getTypeManager.getTypeDescriptor(collection)
    if (typeDescriptor == null) { throw new IllegalArgumentException("Couldn't find a collection in memory")}

    val properties = typeDescriptor.getPropertiesNames
    var structType = new StructType()

    for (property <- properties) {
      val propertyDescriptor: SpacePropertyDescriptor = typeDescriptor.getFixedProperty(property)
      val schemaInference = SchemaInference.schemaFor(propertyDescriptor.getType)
      structType = structType.add(propertyDescriptor.getName, schemaInference.dataType, schemaInference.nullable)
    }
    structType
  }

  override def insert(data: DataFrame, overwrite: Boolean): Unit = {
    if (overwrite && !collectionIsEmpty) {
      gs.clear(new SpaceDocument(collection))
    }

    if (gs.getTypeManager.getTypeDescriptor(collection) == null) {
      gs.getTypeManager.registerTypeDescriptor(new SpaceTypeDescriptorBuilder(collection).supportsDynamicProperties(true).create())
    }

    data.rdd.mapPartitions { rows =>
      InsightEdgeAbstractRelation.rowsToDocuments(rows, schema).map(document => new SpaceDocument(collection, document))
    }.saveToGrid()

    def removeMetadata(s: StructType): StructType = {
      StructType(s.fields.map { f =>
        f.copy(metadata = Metadata.empty, dataType = f.dataType match {
          case dt: StructType => removeMetadata(dt)
          case dt => dt
        })
      })
    }

    val metalessSchema = removeMetadata(schema)
    gs.write(new DataFrameSchema(collection, metalessSchema))
  }

  override def insert(data: DataFrame, mode: SaveMode): Unit = {
    mode match {
      case Append =>
        insert(data, overwrite = false)

      case Overwrite =>
        insert(data, overwrite = true)

      case ErrorIfExists =>
        if (collectionIsEmpty) {
          insert(data, overwrite = false)
        } else {
          throw new IllegalStateException(
            s"""SaveMode is set to ErrorIfExists and collection "$collection" already exists and contains data.
                |Perhaps you meant to set the DataFrame write mode to Append?
                |Example: df.write.mode(SaveMode.Append).grid("$collection")""".stripMargin)
        }

      case Ignore =>
        if (collectionIsEmpty) {
          insert(data, overwrite = false)
        }
    }
  }

  private def collectionIsEmpty: Boolean = {
    if (gs.getTypeManager.getTypeDescriptor(collection) == null) {
      true
    } else {
      val query = new SQLQuery[SpaceDocument](collection, "", Seq()).setProjections("")
      gs.read(query) == null
    }
  }

  override def buildScan(query: String, params: Seq[Any], fields: Seq[String]): RDD[Row] = {
    val clazzName = classOf[SpaceDocument].getName

    val rdd = new InsightEdgeDocumentRDD(ieConfig, sc, collection, query, params, fields.toSeq, options.readBufferSize)

    rdd.mapPartitions { data => InsightEdgeAbstractRelation.beansToRows(data, clazzName, schema, fields) }
  }

}
