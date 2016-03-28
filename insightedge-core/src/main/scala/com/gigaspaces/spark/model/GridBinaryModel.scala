package com.gigaspaces.spark.model

import com.gigaspaces.metadata.StorageType
import com.gigaspaces.scala.annotation._

import scala.beans.BeanProperty

/**
  * Experimental.
  *
  * Grid binary model format.
  *
  * @author Oleksiy_Dyagilev
  */
case class GridBinaryModel(

                 @BeanProperty
                 @SpaceId(autoGenerate = true)
                 var id: String = null,

                 @BeanProperty
                 @SpaceIndex
                 var clazz: String,

                 @BeanProperty
                 @SpaceStorageType(storageType = StorageType.BINARY)
                 var itemsArray: Array[Object]

               ) extends GridModel {

  def this() = this(null, null, null)
  def this(clazz: String, itemsArray: Array[Object]) = this(null, clazz, itemsArray)

}
