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

package org.insightedge.spark.failover


import org.insightedge.spark.utils.InsightEdgeAdminUtils
import org.json.simple.JSONObject
import org.openspaces.admin.Admin
import org.openspaces.admin.pu.ProcessingUnitInstance
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Suite}
import util.control.Breaks._

/**
  * Verifies that we can submit InsightEdge examples locally (demo mode + submit within docker container)
  *
  * @author Kobi Kisos
  */
class FailOverTest extends FlatSpec with BeforeAndAfterAll {
  self: Suite =>

  protected var admin: Admin = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    println("Starting docker container")
    InsightEdgeAdminUtils
      .numberOfInsightEdgeMasters(1)
      .numberOfInsightEdgeSlaves(3)
      .numberOfDataGridMasters(3)
      .numberOfDataGridSlaves(1)
      .create()

    var step1DataGridMap = inspectDataGridNodes()

  }


  private def inspectDataGridNodes(): Map[ProcessingUnitInstance, List[String]] ={
    var spacesOnMachines: Map[ProcessingUnitInstance, List[String]] = Map[ProcessingUnitInstance, List[String]]()

    InsightEdgeAdminUtils.getDataGridAdmin().getMachines.getMachines.foreach(
      m => m.getProcessingUnitInstances.foreach(
        puInstance => spacesOnMachines += (puInstance -> List(m.getHostAddress, puInstance.getSpaceInstance.getMode.name())))
    )
    println(spacesOnMachines)

    spacesOnMachines
  }

  "insightedge-submit.sh " should "submit examples from insightedge-examples.jar" in {

    val fullClassName = s"org.insightedge.examples.basic.LoadRdd"
    val masterIp = InsightEdgeAdminUtils.getMasterIp()
    val masterContainerId = InsightEdgeAdminUtils.getMasterId()
    val spaceName = "insightedge-space"
    val command = "/opt/insightedge/bin/insightedge-submit  --class " + fullClassName +
      " --master spark://" + masterIp + ":7077 /opt/insightedge/quickstart/scala/insightedge-examples.jar" +
      " spark://" + masterIp + ":7077 " + spaceName +" insightedge " + masterIp + ":4174"

    InsightEdgeAdminUtils.exec(masterContainerId, command)

    var appId = ""
    breakable {
      while (true) {
        if (InsightEdgeAdminUtils.getSparkAppsFromHistoryServer("172.17.0.2").size() > 0) {
          appId = InsightEdgeAdminUtils.getSparkAppsFromHistoryServer("172.17.0.2").get(0).asInstanceOf[JSONObject].get("id").toString
          println("ddddddddddddddddddddddddestry appId " + appId)
          break
        }
      }
    }

    breakable {
      while (true) {
        println(InsightEdgeAdminUtils.isAppCompletedHistoryServer("172.17.0.2", appId).get(0).asInstanceOf[JSONObject].get("status").toString)
        if ("RUNNING".equals(InsightEdgeAdminUtils.isAppCompletedHistoryServer("172.17.0.2", appId).get(0).asInstanceOf[JSONObject].get("status").toString)) {
          println("ddddddddddddddddddddddddestry slave1")
          InsightEdgeAdminUtils.destroyContainerByName("slave1")
          println("after              ddddddddddddddddddddddddestry slave1")
          break
        }
      }
    }


    Thread.sleep(60000)


    var historyServerPid = InsightEdgeAdminUtils.execAndReturnProcessStdout(InsightEdgeAdminUtils.getMasterId(), "pgrep -f HistoryServer").stripLineEnd
    println("history server pid " + historyServerPid)
    InsightEdgeAdminUtils.execAndReturnProcessStdout(InsightEdgeAdminUtils.getMasterId(), "kill -9 " + historyServerPid)
    InsightEdgeAdminUtils.startSparkHistoryServer(InsightEdgeAdminUtils.getMasterId())

    Thread.sleep(30000)

    breakable {
      while (true) {
        println(InsightEdgeAdminUtils.isAppCompletedHistoryServer("172.17.0.2", appId).get(0).asInstanceOf[JSONObject].get("status").toString)
        if ("FAILED".equals(InsightEdgeAdminUtils.isAppCompletedHistoryServer("172.17.0.2", appId).get(0).asInstanceOf[JSONObject].get("status").toString)) {
          println("ddddddddddddddddddddddddestry "+ InsightEdgeAdminUtils.isAppCompletedHistoryServer("172.17.0.2", appId).get(0).asInstanceOf[JSONObject].get("status").toString)
          println("after              ddddddddddddddddddddddddestry faileddddddddddddddddddd")
          break
        }
      }
    }

//    http://172.17.0.2:6066/v1/submissions/status/app-20161116143000-0000/

    Thread.sleep(100000)


  //submit
  // check "completed" : false
  //kill primary of first machine
  //check completed" : true





}



override protected def afterAll(): Unit = {
  super.afterAll()
  InsightEdgeAdminUtils
  .shutdown()
}

  def main(args: Array[String]): Unit = {




}

}
