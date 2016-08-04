package org.insightedge.spark.rdd

import org.insightedge.spark.annotation
import annotation._
import org.insightedge.spark.model.BucketedGridModel

import scala.beans.BeanProperty

/**
 * Grid class for test purposes
 *
 * @author Oleksiy_Dyagilev
 */
case class BucketedGridString(
                       @BeanProperty
                       @SpaceId(autoGenerate = true)
                       var id: String,

                       @BeanProperty
                       var string: String
                       ) extends BucketedGridModel {
  def this() = this(null, null)

  def this(string: String) = this(null, string)
}
