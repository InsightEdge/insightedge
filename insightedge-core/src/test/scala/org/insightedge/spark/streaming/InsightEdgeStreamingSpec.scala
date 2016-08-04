package org.insightedge.spark.streaming

import org.insightedge.spark.fixture.{InsightEdge, IEConfig}
import org.insightedge.spark.implicits
import implicits.basic._
import implicits.streaming._
import org.insightedge.spark.rdd.Data
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.InputDStream
import org.insightedge.spark.fixture.SparkStreaming
import org.scalatest.FunSpec
import org.scalatest.concurrent.Eventually._

import scala.collection.mutable
import scala.util.Random

/**
  * @author Oleksiy_Dyagilev
  */
class InsightEdgeStreamingSpec extends FunSpec with IEConfig with InsightEdge with SparkStreaming {

  it("should save single object from Spark driver") {
    val sc = ssc.sparkContext

    val stream: InputDStream[String] = ssc.queueStream(stringQueue(sc))

    stream.foreachRDD { rdd =>
      val str = rdd.first()
      val data = new Data(Random.nextLong(), str)
      sc.saveToGrid(data)
    }

    ssc.start()

    eventually {
      val savedData = spaceProxy.readMultiple(dataQuery())
      assert(savedData.length == 1)
    }(timeout(2))
  }

  it("should save multiple objects from Spark driver") {
    val sc = ssc.sparkContext

    val stream: InputDStream[String] = ssc.queueStream(stringQueue(sc))

    stream.foreachRDD { rdd =>
      val stringsArray = rdd.take(2)
      val datas = stringsArray.map(str => new Data(Random.nextLong(), str))
      sc.saveMultipleToGrid(datas)
    }

    ssc.start()

    eventually {
      val savedData = spaceProxy.readMultiple(dataQuery())
      assert(savedData.nonEmpty)
    }(timeout(2))
  }

  it("should save DStream") {
    val sc = ssc.sparkContext

    val stream: InputDStream[String] = ssc.queueStream(stringQueue(sc))

    stream
      .map(str => new Data(Random.nextLong(), str))
      .saveToGrid()

    ssc.start()

    eventually {
      val savedData = spaceProxy.readMultiple(dataQuery())
      assert(savedData.nonEmpty)
    }(timeout(2))
  }

  def stringQueue(sc: SparkContext) = {
    val q = mutable.Queue[RDD[String]]()
    q += sc.makeRDD(Seq("aa", "bb", "cc"))
    q += sc.makeRDD(Seq("dd", "ee", "ff"))
    q
  }

}
