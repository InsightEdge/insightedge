package com.gigaspaces.spark.context

import com.gigaspaces.spark.mllib.MLModel
import com.gigaspaces.spark.model.BucketedGridModel
import com.gigaspaces.spark.rdd.{GigaSpacesRDD, GigaSpacesSqlRDD}
import com.gigaspaces.spark.utils.{BucketIdSeq, GigaSpaceFactory, GigaSpaceUtils}
import com.gigaspaces.spark.utils.GigaSpaceConstants._
import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext

import scala.reflect._
import scala.util.Random

class GigaSpacesSparkContext(@transient val sc: SparkContext) extends Serializable {

  lazy val gridSqlContext = new SQLContext(sc)

  val gsConfig = {
    val sparkConf = sc.getConf
    GigaSpacesConfig.fromSparkConf(sparkConf)
  }

  def gigaSpace = GigaSpaceFactory.getOrCreateClustered(gsConfig)

  /**
    * Read dataset from GigaSpaces Data Grid.
    *
    * @tparam R GigaSpaces space class
    * @param splitCount        only applicable for BucketedGridModel, number of spark partitions per datagrid partition, defaults to x4. For non-bucketed types this parameter is ignored.
    * @param readRddBufferSize buffer size of the underlying iterator that reads from the grid
    * @return GigaSpaces RDD
    */
  def gridRdd[R : ClassTag](splitCount: Option[Int] = Some(DefaultSplitCount), readRddBufferSize: Int = DefaultReadBufferSize): GigaSpacesRDD[R] = {
    new GigaSpacesRDD[R](gsConfig, sc, splitCount, readRddBufferSize)
  }

  /**
    * Read dataset from Data Grid with GigaSpaces SQL query
    *
    * @param sqlQuery          SQL query to be executed on Data Grid
    * @param queryParams       params for SQL quey
    * @param splitCount        only applicable for BucketedGridModel, number of spark partitions per datagrid partition, defaults to x4. For non-bucketed types this parameter is ignored.
    * @param readRddBufferSize buffer size of the underlying iterator that reads from the grid
    * @tparam R GigaSpaces space class
    * @return
    */
  def gridSql[R : ClassTag](sqlQuery: String, queryParams: Seq[Any] = Seq(), splitCount: Option[Int] = Some(DefaultSplitCount), readRddBufferSize: Int = DefaultReadBufferSize): GigaSpacesSqlRDD[R] = {
    new GigaSpacesSqlRDD[R](gsConfig, sc, sqlQuery, queryParams, Seq.empty[String], splitCount, readRddBufferSize)
  }

  /**
    * Load MLlib model from Data Grid
    *
    * @param modelName name of MLModel
    * @tparam R MLlib model class
    * @return MLlib model
    */
  def loadMLModel[R: ClassTag](modelName: String): Option[R] = {
    val mlModel = gigaSpace.readById(classOf[MLModel], modelName)
    mlModel match {
      case MLModel(name, model: R) => Some(model)
      case _ => None
    }
  }

  /**
    * Save object to Data Grid.
    *
    * This is a method on SparkContext, so it can be called from Spark driver only.
    *
    * @param value object to save
    * @tparam R type of object
    */
  def saveToGrid[R: ClassTag](value: R): Unit = {
    if (classOf[BucketedGridModel].isAssignableFrom(classTag[R].runtimeClass)) {
      value.asInstanceOf[BucketedGridModel].metaBucketId = Random.nextInt(BucketsCount)
    }
    gigaSpace.write(value)
  }

  /**
    * Save objects to Data Grid.
    *
    * This is a method on SparkContext, so it can be called from Spark driver only.
    *
    * @param values    object to save
    * @param batchSize batch size for grid write operations
    * @tparam R type of object
    */
  def saveMultipleToGrid[R: ClassTag](values: Iterable[R], batchSize: Int = DefaultDriverWriteBatchSize): Unit = {
    val assignBucketId = classOf[BucketedGridModel].isAssignableFrom(classTag[R].runtimeClass)
    val bucketIdSeq = new BucketIdSeq()

    val batches = values.grouped(batchSize)
    batches.foreach { batch =>
      val batchArray = batch.asInstanceOf[Iterable[Object]].toArray

      if (assignBucketId) {
        batchArray.foreach { bean =>
          bean.asInstanceOf[BucketedGridModel].metaBucketId = bucketIdSeq.next()
        }
      }

      gigaSpace.writeMultiple(batchArray)
    }
  }

  /**
    * Stops internal Spark context and cleans all resources (connections to Data Grid, etc)
    */
  def stopGigaSpacesContext() = {
    sc.stop()
  }

}
