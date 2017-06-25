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

package org.apache.spark.sql.insightedge.expression

import org.apache.lucene.spatial.query.SpatialOperation
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.expressions.codegen.{CodegenContext, ExprCode}
import org.apache.spark.sql.insightedge.udt.GeoUtils
import org.locationtech.spatial4j.context.SpatialContext
import org.locationtech.spatial4j.shape.{Shape => SpatialShape}

/**
  * @author Leonid_Poliakov
  */
abstract class GeoExpression(left: Expression, right: Expression) extends BinaryExpression with Predicate with Serializable {

  val operation: SpatialOperation

  val operationName: String

  override def nullSafeEval(first: Any, second: Any): Any = {
    val spatialContext = GeoUtils.defaultContext
    val firstShape = GeoUtils.unpackSpatialShape(first, spatialContext)
    val secondShape = GeoUtils.unpackSpatialShape(second, spatialContext)

    operation.evaluate(firstShape, secondShape)
  }

  override def doGenCode(ctx: CodegenContext, ev: ExprCode): ExprCode = {
    val utilClass = GeoUtils.getClass.getName
    val contextClass = classOf[SpatialContext].getName
    val shapeClass = classOf[SpatialShape].getName
    val operationClass = classOf[SpatialOperation].getName

    val context = ctx.freshName("context")
    val firstShape = ctx.freshName("firstShape")
    val secondShape = ctx.freshName("secondShape")

    val utils = utilClass + ".MODULE$"

    nullSafeCodeGen(ctx, ev, (firstVar, secondVar) =>
      s"""
         $contextClass $context = $utils.defaultContext();
         $shapeClass $firstShape = $utils.unpackSpatialShape($firstVar, $context);
         $shapeClass $secondShape = $utils.unpackSpatialShape($secondVar, $context);

         ${ev.value} = $operationClass.$operationName.evaluate($firstShape, $secondShape);
       """)
  }

}