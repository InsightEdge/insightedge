package org.apache.spark.sql.insightedge

import org.apache.spark.sql.types.{Metadata, MetadataBuilder}
import org.apache.spark.sql.{DataFrame, DataFrameReader, DataFrameWriter}

import scala.reflect._

/**
  * @author Danylo_Hurin.
  */
trait DataFrameImplicits {

  val InsightEdgeFormat = "org.apache.spark.sql.insightedge"

  def gridOptions(): Map[String, String] = Map()

  def nestedClass[R: ClassTag]: Metadata = {
    nestedClassName(classTag[R].runtimeClass.getName)
  }

  def nestedClassName(clazz: String): Metadata = {
    new MetadataBuilder().putString("class", clazz).build()
  }

  implicit class DataFrameReaderWrapper(val reader: DataFrameReader) {
    def grid = {
      reader.format(InsightEdgeFormat)
    }

    def loadClass[R: ClassTag]: DataFrame = {
      reader.format(InsightEdgeFormat).option("class", classTag[R].runtimeClass.getName).load()
    }
  }

  implicit class DataFrameWriterWrapper(val writer: DataFrameWriter) {
    def grid(collection: String) = {
      writer.format(InsightEdgeFormat).option("collection", collection)
    }

    def grid = {
      writer.format(InsightEdgeFormat)
    }
  }

}
