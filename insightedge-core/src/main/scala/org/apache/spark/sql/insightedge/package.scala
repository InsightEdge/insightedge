package org.apache.spark.sql

import scala.reflect._

package object insightedge {

  val GigaSpacesFormat = "org.apache.spark.sql.insightedge"

  def gridOptions(): Map[String, String] = Map()

  implicit class DataFrameReaderWrapper(val reader: DataFrameReader) extends AnyVal {
    def grid[T: ClassTag]: DataFrameReader = {
      reader.format(GigaSpacesFormat).option("class", classTag[T].runtimeClass.getName)
    }
  }

  implicit class DataFrameWriterWrapper(val writer: DataFrameWriter) extends AnyVal {
  }

}