package com.gigaspaces.spark.utils

import com.gigaspaces.spark.context.GigaSpacesConfig
import org.scalatest.Suite

/**
  * Suite mixin with GigaSpacesConfig
  *
  * @author Oleksiy_Dyagilev
  */
trait GsConfig {
  self: Suite =>

  val gsConfig = GigaSpacesConfig("test-space", Some("spark"), Some("localhost:4174"))

}
