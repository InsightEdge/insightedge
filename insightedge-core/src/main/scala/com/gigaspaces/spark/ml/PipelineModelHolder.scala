package com.gigaspaces.spark.ml

import com.gigaspaces.metadata.StorageType
import com.gigaspaces.scala.annotation._
import org.apache.spark.ml.PipelineModel

import scala.beans.BeanProperty

/**
  * @author Oleksiy_Dyagilev
  */
case class PipelineModelHolder(@BeanProperty
                               @SpaceId
                               var id: String,

                               @BeanProperty
                               @SpaceStorageType(storageType = StorageType.BINARY)
                               var pipelineModel: PipelineModel
                              ) {
  def this() = this(null, null)

}