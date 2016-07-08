package com.gigaspaces.spark.rdd

import com.gigaspaces.spark.context.GigaSpacesConfig
import com.gigaspaces.spark.model.BucketedGridModel
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
    val assignBucketId = classOf[BucketedGridModel].isAssignableFrom(classTag[T].runtimeClass)
    val bucketIdSeq = new BucketIdSeq()

    rdd.foreachPartition { partition =>
      val space = GigaSpaceFactory.getOrCreateClustered(gsConfig)
      val batches = partition.grouped(saveBatchSize)

      batches.foreach { batch =>
        val batchArray = batch.asInstanceOf[Seq[Object]].toArray

        if (assignBucketId) {
          batchArray.foreach { bean =>
            val model = bean.asInstanceOf[BucketedGridModel]
            model.metaBucketId = bucketIdSeq.next()
          }
        }

        space.writeMultiple(batchArray)
      }
    }
  }

}
