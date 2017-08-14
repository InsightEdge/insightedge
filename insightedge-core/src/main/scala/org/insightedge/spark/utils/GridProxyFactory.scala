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
import org.openspaces.core.{GigaSpace, GigaSpaceConfigurer}
import org.openspaces.core.space.SpaceProxyConfigurer

/**
 * Ensures single GigaSpaces instance per JVM (Spark worker)
 *
 * @author Oleksiy_Dyagilev
 */
object GridProxyFactory extends Logging  {

  System.setProperty("com.gs.protectiveMode.ambiguousQueryRoutingUsage", "false")

  private val clusterProxyCache = new LocalCache[InsightEdgeConfig, GigaSpace]()

  def getOrCreateClustered(ieConfig: InsightEdgeConfig): GigaSpace = {
    clusterProxyCache.getOrElseUpdate(ieConfig, createSpaceProxy(ieConfig))
  }

  def clusteredCacheSize(): Int = clusterProxyCache.size()

  private def createSpaceProxy(ieConfig: InsightEdgeConfig) : GigaSpace = {
    profileWithInfo("createSpaceProxy") {
      val spaceConfigurer = new SpaceProxyConfigurer(ieConfig.spaceName)
      ieConfig.lookupGroups.foreach(spaceConfigurer.lookupGroups)
      ieConfig.lookupLocators.foreach(spaceConfigurer.lookupLocators)
      new GigaSpaceConfigurer(spaceConfigurer).create()
    }
  }

  private def profileWithInfo[T](message: String)(block: => T): T = Profiler.profile(message)(logInfo(_))(block)
}