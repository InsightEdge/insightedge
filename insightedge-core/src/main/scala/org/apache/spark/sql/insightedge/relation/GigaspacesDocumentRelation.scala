package org.apache.spark.sql.insightedge.relation

import com.gigaspaces.document.SpaceDocument
import com.gigaspaces.metadata.SpaceTypeDescriptorBuilder
import com.gigaspaces.query.IdQuery
import com.gigaspaces.spark.implicits.basic._
import com.gigaspaces.spark.rdd.GigaSpacesDocumentRDD
import com.j_spaces.core.client.SQLQuery
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SaveMode._
import org.apache.spark.sql._
import org.apache.spark.sql.insightedge.{DataFrameSchema, InsightEdgeSourceOptions}
import org.apache.spark.sql.types._

import scala.collection.JavaConversions._

private[insightedge] case class GigaspacesDocumentRelation(
                                                            context: SQLContext,
                                                            collection: String,
                                                            options: InsightEdgeSourceOptions
                                                          )
  extends GigaspacesAbstractRelation(context, options) with Serializable {

  lazy val inferedSchema: StructType = {
    gs.read[DataFrameSchema](new IdQuery(classOf[DataFrameSchema], collection)) match {
      case null => new StructType()
      case storedSchema => storedSchema.schema
    }
  }

  override def insert(data: DataFrame, overwrite: Boolean): Unit = {
    if (overwrite && !collectionIsEmpty) {
      gs.takeMultiple(new SQLQuery[SpaceDocument](collection, "", Seq()).setProjections(""))
    }

    if (gs.getTypeManager.getTypeDescriptor(collection) == null) {
      gs.getTypeManager.registerTypeDescriptor(new SpaceTypeDescriptorBuilder(collection).supportsDynamicProperties(true).create())
    }

    data.rdd.mapPartitions { rows =>
      GigaspacesAbstractRelation.rowsToDocuments(rows, schema).map(document => new SpaceDocument(collection, document))
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
                |Example: df.write.grid.mode(SaveMode.Append).save("$collection")""".stripMargin)
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

    val rdd = new GigaSpacesDocumentRDD(gsConfig, sc, collection, query, params, fields.toSeq, options.readBufferSize)

    rdd.mapPartitions { data => GigaspacesAbstractRelation.beansToRows(data, clazzName, schema, fields) }
  }

}
