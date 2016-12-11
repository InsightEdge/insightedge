/*
 * Copyright (c) 2016, GigaSpaces Technologies, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

  def createSpace(ieConfig: InsightEdgeConfig): IJSpace = {
    val spaceUri = s"jini://*/*/${ieConfig.spaceName}"
    val urlSpaceConfigurer = new UrlSpaceConfigurer(spaceUri)
    ieConfig.lookupGroups.foreach(urlSpaceConfigurer.lookupGroups)
    ieConfig.lookupLocators.foreach(urlSpaceConfigurer.lookupLocators)
    System.setProperty("com.gs.protectiveMode.ambiguousQueryRoutingUsage", "false")
    urlSpaceConfigurer.space()
  }

  def createGridProxy(ieConfig: InsightEdgeConfig): GigaSpace = {
    profileWithInfo("createClusteredProxy") {
      new GigaSpaceConfigurer(this.createSpace(ieConfig)).create()
    }
  }

  def createDirectProxy(gsPartition: InsightEdgePartition, ieConfig: InsightEdgeConfig): GigaSpace = {
    profileWithInfo("createDirectProxy") {
      val spaceName = ieConfig.spaceName
      val url = s"jini://*/${gsPartition.gridContainerName}/$spaceName"
      val urlSpaceConfigurer = new UrlSpaceConfigurer(url)
      ieConfig.lookupGroups.foreach(urlSpaceConfigurer.lookupGroups)
      ieConfig.lookupLocators.foreach(urlSpaceConfigurer.lookupLocators)
      new GigaSpaceConfigurer(urlSpaceConfigurer.space()).clustered(false).create()
    }
  }


  def buildGridPartitions[R : ClassTag](ieConfig: InsightEdgeConfig, splitCount: Option[Int], supportsBuckets: Boolean): Seq[InsightEdgePartition] = {
    profileWithInfo("lookupInsightEdgePartitions") {
      val gs = GridProxyFactory.getOrCreateClustered(ieConfig)
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
    val gridPartitionsSize = gridPartitions.size
    val sparkPartitions = gridPartitions.flatMap(splitPartitionByBuckets(_, splitCount, gridPartitionsSize))
    sparkPartitions
  }

  def assignPartitionIds(partitions: Seq[InsightEdgePartition]): Seq[InsightEdgePartition] = {
    partitions.zipWithIndex.map { case (partition, index) => partition.copy(id = index)}
  }

  /**
   * Splits bucket ranges across spark partitions
   */
  def splitPartitionByBuckets(gridPartition: InsightEdgePartition, sparkCount: Int, gridPartitionsSize: Int): Seq[InsightEdgePartition] = {
    var totalBuckets = 0
    var localPartitionId = gridPartition.id
    equallySplit(BucketsCount, sparkCount).map(bucketsCount => {
      val sparkPartition = InsightEdgePartition(localPartitionId , gridPartition.hostName, gridPartition.gridContainerName, Some(totalBuckets), Some(totalBuckets + bucketsCount))
      totalBuckets += bucketsCount
      localPartitionId += gridPartitionsSize
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
