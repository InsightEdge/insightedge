package org.insightedge.spark.fixture

import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.messages.{ContainerConfig, ContainerInfo, HostConfig, PortBinding}
import org.insightedge.spark.utils.BuildUtils
import org.insightedge.spark.utils.BuildUtils.BuildVersion
import org.scalatest.{BeforeAndAfterAll, Suite}
import play.api.libs.ws.ning.NingWSClient

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.util.Try
import scala.concurrent.duration._
import collection.JavaConverters._

/**
  * Suite mixin that starts InsightEdge Demo Mode docker image before all tests and stops after
  *
  * @author Oleksiy_Dyagilev
  */
trait InsightedgeDemoModeDocker extends BeforeAndAfterAll {
  self: Suite =>

  private val DockerImageStartTimeout = 3.minutes
  private val ZeppelinPort = "8090"
  private val ImageName = s"insightedge-tests-demo-mode:${BuildUtils.BuildVersion}"

  protected var containerId: String = _
  private val docker = DefaultDockerClient.fromEnv().build()
  private var containerInfo: ContainerInfo = _

  val wsClient = NingWSClient()

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    println("Starting docker container")

    val randomPort = Seq(PortBinding.randomPort("0.0.0.0")).asJava
    val portBindings = Map(ZeppelinPort -> randomPort).asJava
    val hostConfig = HostConfig.builder().portBindings(portBindings).build()

    val containerConfig = ContainerConfig.builder()
      .hostConfig(hostConfig)
      .image(ImageName).exposedPorts(ZeppelinPort)
      .cmd("/etc/bootstrap.sh", "-d")
      .build()

    val creation = docker.createContainer(containerConfig)
    containerId = creation.id()

    // Start container
    docker.startContainer(containerId)

    containerInfo = docker.inspectContainer(containerId)

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
    val bindings = containerInfo.networkSettings().ports().get(ZeppelinPort + "/tcp")
    val binding = bindings.asScala.head
    val port = binding.hostPort()
    s"http://127.0.0.1:$port"
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
