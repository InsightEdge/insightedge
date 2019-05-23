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
import org.apache.spark.sql.SparkSession
import org.insightedge.spark.context.InsightEdgeConfig
import org.insightedge.spark.implicits.all._


/**
  * Loads DataFrame from Data Grid and prints objects count.
  * converts Pojo with enum to DataFrame
  * @since 14.5
  */
object LoadDataFrame {

  def main(args: Array[String]): Unit = {
    val settings = if (args.length > 0) args else Array("spark://127.0.0.1:7077", sys.env("INSIGHTEDGE_SPACE_NAME"))
    if (settings.length != 2) {
      System.err.println("Usage: LoadRdd <spark master url> <space name>")
      System.exit(1)
    }
    val Array(master, space) = settings
    val config = InsightEdgeConfig(space)
    val sc = new SparkContext(new SparkConf().setAppName("example-load-df").setMaster(master).setInsightEdgeConfig(config))

    val spark = SparkSession.builder
            .appName("example-load-df")
            .master(master)
            .insightEdgeConfig(config)
            .getOrCreate()

    writeInitialDataToSpace(spark)

    readDataFromSpace(spark)

    sc.stopInsightEdgeContext()
    spark.stopInsightEdgeContext()
  }

  private def readDataFromSpace(spark: SparkSession): Unit = {
    val df = spark.read.grid[Person]
    df.printSchema()
    df.show()

    println(s"Person DF count: ${df.count()}")
    val c1 = df.count()
    assert(c1 == 3, "count should equal to 3")

    val filteredDf = df.filter(df("country.name").equalTo("ISRAEL"))
    filteredDf.show()

    println(s"Filtered Person DF count: ${filteredDf.count()}")
    val c2 = filteredDf.count()
    assert(c2 == 1, "count should equal to 1")
  }

  private def writeInitialDataToSpace(spark: SparkSession) = {
    val p1 = new Person("1", "foo", Country.ISRAEL)
    val p2 = new Person("2", "bar", Country.SWEDEN)
    val p3 = new Person("3", "zoo", Country.FRANCE)

    spark.sparkContext.grid.write(p1)
    spark.sparkContext.grid.write(p2)
    spark.sparkContext.grid.write(p3)
  }
}
