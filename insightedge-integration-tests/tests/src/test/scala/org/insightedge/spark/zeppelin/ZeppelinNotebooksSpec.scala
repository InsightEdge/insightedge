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

package org.insightedge.spark.zeppelin

import org.insightedge.spark.fixture.InsightedgeDemoModeDocker
import org.insightedge.spark.utils.RestUtils
import org.insightedge.spark.utils.TestUtils.printLnWithTimestamp
import RestUtils._
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

  val ConfigurationNotebookId = "INSIGHTEDGE-CONFIGURATION"
  val TutorialNotebookId = "INSIGHTEDGE-BASIC"
  val PythonNotebookId = "INSIGHTEDGE-PYTHON"
  val GeospatialNotebookId = "INSIGHTEDGE-GEOSPATIAL"

  "Zeppelin" should "have InsightEdge notebooks" in {
    val resp = wsClient.url(s"$zeppelinUrl/api/notebook").get()
    printLnWithTimestamp( "resp:" + resp )
    val notebookIds = jsonBody(resp) \\ "id"

    printLnWithTimestamp( "notebookIds:" + notebookIds )

    assert(notebookIds.contains(JsString(ConfigurationNotebookId)))
    assert(notebookIds.contains(JsString(TutorialNotebookId)))
    assert(notebookIds.contains(JsString(PythonNotebookId)))
    assert(notebookIds.contains(JsString(GeospatialNotebookId)))
  }

  it should "be possible to run InsightEdge notebooks" in {
    runNotebook(TutorialNotebookId)
    runNotebook(PythonNotebookId)
    runNotebook(GeospatialNotebookId)
  }

  def runNotebook(notebookId: String) = {
    printLnWithTimestamp(s"Running notebook $notebookId ...")

    def bindInterpreters() = {
      val bindUrl = s"$zeppelinUrl/api/notebook/interpreter/bind/$notebookId"
      val interpreters = wsClient.url(bindUrl).get()
      val interpreterIds = jsonBody(interpreters) \\ "id"
      val bindResp = wsClient.url(bindUrl).put(JsArray(interpreterIds))
      jsonBody(bindResp)
    }

    def restartInterpreters() = {
      val settingsUrl = s"$zeppelinUrl/api/interpreter/setting"
      val interpreterIds = (jsonBody(wsClient.url(settingsUrl).get()) \\ "id").map(value => value.toString().replace("\"", ""))
      interpreterIds.foreach { interpreterId =>
        val restartUrl = s"$zeppelinUrl/api/interpreter/setting/restart/$interpreterId"
        printLnWithTimestamp("restartUrl:" + restartUrl)
        jsonBody(wsClient.url(restartUrl).withMethod("PUT").execute(), timeout = 1.minute)
      }
    }

    bindInterpreters()
    restartInterpreters()

    val notebookJobUrl = s"$zeppelinUrl/api/notebook/job/$notebookId"

    val notebookBeforeRun = jsonBody(
      wsClient.url(notebookJobUrl).get()
    )

    val paragraphsCount = (notebookBeforeRun \ "body" \\ "status").size
    printLnWithTimestamp(s"paragraphsCount $paragraphsCount")

    assert((notebookBeforeRun \\ "status") contains JsString("READY"))

    // run notebook
    jsonBody(
      wsClient.url(notebookJobUrl).post(JsObject(Seq())), timeout = 5.seconds
    )

    // eventually all paragraphs should be in FINISHED status
    eventually(timeout(3.minutes), interval(5.second)) {
      val jobStatus = jsonBody(
        wsClient.url(notebookJobUrl).get()
      )
      val finished = (jobStatus \ "body" \\ "status").collect { case s@JsString("FINISHED") => s }
      val finishedCount = finished.size
      printLnWithTimestamp(s"finished count $finishedCount/$paragraphsCount")
      assert(finishedCount == paragraphsCount)
    }
  }


}
