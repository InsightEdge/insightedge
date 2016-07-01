package org.apache.spark.sql.insightedge.expression

import org.apache.lucene.spatial.query.SpatialOperation
import org.apache.spark.sql.catalyst.expressions._

/**
  * @author Leonid_Poliakov
  */
case class GeoWithin(left: Expression, right: Expression) extends GeoExpression(left, right) {

  override val operation: SpatialOperation = SpatialOperation.IsWithin

  override val operationName: String = "IsWithin"

}