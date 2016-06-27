package com.gigaspaces.spark.context

import com.gigaspaces.spark.mllib.MLModel
import com.gigaspaces.spark.model.GridModel
import com.gigaspaces.spark.rdd.{GigaSpacesBinaryRDD, GigaSpacesRDD, GigaSpacesSqlRDD}
import com.gigaspaces.spark.utils.GigaSpaceUtils.DefaultSplitCount
import com.gigaspaces.spark.utils.{BucketIdSeq, GigaSpaceFactory, GigaSpaceUtils}
import org.apache.spark.SparkContext
import org.apache.spark.sql.insightedge._
import org.apache.spark.sql.{DataFrame, SQLContext}

import scala.reflect._
import scala.util.Random

class GigaSpacesSparkContext(@transient val sc: SparkContext) extends Serializable {

  val DefaultReadRddBufferSize = 1000
  val DefaultDriverWriteBatchSize = 1000

  lazy val gridSqlContext = {
    val sql = new SQLContext(sc)
    sql.experimental.extraStrategies = (InsightEdgeSourceStrategy :: Nil) ++ sql.experimental.extraStrategies
    sql
  }

  val gsConfig = {
    val sparkConf = sc.getConf
    GigaSpacesConfig.fromSparkConf(sparkConf)
  }

  def gigaSpace = GigaSpaceFactory.getOrCreateClustered(gsConfig)

  /**
    * Read dataset from GigaSpaces Data Grid.
    *
    * @tparam R GigaSpaces space class
    * @param splitCount        number of spark partitions per datagrid partition; defaults to x4
    * @param readRddBufferSize buffer size of the underlying iterator that reads from the grid
    * @return GigaSpaces RDD
    */
  def gridRdd[R <: GridModel : ClassTag](splitCount: Option[Int] = Some(DefaultSplitCount), readRddBufferSize: Int = DefaultReadRddBufferSize): GigaSpacesRDD[R] = {
    new GigaSpacesRDD[R](gsConfig, sc, splitCount, readRddBufferSize)
  }

  /**
    * Experimental. TODO: cleanup
    */
  def gridBinaryRdd[R <: GridModel : ClassTag](splitCount: Option[Int] = Some(DefaultSplitCount), readRddBufferSize: Int = 100): GigaSpacesBinaryRDD[R] = {
    new GigaSpacesBinaryRDD[R](gsConfig, sc, splitCount, readRddBufferSize)
  }

  /**
    * Read dataset from Data Grid with GigaSpaces SQL query
    *
    * @param sqlQuery          SQL query to be executed on Data Grid
    * @param readRddBufferSize buffer size of the underlying iterator that reads from the grid
    * @param queryParams       params for SQL quey
    * @tparam R GigaSpaces space class
    * @return
    */
  def gridSql[R <: GridModel : ClassTag](sqlQuery: String, queryParams: Seq[Any] = Seq(), readRddBufferSize: Int = DefaultReadRddBufferSize): GigaSpacesSqlRDD[R] = {
    new GigaSpacesSqlRDD[R](gsConfig, sc, sqlQuery, queryParams, Seq.empty[String], readRddBufferSize)
  }

  /**
    * Read `DataFrame` from Data Grid.
    *
    * @tparam R GigaSpaces space class
    * @return `DataFrame` instance
    */
  def gridDataFrame[R <: GridModel : ClassTag](readRddBufferSize: Int = DefaultReadRddBufferSize): DataFrame = {
    gridSqlContext.read.grid.loadClass[R]
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
    if (classOf[GridModel].isAssignableFrom(classTag[R].runtimeClass)) {
      value.asInstanceOf[GridModel].metaBucketId = Random.nextInt(GigaSpaceUtils.BucketsCount)
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
    val assignBucketId = classOf[GridModel].isAssignableFrom(classTag[R].runtimeClass)
    val bucketIdSeq = new BucketIdSeq()

    val batches = values.grouped(batchSize)
    batches.foreach { batch =>
      val batchArray = batch.asInstanceOf[Iterable[Object]].toArray

      if (assignBucketId) {
        batchArray.foreach { bean =>
          bean.asInstanceOf[GridModel].metaBucketId = bucketIdSeq.next()
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
