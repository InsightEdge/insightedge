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

import org.apache.spark.sql.catalyst.expressions.Literal
import org.apache.spark.sql.{ColumnName, Column}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.insightedge.expression.{GeoWithin, GeoContains, GeoIntersects}
import org.apache.spark.sql.types.{DataType, ObjectType}
import org.openspaces.spatial.shapes.Shape

/**
  * @author Danylo_Hurin.
  */
trait GeospatialImplicits {

  implicit class ColumnWrapper(val column: Column) {
    def geoIntersects(other: Column): Column = new Column(GeoIntersects(column.expr, lit(other).expr))

    def geoIntersects(shape: Shape): Column = this.geoIntersects(typedLit(shape, new ObjectType(classOf[Shape])))

    def geoContains(other: Column): Column = new Column(GeoContains(column.expr, lit(other).expr))

    def geoContains(shape: Shape): Column = this.geoContains(typedLit(shape, new ObjectType(classOf[Shape])))

    def geoWithin(other: Column): Column = new Column(GeoWithin(column.expr, lit(other).expr))

    def geoWithin(shape: Shape): Column = this.geoWithin(typedLit(shape, new ObjectType(classOf[Shape])))

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
