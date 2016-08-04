package org.insightedge.spark.fixture

import org.insightedge.spark.context.InsightEdgeConfig
import org.scalatest.Suite

/**
  * Suite mixin with InsightEdgeConfig
  *
  * @author Oleksiy_Dyagilev
  */
trait IEConfig {
  self: Suite =>

  val ieConfig = InsightEdgeConfig("test-space", Some("spark"), Some("localhost:4174"))

}
