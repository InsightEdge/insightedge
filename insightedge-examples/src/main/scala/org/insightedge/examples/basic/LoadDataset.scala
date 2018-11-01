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
import org.insightedge.spark.implicits.all._

/**
  * Loads Products from Data Grid as DataFrame and runs filtering.
  */
object LoadDataset {

  def main(args: Array[String]): Unit = {
    val initConfig = InsightEdgeConfig.fromSparkConf(new SparkConf())

    //args: <spark master url> <space name>
    val settings =  if (args.length > 0) args
    else Array( new SparkConf().get("spark.master", InsightEdgeConfig.SPARK_MASTER_LOCAL_URL_DEFAULT),
      initConfig.spaceName)

    if (settings.length != 2) {
      System.err.println("Usage: LoadDataset <spark master url> <space name>")
      System.exit(1)
    }
    val Array(master, space) = settings
    val ieConfig = initConfig.copy(spaceName = space)
    val spark = SparkSession.builder
      .appName("example-load-dataset")
      .master(master)
      .insightEdgeConfig(ieConfig)
      .getOrCreate()

    import spark.implicits._
    val ds = spark.read.grid[Product].as[Product]
    ds.printSchema()
    val count = ds.filter( o => o.quantity < 5).count()
    println(s"Number of products with quantity < 5: $count")
    spark.stopInsightEdgeContext()
  }

}
