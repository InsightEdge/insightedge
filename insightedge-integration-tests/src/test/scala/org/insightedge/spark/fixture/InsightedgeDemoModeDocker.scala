package org.insightedge.spark.fixture

import com.xebialabs.overcast.host.{CloudHost, CloudHostFactory, DockerHost}
import org.scalatest.{BeforeAndAfterAll, Suite}
import play.api.libs.ws.ning.NingWSClient

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.util.Try
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Suite mixin that starts InsightEdge Demo Mode docker image before all tests and stops after
  *
  * @author Oleksiy_Dyagilev
  */
trait InsightedgeDemoModeDocker extends BeforeAndAfterAll {
  self: Suite =>

  private val DockerImageStartTimeout = 3.minutes
  private val TargetZeppelinPort = 8090
  private lazy val dockerHost: DockerHost = CloudHostFactory.getCloudHost("insightedge-tests-demo-mode").asInstanceOf[DockerHost]

  val wsClient = NingWSClient()

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    println("Starting docker container")
    dockerHost.setup()
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
    dockerHost.teardown()
    wsClient.close()
  }

  def zeppelinUrl = {
    val host = dockerHost.getHostName
    val port = dockerHost.getPort(TargetZeppelinPort)
    s"http://$host:$port"
  }

  def runningContainerId() = {
    dockerHost.getDockerDriver.getContainerId
  }

  private def awaitImageStarted(): Boolean = {
    println("Waiting for Zeppelin to be started ...")
    val startTime = System.currentTimeMillis

    val sleepBetweenAttempts = 1.second

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
