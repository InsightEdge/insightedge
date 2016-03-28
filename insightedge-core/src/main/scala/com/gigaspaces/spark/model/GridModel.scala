package com.gigaspaces.spark.model

import com.gigaspaces.scala.annotation._

import scala.beans.BeanProperty

/**
 * Trait used to define space classes for grid RDDs. Contains metadata used for performance optimization.
 *
 * @author Leonid_Poliakov
 */
trait GridModel {
  @BeanProperty
  @SpaceIndex
  var metaBucketId: Integer = null
}