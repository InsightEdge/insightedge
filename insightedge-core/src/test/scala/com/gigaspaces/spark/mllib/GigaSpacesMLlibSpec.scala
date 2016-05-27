package com.gigaspaces.spark.mllib

import com.gigaspaces.spark.fixture.{GigaSpaces, GsConfig, Spark}
import com.gigaspaces.spark.implicits._
import com.gigaspaces.spark.mllib.implicits._
import org.apache.commons.io.FileUtils
import org.apache.spark.mllib.tree.DecisionTree
import org.apache.spark.mllib.tree.model.{DecisionTreeModel, GradientBoostedTreesModel}
import org.apache.spark.mllib.util.MLUtils
import org.scalatest._


class GigaSpacesMLlibSpec extends FunSpec with GsConfig with GigaSpaces with Spark {

  it("should successfully store MLlib model to Data Grid") {
    val model = createModel()
    model.saveToGrid(sc, "model")
    val loadedModel = sc.loadMLModel[DecisionTreeModel]("model").get
    assert(model.depth === loadedModel.depth)
    assert(model.numNodes === loadedModel.numNodes)
  }

  it("should fail if there is no such MLlib model int DataGrid") {
    val model = createModel()
    model.saveToGrid(sc, "model")
    assert(None === sc.loadMLModel[DecisionTreeModel]("model2"))
    assert(None === sc.loadMLModel[GradientBoostedTreesModel]("model"))
  }

  private def createModel() = {
    val path = FileUtils.getFile("src", "test", "resources", "data", "sample_libsvm_data.txt").getAbsolutePath
    val data = MLUtils.loadLibSVMFile(sc, path)
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
