package com.gigaspaces.spark.mllib

import com.gigaspaces.spark.fixture.{GigaSpaces, GsConfig, Spark}
import com.gigaspaces.spark.implicits.basic._
import com.gigaspaces.spark.implicits.mllib._
import org.apache.commons.io.FileUtils
import org.apache.spark.SparkContext
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.tree.DecisionTree
import org.apache.spark.mllib.tree.model.{DecisionTreeModel, GradientBoostedTreesModel}
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.rdd.RDD
import org.scalatest._


class GigaSpacesMLlibSpec extends FunSpec with GsConfig with GigaSpaces with Spark {

  it("should successfully store MLlib model to Data Grid") {
    val testDataRDD = loadDataFromFile().map(_.features)
    val testDataArray = testDataRDD.collect()
    val model = createModel()
    val prediction = model.predict(testDataRDD).collect()
    model.saveToGrid(sc, "model")

    // stop Spark context and create it again to make sure we can load in another context
    sc.stopGigaSpacesContext()

    sc = new SparkContext(createSparkConf())

    val loadedModel = sc.loadMLModel[DecisionTreeModel]("model").get
    assert(model.depth === loadedModel.depth)
    assert(model.numNodes === loadedModel.numNodes)
    assert(model.algo === loadedModel.algo)

    // check that it predicts the same values
    val loadedModelPrediction = loadedModel.predict(sc.parallelize(testDataArray)).collect()
    assert(prediction sameElements loadedModelPrediction)
  }

  it("should fail if there is no such MLlib model int DataGrid") {
    val model = createModel()
    model.saveToGrid(sc, "model")
    assert(None === sc.loadMLModel[DecisionTreeModel]("model2"))
    assert(None === sc.loadMLModel[GradientBoostedTreesModel]("model"))
  }

  private def loadDataFromFile(): RDD[LabeledPoint] = {
    val path = FileUtils.getFile("src", "test", "resources", "data", "sample_libsvm_data.txt").getAbsolutePath
    MLUtils.loadLibSVMFile(sc, path)
  }

  private def createModel(): DecisionTreeModel = {
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
