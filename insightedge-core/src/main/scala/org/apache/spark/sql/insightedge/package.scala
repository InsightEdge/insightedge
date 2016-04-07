package org.apache.spark.sql

import scala.reflect._

package object insightedge {

  val GigaSpacesFormat = "org.apache.spark.sql.insightedge"

  def gridOptions(): Map[String, String] = Map()

  implicit class DataFrameReaderWrapper(val reader: DataFrameReader) extends AnyVal {
    def grid[T: ClassTag]: DataFrameReader = {
      reader.format(GigaSpacesFormat).option("class", classTag[T].runtimeClass.getName)
    }

    def grid(collection:String) = {
      reader.format(GigaSpacesFormat).option("collection", collection)
    }
  }

  implicit class DataFrameWriterWrapper(val writer: DataFrameWriter) extends AnyVal {
    def grid[T: ClassTag]: DataFrameWriter = {
      writer.format(GigaSpacesFormat).option("class", classTag[T].runtimeClass.getName)
    }

    def grid(collection:String) = {
      writer.format(GigaSpacesFormat).option("collection", collection)
    }
  }

}