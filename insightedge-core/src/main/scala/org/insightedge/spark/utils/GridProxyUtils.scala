package org.insightedge.spark.utils

import java.lang.Math._

import com.j_spaces.core.IJSpace
import org.apache.spark.Logging
import org.insightedge.spark.context.InsightEdgeConfig
import org.insightedge.spark.impl.InsightEdgePartition
import org.insightedge.spark.utils.InsightEdgeConstants.{BucketsCount, DefaultSplitCount}
import org.openspaces.core.space.UrlSpaceConfigurer
import org.openspaces.core.{GigaSpace, GigaSpaceConfigurer}

import scala.collection.JavaConversions._
import scala.reflect._

/**
 * @author Oleksiy_Dyagilev
 */
private[spark] object GridProxyUtils extends Logging {

  def createSpace(gsConfig: InsightEdgeConfig): IJSpace = {
    val spaceUri = s"jini://*/*/${gsConfig.spaceName}"
    val urlSpaceConfigurer = new UrlSpaceConfigurer(spaceUri)
    gsConfig.lookupGroups.foreach(urlSpaceConfigurer.lookupGroups)
    gsConfig.lookupLocators.foreach(urlSpaceConfigurer.lookupLocators)
    urlSpaceConfigurer.space()
  }

  def createGridProxy(gsConfig: InsightEdgeConfig): GigaSpace = {
    profileWithInfo("createClusteredProxy") {
      new GigaSpaceConfigurer(this.createSpace(gsConfig)).create()
    }
  }

  def createDirectProxy(gsPartition: InsightEdgePartition, gsConfig: InsightEdgeConfig): GigaSpace = {
    profileWithInfo("createDirectProxy") {
      val spaceName = gsConfig.spaceName
      val url = s"jini://*/${gsPartition.gridContainerName}/$spaceName"
      val urlSpaceConfigurer = new UrlSpaceConfigurer(url)
      gsConfig.lookupGroups.foreach(urlSpaceConfigurer.lookupGroups)
      gsConfig.lookupLocators.foreach(urlSpaceConfigurer.lookupLocators)
      new GigaSpaceConfigurer(urlSpaceConfigurer.space()).clustered(false).create()
    }
  }


  def buildGridPartitions[R : ClassTag](gsConfig: InsightEdgeConfig, splitCount: Option[Int], supportsBuckets: Boolean): Seq[InsightEdgePartition] = {
    profileWithInfo("lookupInsightEdgePartitions") {
      val gs = GridProxyFactory.getOrCreateClustered(gsConfig)
      val asyncResult = gs.execute(new LookupPartitionTask)
      val gsPartitions = asyncResult.get().map(_.toList).map {
        case List(hostName: String, containerName: String, id: String) => InsightEdgePartition(id.toInt, hostName, containerName)
      }
      if (supportsBuckets) {
        splitPartitionsByBuckets(gsPartitions, splitCount)
      } else {
        gsPartitions
      }
    }
  }

  def splitPartitionsByBuckets(gridPartitions: Seq[InsightEdgePartition], optionalSplitCount: Option[Int]): Seq[InsightEdgePartition] = {
    val splitCount = max(1, optionalSplitCount.getOrElse(DefaultSplitCount))
    val sparkPartitions = gridPartitions.flatMap(splitPartitionByBuckets(_, splitCount))
    assignPartitionIds(sparkPartitions)
  }

  def assignPartitionIds(partitions: Seq[InsightEdgePartition]): Seq[InsightEdgePartition] = {
    partitions.zipWithIndex.map { case (partition, index) => partition.copy(id = index)}
  }

  /**
   * Splits bucket ranges across spark partitions
   */
  def splitPartitionByBuckets(gridPartition: InsightEdgePartition, sparkCount: Int): Seq[InsightEdgePartition] = {
    var totalBuckets = 0
    equallySplit(BucketsCount, sparkCount).map(bucketsCount => {
      val sparkPartition = InsightEdgePartition(gridPartition.id, gridPartition.hostName, gridPartition.gridContainerName, Some(totalBuckets), Some(totalBuckets + bucketsCount))
      totalBuckets += bucketsCount
      sparkPartition
    })
  }

  /**
   * Splits the value across parts, the sum of the result list equals to value and the size equals to parts count
   * <p>
   * Examples:
   * <blockquote><pre>
   * equallySplit(10, 2) -> [5, 5]
   * equallySplit(10, 3) -> [4, 3, 3]
   * equallySplit(10, 4) -> [3, 3, 2, 2]
   * </blockquote></pre>
   *
   * @param value the number to split
   * @param partsCount the number of parts to split to
   * @return a sequence of value parts
   */
  def equallySplit(value: Int, partsCount: Int): Seq[Int] = {
    val valuePerPart = value / partsCount
    val plusOneValue = value % partsCount
    (0 until partsCount).toList.map(index => {
      if (index < plusOneValue) valuePerPart + 1 else valuePerPart
    })
  }

  private def profileWithInfo[T](message: String)(block: => T): T = Profiler.profile(message)(logInfo(_))(block)

}
