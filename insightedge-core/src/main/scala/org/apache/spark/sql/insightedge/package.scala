package org.apache.spark.sql

//import org.apache.spark.sql.catalyst.expressions.Literal
//import org.apache.spark.sql.functions._
//import org.apache.spark.sql.insightedge.expression.{GeoContains, GeoIntersects, GeoWithin}
//import org.apache.spark.sql.types.{DataType, ObjectType}
//import org.openspaces.spatial.shapes.Shape

//package object insightedge extends DataFrameImplicits {

//  implicit class ColumnWrapper(val column: Column) extends AnyVal {
//    def geoIntersects(other: Column): Column = new Column(GeoIntersects(column.expr, lit(other).expr))
//
//    def geoIntersects(shape: Shape): Column = this.geoIntersects(typedLit(shape, new ObjectType(classOf[Shape])))
//
//    def geoContains(other: Column): Column = new Column(GeoContains(column.expr, lit(other).expr))
//
//    def geoContains(shape: Shape): Column = this.geoContains(typedLit(shape, new ObjectType(classOf[Shape])))
//
//    def geoWithin(other: Column): Column = new Column(GeoWithin(column.expr, lit(other).expr))
//
//    def geoWithin(shape: Shape): Column = this.geoWithin(typedLit(shape, new ObjectType(classOf[Shape])))
//
//    def typedLit(value: Any, dataType: DataType): Column = {
//      value match {
//        case c: Column => return c
//        case s: Symbol => return new ColumnName(value.asInstanceOf[Symbol].name)
//        case _ => // continue
//      }
//
//      val literalExpr = Literal.create(value, dataType)
//      Column(literalExpr)
//    }
//  }

//}
