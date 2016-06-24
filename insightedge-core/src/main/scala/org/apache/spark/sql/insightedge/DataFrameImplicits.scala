package org.apache.spark.sql.insightedge

import com.gigaspaces.spark.model.GridModel
import org.apache.spark.sql.{DataFrameWriter, DataFrame, DataFrameReader}

import scala.reflect._

/**
  * @author Danylo_Hurin.
  */
trait DataFrameImplicits {

  val GigaSpacesFormat = "org.apache.spark.sql.insightedge"

  def gridOptions(): Map[String, String] = Map()

  implicit class DataFrameReaderWrapper(val reader: DataFrameReader) {
    def grid = {
      reader.format(GigaSpacesFormat)
    }

    def loadClass[R <: GridModel : ClassTag]: DataFrame = {
      reader.format(GigaSpacesFormat).option("class", classTag[R].runtimeClass.getName).load()
    }
  }

  implicit class DataFrameWriterWrapper(val writer: DataFrameWriter) {
    def grid(collection:String) = {
      writer.format(GigaSpacesFormat).option("collection", collection)
    }

    def grid = {
      writer.format(GigaSpacesFormat)
    }
  }

}

