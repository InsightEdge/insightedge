package org.insightedge.spark.mllib

import com.gigaspaces.metadata.StorageType
import org.insightedge.spark.annotation
import annotation._

import scala.beans.BeanProperty

/**
  * A holder for MLlib and ML instances (models, pipelines, etc)
  */
case class MLInstance(
                    @BeanProperty
                    @SpaceId
                    var id: String,

                    @BeanProperty
                    @SpaceStorageType(storageType = StorageType.BINARY)
                    var instance: AnyRef
                  ) {
  def this() = this(null, null)

}
