package com.gigaspaces.spark.context

import com.gigaspaces.spark.mllib.MLModel
import com.gigaspaces.spark.model.GridModel
import com.gigaspaces.spark.rdd.{GigaSpacesBinaryRDD, GigaSpacesRDD, GigaSpacesSqlRDD}
import com.gigaspaces.spark.utils.GigaSpaceFactory
import com.gigaspaces.spark.utils.GigaSpaceUtils.DefaultSplitCount
import org.apache.spark.SparkContext
import org.apache.spark.sql.{DataFrame, SQLContext}

import scala.reflect.ClassTag
import org.apache.spark.sql.insightedge._

class GigaSpacesSparkContext(@transient val sc: SparkContext) extends Serializable {

  val DefaultReadRddBufferSize = 1000
  val DefaultDriverWriteBatchSize = 1000

  val gridSqlContext = new SQLContext(sc)

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
  def gridBinaryRdd[R: ClassTag](splitCount: Option[Int] = Some(DefaultSplitCount), readRddBufferSize: Int = 100): GigaSpacesBinaryRDD[R] = {
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
  def gridSql[R: ClassTag](sqlQuery: String, queryParams: Seq[Any] = Seq(), readRddBufferSize: Int = DefaultReadRddBufferSize): GigaSpacesSqlRDD[R] = {
    new GigaSpacesSqlRDD[R](gsConfig, sc, sqlQuery, queryParams, readRddBufferSize)
  }

  /**
    * Read `DataFrame` from Data Grid.
    *
    * @tparam R GigaSpaces space class
    * @return `DataFrame` instance
    */
  def gridDataFrame[R: ClassTag](readRddBufferSize: Int = DefaultReadRddBufferSize): DataFrame = {
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
    * @tparam A type of object
    */
  def saveToGrid[A: ClassTag](value: A): Unit = {
    gigaSpace.write(value)
  }

  /**
    * Save objects to Data Grid.
    *
    * This is a method on SparkContext, so it can be called from Spark driver only.
    *
    * @param values    object to save
    * @param batchSize batch size for grid write operations
    * @tparam A type of object
    */
  def saveMultipleToGrid[A: ClassTag](values: Iterable[A], batchSize: Int = DefaultDriverWriteBatchSize): Unit = {
    val batches = values.grouped(batchSize)
    batches.foreach { batch =>
      val arr = batch.asInstanceOf[Iterable[Object]].toArray
      gigaSpace.writeMultiple(arr)
    }
  }

  /**
    * Stops internal Spark context and cleans all resources (connections to Data Grid, etc)
    */
  def stopGigaSpacesContext() = {
    sc.stop()
  }

}
