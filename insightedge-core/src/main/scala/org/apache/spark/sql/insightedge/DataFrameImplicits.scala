package org.apache.spark.sql.insightedge

import org.apache.spark.sql.insightedge.relation.{GigaspacesAbstractRelation, SchemaInference}
import org.apache.spark.sql.types.{Metadata, MetadataBuilder, StructType}
import org.apache.spark.sql.{DataFrame, DataFrameReader, DataFrameWriter}

import scala.reflect._

/**
  * @author Danylo_Hurin.
  */
trait DataFrameImplicits {

  val GigaSpacesFormat = "org.apache.spark.sql.insightedge"

  def gridOptions(): Map[String, String] = Map()

  def nestedClass[R: ClassTag]: Metadata = {
    nestedClassName(classTag[R].runtimeClass.getName)
  }

  def nestedClassName(clazz: String): Metadata = {
    new MetadataBuilder().putString("class", clazz).build()
  }

  implicit class DataFrameReaderWrapper(val reader: DataFrameReader) {
    def grid = {
      reader.format(GigaSpacesFormat)
    }

    def loadClass[R: ClassTag]: DataFrame = {
      reader.format(GigaSpacesFormat).option("class", classTag[R].runtimeClass.getName).load()
    }
  }

  implicit class DataFrameWriterWrapper(val writer: DataFrameWriter) {
    def grid(collection: String) = {
      writer.format(GigaSpacesFormat).option("collection", collection)
    }

    def grid = {
      writer.format(GigaSpacesFormat)
    }
  }

}
