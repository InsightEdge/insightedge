package com.gigaspaces.spark.mllib

import com.gigaspaces.metadata.StorageType
import com.gigaspaces.scala.annotation._
import org.apache.spark.mllib.util.Saveable

import scala.beans.BeanProperty

case class MllibModelHolder(
                    @BeanProperty
                    @SpaceId
                    var id: String,

                    @BeanProperty
                    @SpaceStorageType(storageType = StorageType.BINARY)
                    var model: Saveable
                  ) {
  def this() = this(null, null)

}
