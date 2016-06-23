package org.apache.spark.sql.insightedge.expression

import com.spatial4j.core.context.SpatialContext
import com.spatial4j.core.context.jts.{JtsSpatialContext, JtsSpatialContextFactory}

/**
  * @author Leonid_Poliakov
  */
object SpatialFactory {

  lazy val defaultContext = createDefaultSpatialContext()

  def createDefaultSpatialContext(): SpatialContext = {
    val factory = new JtsSpatialContextFactory()
    factory.geo = true
    new JtsSpatialContext(factory)
  }

}