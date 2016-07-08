package org.apache.spark.sql.insightedge.model

import com.gigaspaces.scala.annotation._
import com.gigaspaces.spark.model.BucketedGridModel

import scala.beans.BeanProperty

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
                 ) {

  def this() = this(null, null, -1, null)

}

case class Address(city: String, state: String)