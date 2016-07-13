package com.gigaspaces.spark.ml

import com.gigaspaces.spark.fixture.{GigaSpaces, GsConfig, Spark}
import com.gigaspaces.spark.implicits.all._
import org.apache.spark.SparkContext
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.ml.feature.{HashingTF, Tokenizer}
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.sql.{Row, SQLContext}
import org.scalatest.FunSpec


/**
  * @author Oleksiy_Dyagilev
  */
class GigaSpacesMlSpec extends FunSpec with GsConfig with GigaSpaces with Spark {

  it("should store and load ML Pipeline Model") {
    val training = sql.createDataFrame(Seq(
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
    val testDf = sql.createDataFrame(testSeq).toDF("id", "text")

    val predictions = model.transform(testDf)
      .select("id", "text", "probability", "prediction")
      .collect()

    def printPredictions(predictions: Array[Row]) = {
      predictions.foreach { case Row(id: Long, text: String, prob: Vector, prediction: Double) =>
        println(s"($id, $text) --> prob=$prob, prediction=$prediction")
      }
    }

    printPredictions(predictions)

    // stop Spark context and create it again to make sure we can load in another context
    sc.stopGigaSpacesContext()
    sc = new SparkContext(createSparkConf())
    sql = new SQLContext(sc)

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
