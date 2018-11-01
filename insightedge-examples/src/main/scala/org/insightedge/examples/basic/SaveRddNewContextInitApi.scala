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

package org.insightedge.examples.basic

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.insightedge.spark.context.InsightEdgeConfig
import org.insightedge.spark.implicits.basic._
import org.openspaces.core.GigaSpaceConfigurer
import org.openspaces.core.space.EmbeddedSpaceConfigurer

import scala.util.Random

/**
  * Generates 100000 Products, converts to Spark RDD and saves to Data Grid. Products have fixed IDs.
  */
object SaveRddNewContextInitApi {

  def main(args: Array[String]): Unit = {
    val settings = if (args.length > 0) args else Array( new SparkConf().get("spark.master", InsightEdgeConfig.SPARK_MASTER_LOCAL_URL_DEFAULT),
      sys.env.getOrElse(InsightEdgeConfig.INSIGHTEDGE_SPACE_NAME, InsightEdgeConfig.INSIGHTEDGE_SPACE_NAME_DEFAULT))

    if (settings.length != 2) {
      System.err.println("Usage: SaveRdd <spark master url> <space name>")
      System.exit(1)
    }

    val Array(master, space) = settings
    val config = InsightEdgeConfig(space)
    val spark = SparkSession.builder
      .appName("example-save-rdd-new-context-init-api")
      .master(master)
      .getOrCreate()

    //initializing the insightedge context via the spark context
    spark.sparkContext.initializeInsightEdgeContext(config)

    val sc = spark.sparkContext

    val productsNum = 100
    println(s"Saving $productsNum products RDD to the space")
    val rdd = sc.parallelize(1 to productsNum).map { i =>
      Product(i, "Description of product " + i, Random.nextInt(10), Random.nextBoolean())
    }

    rdd.saveToGrid()

    sc.stopInsightEdgeContext()
  }

}

