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
import org.insightedge.spark.utils.DockerUtils.dockerExec
import org.insightedge.spark.utils.TestUtils.printLnWithTimestamp
import org.scalatest.{FlatSpec, Suite}


/**
  * Test load DataFrame of Pojo, which also contains enum
  *
  * @Since 14.5 Support for Enum
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
    val fullClassName = s"org.insightedge.spark.jobs.LoadDataFrame"

    val command =
      s"""/opt/gigaspaces-insightedge/insightedge/bin/insightedge-submit
         |--class $fullClassName
         |--master spark://127.0.0.1:7077
         |/opt/gigaspaces-insightedge/insightedge/examples/jars/insightedge-examples.jar""".stripMargin

    printLnWithTimestamp( "command:" + command )
    val exitCode = dockerExec(containerId, command)
    printLnWithTimestamp( "exitCode:" + exitCode )
    assert(exitCode == 0)
  }
}
