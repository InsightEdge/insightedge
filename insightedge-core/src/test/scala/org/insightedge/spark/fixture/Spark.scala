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

import org.apache.spark.sql.{SQLContext, SparkSession}
import org.apache.spark.{SparkConf, SparkContext}
import org.insightedge.spark.implicits.basic._
import org.scalatest._

/**
  * Suite mixin that starts and stops Spark before and after each test
  *
  * @author Oleksiy_Dyagilev
  */
trait Spark extends fixture.FlatSpec {
  self: Suite with IEConfig with InsightEdge =>

  case class FixtureParam(spark: SparkSession, sc: SparkContext)

  override def withFixture(test: OneArgTest): Outcome = {
    println("Before create Spark... ")
    val spark = createSpark()
    val sc = spark.sparkContext
    val theFixture = FixtureParam(spark, sc)
    try {
      println("Before invoking test... ")
      withFixture(test.toNoArgTest(theFixture))
    } finally{
      println("Before stopping Insight Edge... ")
      spark.stopInsightEdgeContext()
    }
  }

  def createSpark(): SparkSession = {
    SparkSession
      .builder()
      .appName("insightedge-test")
      .master("local[2]")
      .insightEdgeConfig(ieConfig)
      .getOrCreate()
  }
}