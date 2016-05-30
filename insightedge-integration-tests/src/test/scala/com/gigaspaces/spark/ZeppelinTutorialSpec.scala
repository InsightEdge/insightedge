package com.gigaspaces.spark

import com.gigaspaces.spark.fixture.InsightEdgeDocker
import com.gigaspaces.spark.utils.RestUtils._
import org.scalatest.FlatSpec
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue}
import org.scalatest.concurrent.Eventually._

import scala.concurrent.duration._

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

    def bindInterpreter() = {
      val bindUrl = s"$zeppelinUrl/api/notebook/interpreter/bind/$TutorialId"
      val interpreters = wsClient.url(bindUrl).get()
      val interpreterIds = jsonBody(interpreters) \\ "id"
      val bindResp = wsClient.url(bindUrl).put(JsArray(interpreterIds))
      jsonBody(bindResp)
    }

    bindInterpreter()

    val notebookJob = s"$zeppelinUrl/api/notebook/job/$TutorialId"

    val notebookBeforeRun = jsonBody(
      wsClient.url(notebookJob).get()
    )

    val paragraphsCount = (notebookBeforeRun \ "body" \\ "status").size
    println(s"paragraphsCount $paragraphsCount")

    assert((notebookBeforeRun \\ "status") contains JsString("READY"))

    // run notebook
    jsonBody(
      wsClient.url(notebookJob).post(JsObject(Seq()))
    )

    // eventually all paragraphs should be in FINISHED status
    eventually(timeout(3.minutes), interval(5.second))  {
      val jobStatus = jsonBody(
        wsClient.url(notebookJob).get()
      )
      val finished = (jobStatus \ "body" \\ "status").collect { case s@JsString("FINISHED") => s }
      val finishedCount = finished.size
      println(s"finished count $finishedCount")
      assert(finishedCount == paragraphsCount)
    }
  }


}
