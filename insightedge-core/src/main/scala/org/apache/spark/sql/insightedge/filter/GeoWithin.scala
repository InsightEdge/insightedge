package org.apache.spark.sql.insightedge.filter

import org.apache.spark.sql.sources.Filter
import org.openspaces.spatial.shapes.Shape

/**
  * @author Leonid_Poliakov
  */
case class GeoWithin(attribute: String, value: Shape) extends Filter {
  override def references: Array[String] = Array(attribute) ++ findReferences(value)
}