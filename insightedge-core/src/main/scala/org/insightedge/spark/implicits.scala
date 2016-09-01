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

package org.insightedge.spark

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.insightedge.{DataFrameImplicits, GeospatialImplicits}
import org.apache.spark.{SparkConf, SparkContext}
import org.insightedge.spark.context.{InsightEdgeConfig, InsightEdgeSparkContext}
import org.insightedge.spark.ml.MLImplicits
import org.insightedge.spark.mllib.MLlibImplicits
import org.insightedge.spark.rdd.InsightEdgeRDDFunctions
import org.insightedge.spark.streaming.StreamingImplicits
import org.insightedge.spark.utils.LocalCache

import scala.language.implicitConversions
import scala.reflect.ClassTag

/**
  * Enables Spark with InsightEdge connector API
  *
  * @author Oleksiy_Dyagilev
  */
object implicits {

  object basic extends BasicImplicits

  object streaming extends StreamingImplicits

  object mllib extends MLlibImplicits

  object ml extends MLImplicits

  object dataframe extends DataFrameImplicits

  object geospatial extends GeospatialImplicits

  object all extends BasicImplicits
    with MLlibImplicits
    with MLImplicits
    with StreamingImplicits
    with DataFrameImplicits
    with GeospatialImplicits


  /** this is to not create a new instance of InsightEdgeSparkContext every time implicit conversion fired **/
  private val insightEdgeSparkContextCache = new LocalCache[SparkContext, InsightEdgeSparkContext]()

  trait BasicImplicits {

    implicit def insightEdgeSparkContext(sc: SparkContext): InsightEdgeSparkContext = {
      insightEdgeSparkContextCache.getOrElseUpdate(sc, new InsightEdgeSparkContext(sc))
    }

    implicit def saveToDataGridExtension[R: ClassTag](rdd: RDD[R]): InsightEdgeRDDFunctions[R] = {
      new InsightEdgeRDDFunctions[R](rdd)
    }

    implicit class SparkConfExtension(sparkConf: SparkConf) {
      def setInsightEdgeConfig(ieConfig: InsightEdgeConfig): SparkConf = {
        ieConfig.populateSparkConf(sparkConf)
        sparkConf
      }
    }

  }

}

