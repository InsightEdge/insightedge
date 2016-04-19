package org.apache.spark.sql.insightedge

import com.gigaspaces.metadata.StorageType
import com.gigaspaces.scala.annotation._
import org.apache.spark.sql.types.StructType

import scala.beans.BeanProperty

class DataFrameSchema(
                       @BeanProperty
                       @SpaceId(autoGenerate = false)
                       var collection: String,

                       @BeanProperty
                       @SpaceStorageType(storageType = StorageType.BINARY)
                       var schema: StructType
                     ) {

  def this() = this(null, null)

}