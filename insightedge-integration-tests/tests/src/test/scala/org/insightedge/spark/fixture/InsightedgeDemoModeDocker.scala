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

package org.insightedge.spark.fixture

import java.util

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.common.collect.ImmutableMap
import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.messages.{ContainerConfig, HostConfig, PortBinding}
import org.insightedge.spark.utils.BuildUtils
import org.insightedge.spark.utils.RestUtils.jsonBody
import org.insightedge.spark.utils.TestUtils.printLnWithTimestamp
import org.scalatest.{BeforeAndAfterAll, Suite}
import play.api.libs.ws.ahc.AhcWSClient

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

/**
  * Suite mixin that starts InsightEdge Demo Mode docker image before all tests and stops after
  *
  * @author Oleksiy_Dyagilev
  */
trait InsightedgeDemoModeDocker extends BeforeAndAfterAll {
  self: Suite =>

  private val DockerImageStartTimeout = 3.minutes
  private val ZeppelinPort = "9090/tcp"
  private val SparkPort = "8080/tcp"
  private val ImageName = s"insightedge-tests-demo-mode"

  protected var containerId: String = _
  private val docker = DefaultDockerClient.fromEnv().build()
  private var zeppelinMappedPort: String = _
  private var sparkMappedPort: String = _

  private val IE_HOME = BuildUtils.IEHome
  private val testFolder = BuildUtils.TestFolder
  private val sharedOutputFolder = testFolder + "/output"

  private val ieLogsPath="/opt/gigaspaces-insightedge/logs"

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  val wsClient              = AhcWSClient()

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    printLnWithTimestamp("Starting docker container")
    printLnWithTimestamp(s"sharedOutputFolder: [$sharedOutputFolder] map to ieLogsPath: [$ieLogsPath] ")
    val portBindings = Map(ZeppelinPort -> randomPort , SparkPort -> randomPort).asJava
    val hostConfig = HostConfig.builder()
      .portBindings(portBindings)
      .appendBinds(IE_HOME + ":/opt/gigaspaces-insightedge")
      .appendBinds(s"$sharedOutputFolder:$ieLogsPath")
      .build()

    val containerConfig = ContainerConfig.builder()
      .hostConfig(hostConfig)
      .image(ImageName)
      .exposedPorts(ZeppelinPort, SparkPort)
      .env("XAP_LICENSE=tryme")
      .cmd("/etc/bootstrap.sh", "-d")
      .build()

    val creation = docker.createContainer(containerConfig)
    containerId = creation.id()

    // Start container
    docker.startContainer(containerId)

    val containerInfo = docker.inspectContainer(containerId)
    val bindings: ImmutableMap[String, util.List[PortBinding]] = containerInfo.networkSettings().ports()
    zeppelinMappedPort = bindings.get(ZeppelinPort).asScala.head.hostPort()
    sparkMappedPort = bindings.get(SparkPort).asScala.head.hostPort()


    if (!awaitImageStarted()) {
      printLnWithTimestamp("image start failed with timeout ... cleaning up")
      stopAll()
      fail("image start failed with timeout")
    }
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    stopAll()
  }

  def stopAll() ={
    printLnWithTimestamp("Stopping docker container")
    docker.killContainer(containerId)
    docker.removeContainer(containerId)
    docker.close()

    wsClient.close()
  }

  def randomPort = {
    Seq(PortBinding.randomPort("0.0.0.0")).asJava
  }

  def zeppelinUrl = {
    s"http://127.0.0.1:$zeppelinMappedPort"
  }

  def sparkUrl = {
    s"http://127.0.0.1:$sparkMappedPort"
  }

  private def awaitImageStarted(): Boolean = {
    printLnWithTimestamp("Waiting for Zeppelin to be started ...")
    val startTime = System.currentTimeMillis

    val sleepBetweenAttempts = 1.second

    printLnWithTimestamp(s"Zeppelin $zeppelinUrl")

    def pingZeppelin() = Try {
      printLnWithTimestamp("ping zeppelin")
      val resp = wsClient.url(zeppelinUrl).get()
      Await.result(resp, 1.second)
    }

    def isSparkAlive() =  {
      printLnWithTimestamp("Check spark master and worker is alive")

      val resp = jsonBody(wsClient.url(s"$sparkUrl/json").get())

      val status = resp \ "status"

      val aliveWorkers = resp \ "aliveworkers"

      "\"ALIVE\"".equals(status.get.toString()) && "1".equals(aliveWorkers.get.toString())
    }

    def timeoutElapsed() = System.currentTimeMillis - startTime > DockerImageStartTimeout.toMillis

    def sleep() = Thread.sleep(sleepBetweenAttempts.toMillis)

    @tailrec
    def retryWhile[T](retry: => T)(cond: T => Boolean): T = {
      val res = retry
      if (cond(res)) {
        res
      } else {
        retryWhile(retry)(cond)
      }
    }

    val zeppelin = retryWhile {
      val r = pingZeppelin()
      sleep()
      r
    }(x => x.isSuccess || timeoutElapsed())

    val sparkMasterAlive = retryWhile {
      val r = isSparkAlive()
      sleep()
      r
    }(x => x || timeoutElapsed())

    zeppelin.isSuccess && sparkMasterAlive
  }


}
