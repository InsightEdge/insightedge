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

import org.insightedge.spark.utils.{BuildUtils, InsightEdgeAdminUtils, PremiumOnlyTest}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Suite}


/**
  * Verifies that InsightEdge can recover from machine failover
  *
  * Toplogy:
  *  master1                    slave1                  slave2                 slave3
  *  Spark Master               Spark Worker Node       Spark Worker Node      Spark Worker Node
  *  XAP Master (GSA,GSM,LUS)   XAP Slave (GSA, 2*GSC)  XAP Slave (GSA, 2*GSC) XAP Slave (GSA, 2*GSC)
  *  Zeppelin
  *  Spark History Server
  *
  * Scenario:
  * 1. submit job
  * 2. destroy slave1
  *
  * Expected result:
  * Job should and with status SUCCEEDED
  *
  * @author Kobi Kisos
  */
class MachineFailOverSaveRddSpec extends FlatSpec with BeforeAndAfterAll {
  self: Suite =>

  private val JOBS = s"/opt/insightedge/quickstart/scala/jobs.jar"

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    println("Starting docker container")
    InsightEdgeAdminUtils
      .numberOfInsightEdgeMasters(1)
      .numberOfInsightEdgeSlaves(3)
      .numberOfDataGridMasters(3)
      .numberOfDataGridSlaves(1)
      .create()
  }

  "insightedge-submit.sh " should "submit SaveRdd example while destroying slave machine" taggedAs PremiumOnlyTest in {

    val fullClassName = s"org.insightedge.spark.jobs.SaveRdd"
    val masterIp = InsightEdgeAdminUtils.getMasterIp()
    val masterContainerId = InsightEdgeAdminUtils.getMasterId()
    val spaceName = "insightedge-space"
    val command = "/opt/insightedge/bin/insightedge-submit  --class " + fullClassName +
      " --master spark://" + masterIp + ":7077 " + JOBS +
      " spark://" + masterIp + ":7077 " + spaceName + " insightedge " + masterIp + ":4174"


    println("---BEFORE COMMAND - all apps info")
    val appsBeforeCommand =  InsightEdgeAdminUtils.getSparkAppsFromHistoryServer(masterIp)
    val str = appsBeforeCommand.toJSONString
    println(str)
    println("END APPS INFO")

    println(command)

    InsightEdgeAdminUtils.exec(masterContainerId, command)


    var appId: String = InsightEdgeAdminUtils.getAppId

    InsightEdgeAdminUtils.destroyMachineWhenAppIsRunning(appId, "slave1")

    //wait for job to finish
    Thread.sleep(60000)

    InsightEdgeAdminUtils.restartSparkHistoryServer()

    //wait for history server to be available
    Thread.sleep(30000)

    InsightEdgeAdminUtils.waitForAppSuccess(appId, 30)
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    InsightEdgeAdminUtils
      .shutdown()
  }

}
