package com.gigaspaces.spark.context

import com.gigaspaces.spark.context.GigaSpacesConfig._
import org.apache.spark.SparkConf

/**
  * @author Oleksiy_Dyagilev
  */
case class GigaSpacesConfig(
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


object GigaSpacesConfig {

  /** keys used in SparkConf, the 'spark' prefix is mandatory, otherwise they are not propagated to executors **/
  private val SpaceNameKey = "spark.gigaspaces.space.name"
  private val LookupGroupKey = "spark.gigaspaces.space.lookup.group"
  private val LookupLocatorKey = "spark.gigaspaces.space.lookup.locator"

  /**
    * instantiate `GigaSpacesConfig` from `SparkConf`
    */
  def fromSparkConf(sparkConf: SparkConf): GigaSpacesConfig = {
    val gsConfig = for {
      spaceName <- sparkConf.getOption(SpaceNameKey)
      lookupGroups = sparkConf.getOption(LookupGroupKey)
      lookupLocator = sparkConf.getOption(LookupLocatorKey)
    } yield GigaSpacesConfig(spaceName, lookupGroups, lookupLocator)

    gsConfig.getOrElse(throw new RuntimeException("Unable to read GigaSpacesConfig from SparkConf. Use sparkConf.setGigaSpaceConfig(gsConfig) to set config"))
  }


}