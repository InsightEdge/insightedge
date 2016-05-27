package com.gigaspaces.spark.fixture

import com.xebialabs.overcast.host.{CloudHost, CloudHostFactory}
import org.scalatest.{BeforeAndAfterAll, Suite}
import play.api.libs.ws.ning.NingWSClient

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.util.Try
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Suite mixin that starts InsightEdge docker image before all tests and stops after
  *
  * @author Oleksiy_Dyagilev
  */
trait InsightEdgeDocker extends BeforeAndAfterAll {
  self: Suite =>

  private val DockerImageStartTimeout = 2.minutes
  private val TargetZeppelinPort = 8090
  private lazy val dockerHost: CloudHost = CloudHostFactory.getCloudHost("insightedge-integration-tests")

  val wsClient = NingWSClient()

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    println("Starting docker container")
    dockerHost.setup()
    if (!awaitImageStarted()) {
      throw new RuntimeException("image start failed with timeout")
    }
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    println("Stopping docker container")
    dockerHost.teardown()
    wsClient.close()
  }

  def zeppelinUrl = {
    val host = dockerHost.getHostName
    val port = dockerHost.getPort(TargetZeppelinPort)
    s"http://$host:$port"
  }

  private def awaitImageStarted(): Boolean = {
    println("Waiting for Zeppelin to be started ...")
    val startTime = System.currentTimeMillis

    val sleepBetweenAttempts = 1.second

    val host = dockerHost.getHostName
    val port = dockerHost.getPort(TargetZeppelinPort)

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
