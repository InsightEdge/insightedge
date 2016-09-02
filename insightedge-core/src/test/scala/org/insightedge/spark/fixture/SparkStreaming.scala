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

package org.insightedge.spark.fixture

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.insightedge.spark.implicits.basic._
import org.scalatest.concurrent.Eventually
import org.scalatest.time.Span
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite, time}

/**
  * @author Oleksiy_Dyagilev
  */
trait SparkStreaming extends BeforeAndAfterAll with BeforeAndAfterEach {
  self: Suite with IEConfig =>

  var ssc: StreamingContext = _

  override protected def beforeEach() = {
    super.beforeEach()

    val sparkConf = new SparkConf()
      .setAppName("insightedge-streaming-test")
      .setMaster("local[2]")
      .setInsightEdgeConfig(ieConfig)

    ssc = new StreamingContext(sparkConf, Seconds(1))
  }

  override protected def afterEach() = {
    ssc.stop()
    ssc.sparkContext.stopInsightEdgeContext()
    super.afterEach()
  }

  def timeout(sec: Int) = Eventually.PatienceConfig(Span(sec, time.Seconds))

}
