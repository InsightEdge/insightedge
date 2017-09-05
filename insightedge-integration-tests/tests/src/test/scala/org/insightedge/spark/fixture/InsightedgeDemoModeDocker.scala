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

import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.messages.HostConfig.Bind
import com.spotify.docker.client.messages.{ContainerConfig, HostConfig, PortBinding}
import org.insightedge.spark.utils.BuildUtils
import org.scalatest.{BeforeAndAfterAll, Suite}
import play.api.libs.ws.ning.NingWSClient

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
  private val ZeppelinPort = "9090"
  private val ImageName = s"insightedge-tests-demo-mode"

  protected var containerId: String = _
  private val docker = DefaultDockerClient.fromEnv().build()
  private var zeppelinMappedPort: String = _

  private val IE_HOME = BuildUtils.IEHome
  private val testFolder = BuildUtils.TestFolder
  private val sharedOutputFolder = testFolder + "/output"

  private val ieLogsPath="/opt/gigaspaces-insightedge/logs"

  val wsClient = NingWSClient()

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    println("Starting docker container")
    println(s"sharedOutputFolder: [$sharedOutputFolder] map to ieLogsPath: [$ieLogsPath] ")
    val randomPort = Seq(PortBinding.randomPort("0.0.0.0")).asJava
    val portBindings = Map(ZeppelinPort -> randomPort).asJava
    val hostConfig = HostConfig.builder()
      .portBindings(portBindings)
      .appendBinds(IE_HOME + ":/opt/gigaspaces-insightedge")
      .appendBinds(s"$sharedOutputFolder:$ieLogsPath")
      .build()

    val containerConfig = ContainerConfig.builder()
      .hostConfig(hostConfig)
      .image(ImageName).exposedPorts(ZeppelinPort)
      .cmd("/etc/bootstrap.sh", "-d")
      .build()

    val creation = docker.createContainer(containerConfig)
    containerId = creation.id()

    // Start container
    docker.startContainer(containerId)

    val containerInfo = docker.inspectContainer(containerId)
    val bindings = containerInfo.networkSettings().ports().get(ZeppelinPort + "/tcp")
    val binding = bindings.asScala.head
    zeppelinMappedPort = binding.hostPort()

    if (!awaitImageStarted()) {
      println("image start failed with timeout ... cleaning up")
      stopAll()
      fail("image start failed with timeout")
    }
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    stopAll()
  }

  def stopAll() ={
    println("Stopping docker container")
    docker.killContainer(containerId)
    docker.removeContainer(containerId)
    docker.close()

    wsClient.close()
  }

  def zeppelinUrl = {
    s"http://127.0.0.1:$zeppelinMappedPort"
  }

  private def awaitImageStarted(): Boolean = {
    println("Waiting for Zeppelin to be started ...")
    val startTime = System.currentTimeMillis

    val sleepBetweenAttempts = 1.second

    println(s"Zeppelin $zeppelinUrl")

    def attempt() = Try {
      println("ping zeppelin")
      val resp = wsClient.url(zeppelinUrl).get()
      Await.result(resp, 1.second)
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

    val res = retryWhile {
      val r = attempt()
      sleep()
      r
    }(x => x.isSuccess || timeoutElapsed())

    res.isSuccess
  }


}
