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

package org.insightedge.spark.ml

import org.apache.spark.SparkContext
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.feature.{HashingTF, Tokenizer}
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.linalg.DenseVector
import org.apache.spark.sql.{Row, SQLContext}
import org.insightedge.spark.fixture.{IEConfig, InsightEdge, Spark}
import org.insightedge.spark.implicits.all._
import org.scalatest.FunSpec


/**
  * @author Oleksiy_Dyagilev
  */
class InsightEdgeMlSpec extends FunSpec with IEConfig with InsightEdge with Spark {

  it("should store and load ML Pipeline Model (Tokenizer, HashingTF, LogisticRegression)") {
    val training = spark.createDataFrame(Seq(
      (0L, "a b c d e spark", 1.0),
      (1L, "b d", 0.0),
      (2L, "spark f g h", 1.0),
      (3L, "hadoop mapreduce", 0.0)
    )).toDF("id", "text", "label")

    val tokenizer = new Tokenizer()
      .setInputCol("text")
      .setOutputCol("words")
    val hashingTF = new HashingTF()
      .setNumFeatures(1000)
      .setInputCol(tokenizer.getOutputCol)
      .setOutputCol("features")
    val lr = new LogisticRegression()
      .setMaxIter(10)
      .setRegParam(0.01)
    val pipeline = new Pipeline()
      .setStages(Array(tokenizer, hashingTF, lr))

    val model = pipeline.fit(training)

    // Save model to grid
    model.saveToGrid(sc, "testPipelineModel")

    val testSeq = Seq(
      (4L, "spark i j k"),
      (5L, "l m n"),
      (6L, "mapreduce spark"),
      (7L, "apache hadoop")
    )
    val testDf = spark.createDataFrame(testSeq).toDF("id", "text")

    val predictions = model.transform(testDf)
      .select("id", "text", "probability", "prediction")
      .collect()

    def printPredictions(predictions: Array[Row]) = {
      predictions.foreach { row: Row =>
          val id = row.getAs[Long]("id")
          val text = row.getAs[String]("text")
          val probability = row.getAs[DenseVector]("probability")
          val prediction = row.getAs[Double]("prediction")
          println(s"($id, $text) --> prob=$probability, prediction=$prediction")
      }
    }

    printPredictions(predictions)

    // stop Spark context and create it again to make sure we can load in another context
    spark.stopInsightEdgeContext()
    spark = createSpark()
    sc = spark.sparkContext

    // load model from grid
    val loadedModel = sc.loadMLInstance[PipelineModel]("testPipelineModel").get

    val afterLoadPredictions = loadedModel.transform(testDf)
      .select("id", "text", "probability", "prediction")
      .collect()

    printPredictions(afterLoadPredictions)

    // assert it predicts the same
    assert(predictions sameElements afterLoadPredictions)
    assert(model.stages.length == loadedModel.stages.length)
  }

}
