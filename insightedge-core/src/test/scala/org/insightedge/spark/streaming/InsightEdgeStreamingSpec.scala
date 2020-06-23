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

package org.insightedge.spark.streaming

import org.insightedge.spark.implicits
import implicits.basic._
import implicits.streaming._
import org.insightedge.spark.rdd.Data
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.InputDStream
import org.insightedge.spark.fixture.InsightEdgeStreaming
import org.scalatest.{FunSpec, fixture}
import org.scalatest.concurrent.Eventually._

import scala.collection.mutable
import scala.util.Random

/**
  * @author Oleksiy_Dyagilev
  */
class InsightEdgeStreamingSpec extends fixture.FlatSpec with InsightEdgeStreaming {

  it should "should save single object from Spark driver" in { ie =>
    val sc = ssc.sparkContext

    val stream: InputDStream[String] = ssc.queueStream(stringQueue(sc))

    stream.foreachRDD { rdd =>
      val str = rdd.first()
      val data = new Data(Random.nextLong(), str)
      sc.saveToGrid(data)
    }

    ssc.start()

    eventually {
      val savedData = ie.spaceProxy.readMultiple(dataQuery())
      assert(savedData.length == 1)
    }(timeout(2), org.scalactic.source.Position("InsightEdgeStreamingSpec", "", 0))
  }

  it should "save multiple objects from Spark driver" in { ie =>
    val sc = ssc.sparkContext

    val stream: InputDStream[String] = ssc.queueStream(stringQueue(sc))

    stream.foreachRDD { rdd =>
      val stringsArray = rdd.take(2)
      val datas = stringsArray.map(str => new Data(Random.nextLong(), str))
      sc.saveMultipleToGrid(datas)
    }

    ssc.start()

    eventually {
      val savedData = ie.spaceProxy.readMultiple(dataQuery())
      assert(savedData.nonEmpty)
    }(timeout(2), org.scalactic.source.Position("InsightEdgeStreamingSpec", "", 0))
  }

  it should "save DStream" in { ie =>
    val sc = ssc.sparkContext

    val stream: InputDStream[String] = ssc.queueStream(stringQueue(sc))

    stream
      .map(str => new Data(Random.nextLong(), str))
      .saveToGrid()

    ssc.start()

    eventually {
      val savedData = ie.spaceProxy.readMultiple(dataQuery())
      assert(savedData.nonEmpty)
    }(timeout(2), org.scalactic.source.Position("InsightEdgeStreamingSpec", "", 0))
  }

  def stringQueue(sc: SparkContext) = {
    val q = mutable.Queue[RDD[String]]()
    q += sc.makeRDD(Seq("aa", "bb", "cc"))
    q += sc.makeRDD(Seq("dd", "ee", "ff"))
    q
  }

}
