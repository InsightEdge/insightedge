package com.gigaspaces.spark.streaming

import com.gigaspaces.spark.context.GigaSpacesConfig
import com.gigaspaces.spark.utils.GigaSpaceFactory
import org.apache.spark.streaming.dstream.DStream

import scala.reflect.ClassTag

/**
  * Extra functions available on DStream through an implicit conversion.
  *
  * @author Oleksiy_Dyagilev
  */
class SaveDStreamToGridExtension[T: ClassTag](@transient dStream: DStream[T]) extends Serializable {

  /**
    * Saves DStream to Data Grid
    * @param writeBatchSize batch size for grid write operations
    */
  def saveToGrid(writeBatchSize: Int = 1000) = {
    val sparkConfig = dStream.context.sparkContext.getConf
    val gsConfig = GigaSpacesConfig.fromSparkConf(sparkConfig)

    dStream.foreachRDD { rdd =>
      rdd.foreachPartition { partitionOfRecords =>
        val gigaSpace = GigaSpaceFactory.getOrCreateClustered(gsConfig)
        val batches = partitionOfRecords.grouped(writeBatchSize)

        batches.foreach { batch =>
          val arr = batch.asInstanceOf[Iterable[Object]].toArray
          gigaSpace.writeMultiple(arr)
        }
      }
    }
  }


}
