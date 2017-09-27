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

package org.insightedge.spark.jobs

import org.apache.spark.{SparkConf, SparkContext}
import org.insightedge.spark.context.InsightEdgeConfig
import org.insightedge.spark.implicits.basic._

/**
  * Loads Product RDD from Data Grid and prints objects count.
  */
object LoadRdd {

  def main(args: Array[String]): Unit = {
    val settings = if (args.length > 0) args else Array("spark://127.0.0.1:7077", sys.env("INSIGHTEDGE_SPACE_NAME"))
    if (settings.length != 2) {
      System.err.println("Usage: LoadRdd <spark master url> <space name>")
      System.exit(1)
    }
    val Array(master, space) = settings
    val config = InsightEdgeConfig(space)
    val sc = new SparkContext(new SparkConf().setAppName("example-load-rdd").setMaster(master).setInsightEdgeConfig(config))
    Thread.sleep(30000)
    val rdd = sc.gridRdd[Product]()
    println(s"Products RDD count: ${rdd.count()}")
    sc.stopInsightEdgeContext()
  }

}
