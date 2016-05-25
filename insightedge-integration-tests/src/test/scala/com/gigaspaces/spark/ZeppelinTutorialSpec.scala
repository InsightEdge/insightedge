package com.gigaspaces.spark

import com.xebialabs.overcast.host.{CloudHost, CloudHostFactory, DockerHost}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec}
import play.api.libs.ws._
import play.api.libs.ws.ning.NingWSClient

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

/**
  * @author Oleksiy_Dyagilev
  */
class ZeppelinTutorialSpec extends FlatSpec with BeforeAndAfterAll {

  val ZeppelinPort = 8090
  val DockerImageStartTimeout = 2.minutes

  var dockerHost: CloudHost = _
  val wsClient = NingWSClient()

  override protected def beforeAll(): Unit = {
    println("Starting docker container")
    dockerHost = CloudHostFactory.getCloudHost("insightedge-integration-tests")
    dockerHost.setup()
    if (!awaitImageStarted()) {

    }
  }


  override protected def afterAll(): Unit = {
    println("Stopping docker container")
    dockerHost.teardown()
  }

  it should "test something" in {
    println("test something")
    assert(1 == 1)
  }

  def awaitImageStarted(): Boolean = {
    println("Waiting for Zeppelin to be started ...")
    val startTime = System.currentTimeMillis

    val sleepBetweenAttempts = 1.second

    val host = dockerHost.getHostName
    val port = dockerHost.getPort(ZeppelinPort)

    def attempt() = Try {
      println("ping zeppelin")
      val resp = wsClient.url(s"http://$host:$port").get()
      Await.result(resp, 1.second)
    }

    def timeoutElapsed() = System.currentTimeMillis - startTime > DockerImageStartTimeout.toMillis

    def sleep() = Thread.sleep(sleepBetweenAttempts.toMillis)

    @tailrec
    def await(): Boolean = attempt() match {
      case fail: Failure[_] =>
        if (timeoutElapsed()) {
          false
        } else {
          sleep()
          await()
        }
      case succ => true
    }

    await()
  }

}
