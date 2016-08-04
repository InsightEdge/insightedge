package org.apache.spark.sql.insightedge

import com.gigaspaces.metadata.StorageType
import org.insightedge.spark.annotation
import annotation._
import org.apache.spark.sql.types.StructType

import scala.beans.BeanProperty

/**
  * Stores the dataframe schema in space when dataframe is persisted.
  * Is required to be able to read the dataframe back into spark with it's schema.
  *
  * @param collection the name of the type of space documents in space
  * @param schema     the schema of dataframe being persisted
  */
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