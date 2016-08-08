package org.insightedge.spark.rdd

import org.insightedge.spark.context.InsightEdgeConfig
import org.insightedge.spark.model.BucketedGridModel
import org.insightedge.spark.utils.{BucketIdSeq, GridProxyFactory}
import org.apache.spark.rdd.RDD

import scala.reflect.{ClassTag, classTag}

/**
  * Extra functions available on RDD through an implicit conversion.
  *
  * @author Oleksiy_Dyagilev
  */
class SaveRddToGridExtension[T: ClassTag](rdd: RDD[T]) extends Serializable {

  val DefaultWriteBatchSize = 1000

  val ieConfig = {
    val sparkConf = rdd.sparkContext.getConf
    InsightEdgeConfig.fromSparkConf(sparkConf)
  }

  /**
    * Saves values from given RDD into Data Grid
    */
  def saveToGrid(saveBatchSize: Int = DefaultWriteBatchSize) = {
    val assignBucketId = classOf[BucketedGridModel].isAssignableFrom(classTag[T].runtimeClass)
    val bucketIdSeq = new BucketIdSeq()

    rdd.foreachPartition { partition =>
      val space = GridProxyFactory.getOrCreateClustered(ieConfig)
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
