package com.gigaspaces.spark.rdd

import com.gigaspaces.spark.context.GigaSpacesConfig
import com.gigaspaces.spark.model.{GridBinaryModel, GridModel}
import com.gigaspaces.spark.utils.{BucketIdSeq, GigaSpaceFactory}
import org.apache.spark.rdd.RDD

import scala.reflect.{ClassTag, classTag}

/**
  * Extra functions available on RDD through an implicit conversion.
  *
  * @author Oleksiy_Dyagilev
  */
class SaveRddToGridExtension[T: ClassTag](rdd: RDD[T]) extends Serializable {

  val DefaultWriteBatchSize = 1000

  val gsConfig = {
    val sparkConf = rdd.sparkContext.getConf
    GigaSpacesConfig.fromSparkConf(sparkConf)
  }

  /**
    * Saves values from given RDD into Data Grid
    */
  def saveToGrid(saveBatchSize: Int = DefaultWriteBatchSize) = {
    val assignBucketId = classOf[GridModel].isAssignableFrom(classTag[T].runtimeClass)
    val bucketIdSeq = new BucketIdSeq()

    rdd.foreachPartition { partition =>
      val space = GigaSpaceFactory.getOrCreateClustered(gsConfig)
      val batches = partition.grouped(saveBatchSize)

      batches.foreach { batch =>
        val batchArray = batch.asInstanceOf[Seq[Object]].toArray

        if (assignBucketId) {
          batchArray.foreach { bean =>
            val model = bean.asInstanceOf[GridModel]
            model.metaBucketId = bucketIdSeq.next()
          }
        }

        space.writeMultiple(batchArray)
      }
    }
  }

  /**
    * Experimental. TODO: cleanup
    */
  def saveToGridBinary(binaryArraySize: Int = 1000) = {
    val bucketIdSeq = new BucketIdSeq()
    // TODO: proper name, perf/uniqueness
    val clazzString = classTag[T].runtimeClass.getName

    rdd.foreachPartition { partition =>
      val space = GigaSpaceFactory.getOrCreateClustered(gsConfig)
      val splits = partition.grouped(binaryArraySize)

      splits.foreach(split => {
        val itemsArray = split.asInstanceOf[Seq[Object]].toArray
        val gridBinaryModel = new GridBinaryModel(clazzString, itemsArray)
        gridBinaryModel.metaBucketId = bucketIdSeq.next()

        space.write(gridBinaryModel)
      })
    }

  }

}
