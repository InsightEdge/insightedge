package com.gigaspaces.spark.rdd

import com.gigaspaces.client.iterator.IteratorScope
import com.gigaspaces.spark.context.GigaSpacesConfig
import com.gigaspaces.spark.impl.{GigaSpacesQueryIterator, ProfilingIterator, GigaSpacesPartition}
import com.gigaspaces.spark.model.{GridBinaryModel, GridModel}
import com.j_spaces.core.client.SQLQuery
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.{Partition, SparkContext, TaskContext}
import org.openspaces.core.IteratorBuilder

import scala.reflect._

/**
  * Experimental
  *
  * TODO: cleanup/refactor/remove
  */
class GigaSpacesBinaryRDD[R : ClassTag](
                                         gsConfig: GigaSpacesConfig,
                                         sc: SparkContext,
                                         splitCount: Option[Int],
                                         readRddBufferSize: Int
                                       ) extends GigaSpacesAbstractRDD[R](gsConfig, sc, splitCount, readRddBufferSize) {

  @DeveloperApi
  override def compute(split: Partition, context: TaskContext): Iterator[R] = {
    val clazzString = classTag[R].runtimeClass.getName

    val query = new SQLQuery[GridBinaryModel](classOf[GridBinaryModel], bucketQuery(split) + " AND clazz = ?")
    query.setParameters(clazzString)

    val startTime = System.currentTimeMillis()
    val gsPartition = split.asInstanceOf[GigaSpacesPartition]
    logInfo(s"Reading partition $gsPartition")

    val directProxy = createDirectProxy(gsPartition)

    val iteratorBuilder = new IteratorBuilder(directProxy)
      .addTemplate(query)
      .bufferSize(readRddBufferSize)
      .iteratorScope(IteratorScope.CURRENT)

    val iterator = profileWithInfo("createIterator") {
      val gridObjectsIter = new GigaSpacesQueryIterator[GridBinaryModel, GridBinaryModel](iteratorBuilder.iterate(), identity)
      val itemsIter = gridObjectsIter.flatMap(_.getItemsArray.asInstanceOf[Array[R]])
      new ProfilingIterator(itemsIter)
    }

    context.addTaskCompletionListener { _ =>
      val endTime = System.currentTimeMillis()
      val duration = (endTime - startTime) / 1000.0
      logInfo(f"Fetched ${iterator.count()} items(not space objects) for partition $gsPartition in $duration%.3f s. May include time of pipelined operation.")
    }

    iterator
  }

  @DeveloperApi
  override def supportsBuckets(): Boolean = true

}
