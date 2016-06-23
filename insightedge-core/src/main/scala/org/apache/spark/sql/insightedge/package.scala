package org.apache.spark.sql

import com.gigaspaces.spark.model.GridModel
import org.apache.spark.sql.catalyst.expressions.Literal
import org.apache.spark.sql.functions._
import org.apache.spark.sql.insightedge.expression.{GeoIntersects, SubtypeOf}
import org.apache.spark.sql.types.{DataType, ObjectType}
import org.openspaces.spatial.shapes.Shape

import scala.reflect._

package object insightedge {

  val GigaSpacesFormat = "org.apache.spark.sql.insightedge"

  def gridOptions(): Map[String, String] = Map()

  implicit class DataFrameReaderWrapper(val reader: DataFrameReader) extends AnyVal {
    def grid = {
      reader.format(GigaSpacesFormat)
    }

    def loadClass[R <: GridModel : ClassTag]: DataFrame = {
      reader.format(GigaSpacesFormat).option("class", classTag[R].runtimeClass.getName).load()
    }
  }

  implicit class DataFrameWriterWrapper(val writer: DataFrameWriter) extends AnyVal {
    def grid(collection: String) = {
      writer.format(GigaSpacesFormat).option("collection", collection)
    }

    def grid = {
      writer.format(GigaSpacesFormat)
    }
  }

  implicit class ColumnWrapper(val column: Column) extends AnyVal {
    def subtypeOf(other: Column): Column = new Column(SubtypeOf(column.expr, lit(other).expr))

    def subtypeOf(clazz: Class[_]): Column = this.subtypeOf(typedLit(clazz, new ObjectType(classOf[Class[_]])))

    def geoIntersects(other: Column): Column = new Column(GeoIntersects(column.expr, lit(other).expr))

    def geoIntersects(shape: Shape): Column = this.geoIntersects(typedLit(shape, new ObjectType(classOf[Shape])))

    def typedLit(value: Any, dataType: DataType): Column = {
      value match {
        case c: Column => return c
        case s: Symbol => return new ColumnName(value.asInstanceOf[Symbol].name)
        case _ => // continue
      }

      val literalExpr = Literal.create(value, dataType)
      Column(literalExpr)
    }
  }

}