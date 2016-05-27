package com.gigaspaces.spark

import com.gigaspaces.spark.fixture.InsightEdgeDocker
import com.gigaspaces.spark.utils.RestUtils._
import org.scalatest.FlatSpec
import play.api.libs.json.JsString

/**
  * Zeppelin Tutorial tests
  *
  * @author Oleksiy_Dyagilev
  */
class ZeppelinTutorialSpec extends FlatSpec with InsightEdgeDocker {

  val TutorialId = "INSIGHTEDGE-BASIC"

  "Zeppelin" should "have InsightEdge Basics tutorial" in {
    val resp = wsClient.url(s"$zeppelinUrl/api/notebook").get()
    val notebookIds = jsonBody(resp) \\ "id"

    assert(notebookIds.contains(JsString(TutorialId)))
  }

  it should "be possible to run InsightEdge Basics tutorial" in {
//    val resp = wsClient.url(s"$zeppelinUrl/api/notebook").get()
//    val notebookIds = jsonBody(resp) \\ "id"
//
//    assert(notebookIds.contains(JsString("INSIGHTEDGE-BASIC")))
  }



}
