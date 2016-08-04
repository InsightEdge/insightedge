package org.insightedge.spark.context

import org.apache.spark.SparkConf
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
  }

}


object InsightEdgeConfig {

  /** keys used in SparkConf, the 'spark' prefix is mandatory, otherwise they are not propagated to executors **/
  private val SpaceNameKey = "spark.insightedge.space.name"
  private val LookupGroupKey = "spark.insightedge.space.lookup.group"
  private val LookupLocatorKey = "spark.insightedge.space.lookup.locator"

  /**
    * instantiate `InsightEdgeConfig` from `SparkConf`
    */
  def fromSparkConf(sparkConf: SparkConf): InsightEdgeConfig = {
    val gsConfig = for {
      spaceName <- sparkConf.getOption(SpaceNameKey)
      lookupGroups = sparkConf.getOption(LookupGroupKey)
      lookupLocator = sparkConf.getOption(LookupLocatorKey)
    } yield InsightEdgeConfig(spaceName, lookupGroups, lookupLocator)

    gsConfig.getOrElse(throw new RuntimeException("Unable to read InsightEdgeConfig from SparkConf. Use sparkConf.setInsightEdgeConfig(gsConfig) to set config"))
  }


}