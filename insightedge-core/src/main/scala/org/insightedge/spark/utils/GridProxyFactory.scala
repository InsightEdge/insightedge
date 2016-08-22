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