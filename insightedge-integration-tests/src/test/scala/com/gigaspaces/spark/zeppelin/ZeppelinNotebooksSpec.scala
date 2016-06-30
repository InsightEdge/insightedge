package com.gigaspaces.spark.zeppelin

import com.gigaspaces.spark.fixture.InsightedgeDemoModeDocker
import com.gigaspaces.spark.utils.RestUtils._
import org.scalatest.FlatSpec
import org.scalatest.concurrent.Eventually._
import play.api.libs.json.{JsArray, JsObject, JsString}

import scala.concurrent.duration._

/**
  * Verifies that Zeppelin notebooks work in a demo mode
  *
  * @author Oleksiy_Dyagilev
  */
class ZeppelinNotebooksSpec extends FlatSpec with InsightedgeDemoModeDocker {

  val TutorialNotebookId = "INSIGHTEDGE-BASIC"
  val PythonNotebookId = "INSIGHTEDGE-PYTHON"

  "Zeppelin" should "have InsightEdge notebooks" in {
    val resp = wsClient.url(s"$zeppelinUrl/api/notebook").get()
    val notebookIds = jsonBody(resp) \\ "id"

    assert(notebookIds.contains(JsString(TutorialNotebookId)))
    assert(notebookIds.contains(JsString(PythonNotebookId)))
  }

  it should "be possible to run InsightEdge notebooks" in {
    runNotebook(TutorialNotebookId)
    runNotebook(PythonNotebookId)
  }

  def runNotebook(notebookId: String) = {
    println(s"Running notebook $notebookId ...")
    def bindInterpreter() = {
      val bindUrl = s"$zeppelinUrl/api/notebook/interpreter/bind/$notebookId"
      val interpreters = wsClient.url(bindUrl).get()
      val interpreterIds = jsonBody(interpreters) \\ "id"
      val bindResp = wsClient.url(bindUrl).put(JsArray(interpreterIds))
      jsonBody(bindResp)
    }

    bindInterpreter()

    val notebookJobUrl = s"$zeppelinUrl/api/notebook/job/$notebookId"

    val notebookBeforeRun = jsonBody(
      wsClient.url(notebookJobUrl).get()
    )

    val paragraphsCount = (notebookBeforeRun \ "body" \\ "status").size
    println(s"paragraphsCount $paragraphsCount")

    assert((notebookBeforeRun \\ "status") contains JsString("READY"))

    // run notebook
    jsonBody(
      wsClient.url(notebookJobUrl).post(JsObject(Seq()))
    )

    // eventually all paragraphs should be in FINISHED status
    eventually(timeout(3.minutes), interval(5.second))  {
      val jobStatus = jsonBody(
        wsClient.url(notebookJobUrl).get()
      )
      val finished = (jobStatus \ "body" \\ "status").collect { case s@JsString("FINISHED") => s }
      val finishedCount = finished.size
      println(s"finished count $finishedCount")
      assert(finishedCount == paragraphsCount)
    }
  }


}
