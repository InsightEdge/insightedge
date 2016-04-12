package org.apache.spark.sql.insightedge

import com.gigaspaces.scala.annotation._
import com.gigaspaces.spark.model.GridModel

import scala.beans.{BeanProperty, BooleanBeanProperty}

/**
  * Space class for tests
  */
case class Person(
                   @BeanProperty
                   @SpaceId(autoGenerate = true)
                   var id: String,

                   @BeanProperty
                   var name: String,

                   @BeanProperty
                   var age: Int,

                   @BeanProperty
                   var address: Address
                 ) extends GridModel {

  def this() = this(null, null, -1, null)

}

case class Address(city: String, state: String)