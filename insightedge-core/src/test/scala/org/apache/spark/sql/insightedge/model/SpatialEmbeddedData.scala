package org.apache.spark.sql.insightedge.model

import org.insightedge.spark.annotation
import annotation._
import org.openspaces.spatial.shapes.Point

import scala.beans.BeanProperty

/**
  * Space class for tests
  */
case class SpatialEmbeddedData(
                                @BeanProperty
                                @SpaceId(autoGenerate = true)
                                var id: String,

                                @BeanProperty
                                @SpaceSpatialIndex(path = "point")
                                var location: Location
                              ) {

  def this() = this(null, null)

}

case class Location(@BeanProperty point: Point)