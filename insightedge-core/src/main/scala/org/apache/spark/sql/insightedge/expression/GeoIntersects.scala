package org.apache.spark.sql.insightedge.expression

import com.spatial4j.core.context.SpatialContext
import com.spatial4j.core.shape.{Shape => SpatialShape}
import org.apache.lucene.spatial.query.SpatialOperation
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.expressions.codegen.{CodeGenContext, GeneratedExpressionCode}
import org.apache.spark.sql.types.{AbstractDataType, ObjectType}
import org.openspaces.spatial.shapes.{Shape => SpaceShape}
import org.openspaces.spatial.spatial4j.Spatial4jShapeProvider

/**
  * @author Leonid_Poliakov
  */
case class GeoIntersects(left: Expression, right: Expression) extends BinaryExpression with Predicate {

  override def nullSafeEval(first: Any, second: Any): Any = {
    val spatialContext = SpatialFactory.defaultContext
    val firstShape = first.asInstanceOf[Spatial4jShapeProvider].getSpatial4jShape(spatialContext)
    val secondShape = second.asInstanceOf[Spatial4jShapeProvider].getSpatial4jShape(spatialContext)

    SpatialOperation.Intersects.evaluate(firstShape, secondShape)
  }

  override def genCode(ctx: CodeGenContext, ev: GeneratedExpressionCode): String = {
    val factoryClass = SpatialFactory.getClass.getName
    val contextClass = classOf[SpatialContext].getName
    val shapeClass = classOf[SpatialShape].getName
    val providerClass = classOf[Spatial4jShapeProvider].getName
    val operationClass = classOf[SpatialOperation].getName

    val context = ctx.freshName("context")
    val firstShape = ctx.freshName("firstShape")
    val secondShape = ctx.freshName("secondShape")

    defineCodeGen(ctx, ev, (firstVar, secondVar) =>
      s"""
         $contextClass $context = $factoryClass.defaultContext;
         $shapeClass $firstShape = (($providerClass)$firstVar).getSpatial4jShape($context);
         $shapeClass $secondShape = (($providerClass)$secondVar).getSpatial4jShape($context);

         ${ev.value} = $operationClass.Intersects.evaluate($firstShape, $secondShape);
       """)
  }

}