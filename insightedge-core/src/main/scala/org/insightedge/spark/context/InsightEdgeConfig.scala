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

package org.insightedge.spark.context

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.insightedge.spark.context.InsightEdgeConfig._

/**
  * @author Oleksiy_Dyagilev
  */
case class InsightEdgeConfig(
                             spaceName: String,
                             lookupGroups: Option[String] = None,
                             lookupLocators: Option[String] = None) {

  /**
    * Populate SparkConf with this config values
    */
  def populateSparkConf(sparkConf: SparkConf) = {
    sparkConf.set(SpaceNameKey, spaceName)
    lookupGroups.foreach(group => sparkConf.set(LookupGroupKey, group))
    lookupLocators.foreach(locator => sparkConf.set(LookupLocatorKey, locator))
    sparkConf
  }

  def populateSparkSessionBuilder(builder: SparkSession.Builder) = {
    builder.config(SpaceNameKey, spaceName)
    lookupGroups.foreach(group => builder.config(LookupGroupKey, group))
    lookupLocators.foreach(locator => builder.config(LookupLocatorKey, locator))
  }

}


object InsightEdgeConfig {

  val INSIGHTEDGE_SPACE_NAME = "INSIGHTEDGE_SPACE_NAME"
  val INSIGHTEDGE_SPACE_NAME_DEFAULT = "demo"
  val SPARK_MASTER_LOCAL_URL_DEFAULT = "spark://127.0.0.1:7077"

  /** keys used in SparkConf, the 'spark' prefix is mandatory, otherwise they are not propagated to executors **/
  private val SpaceNameKey = "spark.insightedge.space.name"
  private val SpaceManagerKey = "spark.insightedge.space.manager"
  private val LookupGroupKey = "spark.insightedge.space.lookup.group"
  private val LookupLocatorKey = "spark.insightedge.space.lookup.locator"

  /**
    * instantiate `InsightEdgeConfig` from `SparkConf`
    */
  def fromSparkConf(sparkConf: SparkConf): InsightEdgeConfig = {
    val ieConfig = for {
      spaceName <- sparkConf.getOption(SpaceNameKey).orElse(Option(sys.env.getOrElse(InsightEdgeConfig.INSIGHTEDGE_SPACE_NAME,
        InsightEdgeConfig.INSIGHTEDGE_SPACE_NAME_DEFAULT)))
      spaceManagerKey = sparkConf.getOption(SpaceManagerKey).flatMap(k => Option(k.concat("-insightedge-manager-hs")))
      lookupGroups = sparkConf.getOption(LookupGroupKey)
      lookupLocator = spaceManagerKey.orElse(sparkConf.getOption(LookupLocatorKey))
    } yield InsightEdgeConfig(spaceName, lookupGroups, lookupLocator)

    ieConfig.getOrElse(throw new RuntimeException("Unable to read InsightEdgeConfig from SparkConf. Use sparkConf.setInsightEdgeConfig(ieConfig) to set config"))
  }


}