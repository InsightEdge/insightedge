package org.insightedge.spark.rdd

import org.insightedge.scala.annotation
import annotation._
import org.insightedge.spark.model.BucketedGridModel

import scala.beans.{BeanProperty, BooleanBeanProperty}

/**
 * Space class for tests
 */
case class BucketedData(
                 @BeanProperty
                 @SpaceId(autoGenerate = true)
                 var id: String,

                 @BeanProperty
                 @SpaceRouting
                 var routing: Long,

                 @BeanProperty
                 var data: String,

                 @BooleanBeanProperty
                 var flag: Boolean
                 ) extends BucketedGridModel {

  def this(routing: Long, data: String) = this(null, routing, data, false)

  def this() = this(-1, null)

  def this(routing: Long) = this(routing, null)
}