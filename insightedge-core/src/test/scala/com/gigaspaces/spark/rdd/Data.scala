package com.gigaspaces.spark.rdd

import com.gigaspaces.scala.annotation._
import com.gigaspaces.spark.model.BucketedGridModel

import scala.beans.{BeanProperty, BooleanBeanProperty}

/**
  * Space class for tests
  */
case class Data @SpaceClassConstructor()(
                                          @BeanProperty
                                          @SpaceId(autoGenerate = true)
                                          var id: String,

                                          @BeanProperty
                                          @SpaceRouting
                                          @SpaceProperty(nullValue = "-1")
                                          routing: Long,

                                          @BeanProperty
                                          data: String,

                                          @BooleanBeanProperty
                                          flag: Boolean
                                        ) {
  def this(routing: Long, data: String) = this(null, routing, data, false)

  def this() = this(-1L, null)

  def this(routing: Long) = this(routing, null)
}