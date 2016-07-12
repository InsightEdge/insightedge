package com.gigaspaces.spark.ml

import com.gigaspaces.spark.fixture.{GigaSpaces, GsConfig, Spark}
import org.scalatest.FunSpec

import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.feature.{HashingTF, Tokenizer}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.sql.Row

import com.gigaspaces.spark.implicits.all._


/**
  * @author Oleksiy_Dyagilev
  */
class GigaSpacesMlSpec extends FunSpec with GsConfig with GigaSpaces with Spark {

  it("should store ML model to Data Grid") {
    val training = sql.createDataFrame(Seq(
      (0L, "a b c d e spark", 1.0),
      (1L, "b d", 0.0),
      (2L, "spark f g h", 1.0),
      (3L, "hadoop mapreduce", 0.0)
    )).toDF("id", "text", "label")

    // Configure an ML pipeline, which consists of three stages: tokenizer, hashingTF, and lr.
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

    // Fit the pipeline to training documents.
    val model = pipeline.fit(training)

    val modelHolder = PipelineModelHolder("abc", model)
    sc.gigaSpace.write(modelHolder)

    val loadedModel = sc.gigaSpace.read(PipelineModelHolder("abc", null)).pipelineModel



    // now we can optionally save the fitted pipeline to disk
//    model.save("/tmp/spark-logistic-regression-model")

    // we can also save this unfit pipeline to disk
//    pipeline.save("/tmp/unfit-lr-model")

    // and load it back in during production
//    val sameModel = PipelineModel.load("/tmp/spark-logistic-regression-model")

    // Prepare test documents, which are unlabeled (id, text) tuples.
    val test = sql.createDataFrame(Seq(
      (4L, "spark i j k"),
      (5L, "l m n"),
      (6L, "mapreduce spark"),
      (7L, "apache hadoop")
    )).toDF("id", "text")

    // Make predictions on test documents.
    loadedModel.transform(test)
      .select("id", "text", "probability", "prediction")
      .collect()
      .foreach { case Row(id: Long, text: String, prob: Vector, prediction: Double) =>
        println(s"($id, $text) --> prob=$prob, prediction=$prediction")
      }
  }

}
