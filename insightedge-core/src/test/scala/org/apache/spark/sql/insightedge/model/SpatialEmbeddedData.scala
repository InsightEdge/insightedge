package org.apache.spark.sql.insightedge.model

import com.gigaspaces.scala.annotation._
import com.gigaspaces.spark.model.GridModel
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
                              ) extends GridModel {

  def this() = this(null, null)

}

case class Location(@BeanProperty point: Point)