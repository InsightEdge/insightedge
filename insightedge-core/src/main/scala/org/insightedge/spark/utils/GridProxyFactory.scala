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

import org.insightedge.spark.context.InsightEdgeConfig
import org.insightedge.spark.impl.InsightEdgePartition
import org.openspaces.core.GigaSpace

/**
 * Ensures single GigaSpaces instance per JVM (Spark worker)
 *
 * @author Oleksiy_Dyagilev
 */
object GridProxyFactory {

  private val clusterProxyCache = new LocalCache[InsightEdgeConfig, GigaSpace]()
  private val directProxyCache = new LocalCache[String, GigaSpace]()

  def getOrCreateClustered(ieConfig: InsightEdgeConfig): GigaSpace = {
    clusterProxyCache.getOrElseUpdate(ieConfig, GridProxyUtils.createGridProxy(ieConfig))
  }

  def clusteredCacheSize(): Int = clusterProxyCache.size()

  def getOrCreateDirect(partition: InsightEdgePartition, ieConfig: InsightEdgeConfig): GigaSpace = {
    directProxyCache.getOrElseUpdate(directProxyKey(partition), GridProxyUtils.createDirectProxy(partition, ieConfig))
  }

  def directCacheSize(): Int = directProxyCache.size()

  protected def directProxyKey(p: InsightEdgePartition): String = {
    s"${p.hostName}:${p.gridContainerName}}"
  }

}