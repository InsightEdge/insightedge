package com.gigaspaces.spark.utils

import com.gigaspaces.spark.context.GigaSpacesConfig
import com.gigaspaces.spark.impl.GigaSpacesPartition
import org.openspaces.core.GigaSpace

/**
 * Ensures single GigaSpace instance per JVM (Spark worker)
 *
 * @author Oleksiy_Dyagilev
 */
object GigaSpaceFactory {

  private val clusterProxyCache = new LocalCache[GigaSpacesConfig, GigaSpace]()
  private val directProxyCache = new LocalCache[String, GigaSpace]()

  def getOrCreateClustered(gsConfig: GigaSpacesConfig): GigaSpace = {
    clusterProxyCache.getOrElseUpdate(gsConfig, GigaSpaceUtils.createGigaSpace(gsConfig))
  }

  def clusteredCacheSize(): Int = clusterProxyCache.size()

  def getOrCreateDirect(partition: GigaSpacesPartition, gsConfig: GigaSpacesConfig): GigaSpace = {
    directProxyCache.getOrElseUpdate(directProxyKey(partition), GigaSpaceUtils.createDirectProxy(partition, gsConfig))
  }

  def directCacheSize(): Int = directProxyCache.size()

  protected def directProxyKey(p: GigaSpacesPartition): String = {
    s"${p.hostName}:${p.gigaSpaceContainerName}}"
  }

}