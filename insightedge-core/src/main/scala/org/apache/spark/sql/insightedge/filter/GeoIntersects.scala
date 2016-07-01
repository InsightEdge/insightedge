package org.apache.spark.sql.insightedge.filter

import org.apache.spark.sql.sources.Filter
import org.openspaces.spatial.shapes.Shape

/**
  * @author Leonid_Poliakov
  */
case class GeoIntersects(attribute: String, value: Shape) extends Filter