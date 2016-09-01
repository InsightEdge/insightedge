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

package org.insightedge.spark.mllib

import org.apache.commons.io.FileUtils
import org.apache.spark.SparkContext
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.tree.DecisionTree
import org.apache.spark.mllib.tree.model.{DecisionTreeModel, GradientBoostedTreesModel}
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.rdd.RDD
import org.insightedge.spark.fixture.{InsightEdge, IEConfig, Spark}
import org.insightedge.spark.implicits.basic._
import org.insightedge.spark.implicits.mllib._
import org.scalatest._


class InsightEdgeMLlibSpec extends FunSpec with IEConfig with InsightEdge with Spark {

  it("should successfully store DecisionTreeModel MLlib model to Data Grid") {
    val testDataRDD = loadDataFromFile().map(_.features)
    val testDataArray = testDataRDD.collect()
    val model = createDecisionTreeModel()
    val prediction = model.predict(testDataRDD).collect()
    model.saveToGrid(sc, "model")

    // stop Spark context and create it again to make sure we can load in another context
    sc.stopInsightEdgeContext()

    sc = new SparkContext(createSparkConf())

    val loadedModel = sc.loadMLInstance[DecisionTreeModel]("model").get
    assert(model.depth === loadedModel.depth)
    assert(model.numNodes === loadedModel.numNodes)
    assert(model.algo === loadedModel.algo)

    // check that it predicts the same values
    val loadedModelPrediction = loadedModel.predict(sc.parallelize(testDataArray)).collect()
    assert(prediction sameElements loadedModelPrediction)
  }

  it("should load nothing if there is no such MLlib model int DataGrid") {
    val model = createDecisionTreeModel()
    model.saveToGrid(sc, "model")
    assert(None === sc.loadMLInstance[DecisionTreeModel]("model2"))
    assert(None === sc.loadMLInstance[GradientBoostedTreesModel]("model"))
  }

  private def loadDataFromFile(): RDD[LabeledPoint] = {
    val path = FileUtils.getFile("src", "test", "resources", "data", "sample_libsvm_data.txt").getAbsolutePath
    MLUtils.loadLibSVMFile(sc, path)
  }

  private def createDecisionTreeModel(): DecisionTreeModel = {
    val data = loadDataFromFile()
    val splits = data.randomSplit(Array(0.7, 0.3))
    val (trainingData, testData) = (splits(0), splits(1))
    val numClasses = 2
    val categoricalFeaturesInfo = Map[Int, Int]()
    val impurity = "gini"
    val maxDepth = 5
    val maxBins = 32
    DecisionTree.trainClassifier(trainingData, numClasses, categoricalFeaturesInfo,
      impurity, maxDepth, maxBins)
  }

}
