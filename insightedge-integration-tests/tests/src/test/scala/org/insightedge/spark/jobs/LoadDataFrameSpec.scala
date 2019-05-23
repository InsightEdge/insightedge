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

package org.insightedge.spark.jobs

import org.insightedge.spark.fixture.InsightedgeDemoModeDocker
import org.insightedge.spark.utils.InsightEdgeAdminUtils
import org.insightedge.spark.utils.TestUtils.printLnWithTimestamp
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Suite}


/**
  * Test load DataFrame of Pojo, which also contains enum
  *
  * @author Moran
  */
class LoadDataFrameSpec extends FlatSpec with InsightedgeDemoModeDocker {
  self: Suite =>

  private val JOBS = s"/opt/insightedge/insightedge/examples/jars/jobs.jar"

  override protected def beforeAll(): Unit = {
    printLnWithTimestamp("beforeAll - LoadDataFrameSpec")
    super.beforeAll()
  }

  "insightedge-submit " should "submit LoadDataFrame job"  in {
    val loadDfFullClassName = s"org.insightedge.spark.jobs.LoadDataFrame"
    val masterIp = InsightEdgeAdminUtils.getMasterIp()
    val masterContainerId = InsightEdgeAdminUtils.getMasterId()

    printLnWithTimestamp( "masterContainerId:" + masterContainerId )

    val spaceName = "demo"

    val loadDfCommand = s"/opt/insightedge/insightedge/bin/insightedge-submit --class " + loadDfFullClassName +
      " --master spark://" + masterIp + ":7077 " + JOBS +
      " spark://" + masterIp + ":7077 " + spaceName

    printLnWithTimestamp( "loadDfCommand Command:" + loadDfCommand )

    InsightEdgeAdminUtils.exec(masterContainerId, loadDfCommand)

    val appId: String = InsightEdgeAdminUtils.getAppId(0)
    printLnWithTimestamp(s"Application Id = $appId")

    InsightEdgeAdminUtils.waitForAppSuccess(appId, 60)

    InsightEdgeAdminUtils.exec(masterContainerId, loadDfCommand)
  }


  override protected def afterAll(): Unit = {
    super.afterAll()
    InsightEdgeAdminUtils
      .shutdown()
  }

}
