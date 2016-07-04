package com.gigaspaces.spark.examples

import com.gigaspaces.spark.fixture.InsightedgeDemoModeDocker
import com.gigaspaces.spark.utils.DockerUtils.dockerExec
import org.scalatest.FlatSpec

/**
  * Verifies that we can submit InsightEdge examples locally (demo mode + submit within docker container)
  *
  * @author Oleksiy_Dyagilev
  */
class ExamplesSubmitSpec extends FlatSpec with InsightedgeDemoModeDocker {

  "insightedge-submit.sh " should "submit examples from insightedge-examples.jar" in {
    val containerId = runningContainerId()
    val exampleClassNames = Seq(
      "basic.SaveRdd",
      "basic.LoadRdd",
      "basic.LoadRddWithSql",
      "basic.PersistDataFrame",
      "basic.PersistDataFrame",
      "mllib.SaveAndLoadMLModel",
      "offheap.OffHeapPersistence"
    )

    exampleClassNames.foreach { className =>
      val fullClassName = s"com.gigaspaces.insightedge.examples.$className"
      val command =
        s"""/opt/gigaspaces-insightedge/bin/insightedge-submit
          |--class $fullClassName
          |--master spark://127.0.0.1:7077
          |/opt/gigaspaces-insightedge/quickstart/scala/insightedge-examples.jar""".stripMargin

      val exitCode = dockerExec(containerId, command)
      assert(exitCode == 0)
    }
  }

  "insightedge-submit.sh " should "fail with wrong space name" in {
    val containerId = runningContainerId()
    val spaceName = "non-existing-space"
    val command =
      s"""/opt/gigaspaces-insightedge/bin/insightedge-submit
          |--class com.gigaspaces.insightedge.examples.basic.SaveRdd
          |--master spark://127.0.0.1:7077
          |/opt/gigaspaces-insightedge/quickstart/scala/insightedge-examples.jar
          |spark://127.0.0.1:7077
          |$spaceName
          |insightedge
          |127.0.0.1:4174""".stripMargin

    val exitCode = dockerExec(containerId, command)
    assert(exitCode != 0)
  }


}
