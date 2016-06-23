package com.gigaspaces.spark.rdd

import com.gigaspaces.client.iterator.IteratorScope
import com.gigaspaces.document.SpaceDocument
import com.gigaspaces.spark.context.GigaSpacesConfig
import com.gigaspaces.spark.impl.{GigaSpacesPartition, GigaSpacesQueryIterator, ProfilingIterator}
import com.gigaspaces.spark.utils.{GigaSpaceFactory, GigaSpaceUtils, Profiler}
import com.j_spaces.core.client.SQLQuery
import org.apache.spark.rdd.RDD
import org.apache.spark.{Partition, SparkContext, TaskContext}
import org.openspaces.core.{GigaSpace, IteratorBuilder}

import scala.reflect._

abstract class GigaSpacesAbstractRDD[R: ClassTag](
                                                   gsConfig: GigaSpacesConfig,
                                                   sc: SparkContext,
                                                   partitions: Option[Int],
                                                   readRddBufferSize: Int
                                                 ) extends RDD[R](sc, deps = Nil) {

  /**
    * Reads rdd data from Data Grid for given partition(split)
    *
    * @param split         partition
    * @param dataGridQuery Data Grid query to read data
    * @param convertFunc   convert function to be applied on read Data Grid data
    * @tparam T type of Data Grid query
    * @return iterator over Data Grid
    */
  protected def computeInternal[T](split: Partition, dataGridQuery: SQLQuery[T], convertFunc: T => R, context: TaskContext): Iterator[R] = {
    val startTime = System.currentTimeMillis()
    val gsPartition = split.asInstanceOf[GigaSpacesPartition]
    logInfo(s"Reading partition $gsPartition")

    val directProxy = createDirectProxy(gsPartition)

    val iteratorBuilder = new IteratorBuilder(directProxy)
      .addTemplate(dataGridQuery)
      .bufferSize(readRddBufferSize)
      .iteratorScope(IteratorScope.CURRENT)

    val iterator = profileWithInfo("createIterator") {
      new ProfilingIterator(new GigaSpacesQueryIterator[T, R](iteratorBuilder.iterate(), convertFunc))
    }

    context.addTaskCompletionListener { _ =>
      val endTime = System.currentTimeMillis()
      val duration = (endTime - startTime) / 1000.0
      logInfo(f"Fetched ${iterator.count()} rows for partition $gsPartition in $duration%.3f s. May include time of pipelined operation.")
    }

    iterator
  }

  /**
    * @return if RDD implementation supports bucketing or not
    */
  protected def supportsBuckets(): Boolean = false

  /**
    * Create GigaSpaces Query
    *
    * @param sqlQuery query statement
    * @param params   bounded parameters
    * @param fields   projected fields
    * @tparam T type of query
    * @return GigaSpaces sql query
    */
  protected def createGigaSpacesQuery[T: ClassTag](sqlQuery: String, params: Seq[Any] = Seq(), fields: Seq[String] = Seq()): SQLQuery[T] = {
    val clazz = classTag[T].runtimeClass
    val query = new SQLQuery[T](clazz.asInstanceOf[Class[T]], sqlQuery)
    query.setParameters(params.map(_.asInstanceOf[Object]): _*)
    if (fields.nonEmpty) {
      query.setProjections(fields.toArray: _*)
    }
    query
  }

  /**
    * Create GigaSpaces Query for SpaceDocuments
    *
    * @param typeName name of the documents type
    * @param sqlQuery query statement
    * @param params   bounded parameters
    * @param fields   projected fields
    * @return GigaSpaces sql query
    */
  protected def createDocumentGigaSpacesQuery(typeName: String, sqlQuery: String, params: Seq[Any] = Seq(), fields: Seq[String] = Seq()): SQLQuery[SpaceDocument] = {
    val query = new SQLQuery[SpaceDocument](typeName, sqlQuery)
    query.setParameters(params.map(_.asInstanceOf[Object]): _*)
    if (fields.nonEmpty) {
      query.setProjections(fields.toArray: _*)
    }
    query
  }

  /**
    * Create a query by metaBucketId for given bucketed partition or empty query for non-bucketed partition
    *
    * @param split the partition bean
    * @return sql query string with bucket range
    */
  protected def bucketQuery(split: Partition): String = {
    val partition = split.asInstanceOf[GigaSpacesPartition]
    val rangeQuery = for {
      bottom <- partition.bucketRangeBottom
      top <- partition.bucketRangeTop
    } yield s"metaBucketId >= $bottom and metaBucketId < $top"

    rangeQuery.getOrElse("")
  }

  /**
    * Wraps given query into (...) and appends 'and `bucketQuery`' in the end.
    *
    * @param query given query
    * @param split given partition
    * @return query appended with bucket ids
    */
  protected def bucketize(query: String, split: Partition): String = {
    if (query.trim.isEmpty) {
      bucketQuery(split)
    } else {
      s"($query) and ${bucketQuery(split)}"
    }
  }

  /**
    * Gets partitions for the given cache RDD.
    *
    * @return Partitions
    */
  override protected def getPartitions: Array[Partition] = {
    profileWithInfo("getPartitions") {
      val dataGridPartitions = GigaSpaceUtils.buildGigaSpacePartitions[R](gsConfig, partitions, supportsBuckets())
      logInfo(s"Found data grid partitions $dataGridPartitions")

      dataGridPartitions.toArray
    }
  }

  /**
    * Gets preferred locations for the given partition.
    *
    * @param split Split partition.
    * @return
    */
  override protected def getPreferredLocations(split: Partition): Seq[String] = {
    profileWithInfo("getPreferredLocations") {
      val gigaSpacesPartition = split.asInstanceOf[GigaSpacesPartition]
      val preferredHost = gigaSpacesPartition.hostName
      logInfo(s"Preferred location for partition ${split.index} is $preferredHost")
      Seq(preferredHost)
    }
  }

  protected def createDirectProxy(gsPartition: GigaSpacesPartition): GigaSpace = {
    GigaSpaceFactory.getOrCreateDirect(gsPartition, gsConfig)
  }


  protected def profileWithInfo[T](message: String)(block: => T): T = Profiler.profile(message)(logInfo(_))(block)

}

