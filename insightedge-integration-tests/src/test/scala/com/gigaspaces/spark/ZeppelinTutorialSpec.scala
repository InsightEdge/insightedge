package com.gigaspaces.spark

import com.gigaspaces.spark.fixture.InsightEdgeDocker
import org.scalatest.FlatSpec
import org.scalatest.concurrent.Eventually._

import scala.concurrent.Await

/**
  * Zeppelin Tutorial tests
  *
  * @author Oleksiy_Dyagilev
  */
class ZeppelinTutorialSpec extends FlatSpec with InsightEdgeDocker {

  "Zeppelin" should "have InsightEdge Basics tutorial" in {
    val resp = wsClient.url(s"http://localhost:32829/api/notebook").get()
    val res = Await.result(resp, 1.second)
  }

}
