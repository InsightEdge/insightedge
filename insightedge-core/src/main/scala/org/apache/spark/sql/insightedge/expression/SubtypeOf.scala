package org.apache.spark.sql.insightedge.expression

import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.expressions.codegen.{CodeGenContext, GeneratedExpressionCode}

/**
  * @author Leonid_Poliakov
  */
case class SubtypeOf(left: Expression, right: Expression) extends BinaryExpression with Predicate {

  override def nullSafeEval(first: Any, second: Any): Any = {
    val firstClazz = first.asInstanceOf[AnyRef].getClass
    val secondClazz = second.asInstanceOf[Class[_]]

    secondClazz.isAssignableFrom(firstClazz)
  }

  override def genCode(ctx: CodeGenContext, ev: GeneratedExpressionCode): String = {
    defineCodeGen(ctx, ev, (first, second) => s"((java.lang.Class)$second).isAssignableFrom($first.getClass())")
  }

}