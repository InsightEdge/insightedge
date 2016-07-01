package org.apache.spark.sql.insightedge.expression

import org.apache.lucene.spatial.query.SpatialOperation
import org.apache.spark.sql.catalyst.expressions._

/**
  * @author Leonid_Poliakov
  */
case class GeoContains(left: Expression, right: Expression) extends GeoExpression(left, right) {

  override val operation: SpatialOperation = SpatialOperation.Contains

  override val operationName: String = "Contains"

}