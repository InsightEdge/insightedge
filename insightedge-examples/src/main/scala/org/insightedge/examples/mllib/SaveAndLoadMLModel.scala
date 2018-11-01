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

package org.insightedge.examples.mllib

import org.apache.spark.mllib.clustering.{KMeans, KMeansModel}
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}
import org.insightedge.spark.context.InsightEdgeConfig
import org.insightedge.spark.implicits.all._

/**
  * Saves/reloads the ML model to/from Data Grid.
  */
object SaveAndLoadMLModel {

  def main(args: Array[String]): Unit = {
    val initConfig = InsightEdgeConfig.fromSparkConf(new SparkConf())

    //args: <spark master url> <space name>
    val settings =  if (args.length > 0) args
    else Array( new SparkConf().get("spark.master", InsightEdgeConfig.SPARK_MASTER_LOCAL_URL_DEFAULT),
      initConfig.spaceName)

    if (settings.length != 2) {
      System.err.println("Usage: SaveAndLoadMLModel <spark master url> <space name>")
      System.exit(1)
    }
    val Array(master, space) = settings
    val ieConfig = initConfig.copy(spaceName = space)
    val spark = SparkSession.builder
      .appName("example-mllib")
      .master(master)
      .insightEdgeConfig(ieConfig)
      .getOrCreate()
    val sc = spark.sparkContext

    val modelName = "decisionTreeModel"
    val model = createModel(sc)
    println(s"Saving ${model.getClass.getSimpleName} to the datagrid")
    model.saveToGrid(sc, modelName)
    println(s"Loading $modelName from the datagrid")
    val loadedModel = sc.loadMLInstance[KMeansModel](modelName).get
    println(s"Model ${loadedModel.getClass.getSimpleName} is loaded")
    spark.stopInsightEdgeContext()
  }

  private def createModel(sc: SparkContext) = {
    val vectors = List(Vectors.dense(1.0, 1.0, 3.0), Vectors.dense(2.0, 0.0, 1.0), Vectors.dense(2.0, 1.0, 0.0))
    val rdd = sc.parallelize(vectors)
    val k = 2
    val maxIterations = 100
    KMeans.train(rdd, k, maxIterations)
  }

}
