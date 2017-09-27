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

import scala.util.Random

/**
  * Generates 100000 Products, converts to Spark RDD and saves to Data Grid. Products have fixed IDs.
  */
object SaveRdd {

  def main(args: Array[String]): Unit = {
    val settings = if (args.length > 0) args else Array("spark://127.0.0.1:7077", sys.env("INSIGHTEDGE_SPACE_NAME"))
    if (settings.length != 2) {
      System.err.println("Usage: SaveRdd <spark master url> <space name>")
      System.exit(1)
    }
    val Array(master, space) = settings
    val config = InsightEdgeConfig(space)
    val sc = new SparkContext(new SparkConf().setAppName("example-save-rdd").setMaster(master).setInsightEdgeConfig(config))

    val products = (1 to 1000000).map { i => Product(i, "Description of product " + i, Random.nextInt(10), Random.nextBoolean()) }
    println(s"Saving ${products.size} products RDD to the space")
    val rdd = sc.parallelize(products, 9)
    rdd.saveToGrid()
    sc.stopInsightEdgeContext()
  }

}
