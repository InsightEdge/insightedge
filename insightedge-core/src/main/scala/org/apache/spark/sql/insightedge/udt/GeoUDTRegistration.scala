package org.apache.spark.sql.insightedge.udt

import org.apache.spark.sql.types.UDTRegistration

/**
  * Registers Geo Shape UDTs
  *
  * @author Oleksiy_Dyagilev
  */
object GeoUDTRegistration {

  /**
    * register Geo Shape UDTs if not already registered
    */
  def registerIfNotAlready(): Unit ={
    registerIfNotAlready("org.openspaces.spatial.shapes.Circle", "org.apache.spark.sql.insightedge.udt.CircleUDT")
    registerIfNotAlready("org.openspaces.spatial.shapes.Point", "org.apache.spark.sql.insightedge.udt.PointUDT")
    registerIfNotAlready("org.openspaces.spatial.shapes.Polygon", "org.apache.spark.sql.insightedge.udt.PolygonUDT")
    registerIfNotAlready("org.openspaces.spatial.shapes.Rectangle", "org.apache.spark.sql.insightedge.udt.RectangleUDT")
  }

  private def registerIfNotAlready(userClass: String, udtClass: String) = {
    if (!UDTRegistration.exists(userClass)){
      UDTRegistration.register(userClass, udtClass)
    }
  }

}
