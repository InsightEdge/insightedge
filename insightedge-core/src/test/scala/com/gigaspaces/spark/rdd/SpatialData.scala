package com.gigaspaces.spark.rdd

import com.gigaspaces.scala.annotation._
import com.gigaspaces.spark.model.GridModel
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
                        @SpaceProperty(nullValue = "-1")
                        var routing: Long,

                        @BeanProperty
                        var circle: Circle,

                        @BeanProperty
                        var rect: Rectangle,

                        @BeanProperty
                        var point: Point

                      ) extends GridModel {

  def this(routing: Long) = this(null, routing, null, null, null)

  def this() = this(-1)

}