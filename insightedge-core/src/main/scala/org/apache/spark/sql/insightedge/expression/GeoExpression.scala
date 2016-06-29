package org.apache.spark.sql.insightedge.expression

import com.spatial4j.core.context.SpatialContext
import com.spatial4j.core.shape.{Shape => SpatialShape}
import org.apache.lucene.spatial.query.SpatialOperation
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.expressions.codegen.{CodeGenContext, GeneratedExpressionCode}
import org.apache.spark.sql.insightedge.udt.GeoUtils

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

  override def genCode(ctx: CodeGenContext, ev: GeneratedExpressionCode): String = {
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