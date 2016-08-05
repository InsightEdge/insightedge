package org.apache.spark.sql.insightedge.model

import org.insightedge.scala.annotation
import annotation._
import org.openspaces.spatial.shapes.{Circle, Point, Rectangle}

import scala.beans.BeanProperty

/**
  * Space class for tests
  */
case class SpatialData(
                        @BeanProperty
                        @SpaceId(autoGenerate = true)
                        var id: String,

                        @BeanProperty
                        @SpaceRouting
                        var routing: Long,

                        @BeanProperty
                        @SpaceSpatialIndex
                        var circle: Circle,

                        @BeanProperty
                        var rect: Rectangle,

                        @BeanProperty
                        var point: Point
                      ) {

  def this(routing: Long) = this(null, routing, null, null, null)

  def this() = this(-1)

}