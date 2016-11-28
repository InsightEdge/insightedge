package org.insightedge.spark.utils

import java.lang.Exception
import java.util.concurrent.TimeUnit

import com.gigaspaces.cluster.activeelection.SpaceMode
import com.spotify.docker.client.DockerClient.RemoveContainerParam
import com.spotify.docker.client.messages.{ContainerConfig, HostConfig, PortBinding}
import com.spotify.docker.client.{DefaultDockerClient, DockerClient}
import org.json.simple.parser.JSONParser
import org.json.simple.{JSONArray, JSONObject}
import org.openspaces.admin.pu.ProcessingUnitInstance
import org.openspaces.admin.{Admin, AdminFactory}
import org.scalatest.Assertions
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ning.NingWSClient

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, TimeoutException}
import scala.util.{Failure, Success, Try}
import scala.util.control.Breaks._

/**
  * Created by kobikis on 13/11/16.
  *
  * @since 1.1.0
  */

object InsightEdgeAdminUtils extends Assertions{

  private val DockerImageStartTimeout = 3.minutes
  private val ZeppelinPort = "8090"
  private val ImageName = s"insightedge-test:${BuildUtils.BuildVersion}"

  private val docker = DefaultDockerClient.fromEnv().build()
  private var zeppelinMappedPort: String = _

  private var IE_HOME = BuildUtils.IEHome

  private val wsClient = NingWSClient()
  var containersId: Map[String, String] = Map[String, String]()
  private var ieSlaveCounter = 0
  private var ieMasterCounter = 0

  private var numOfIEMasters: Int = _
  private var numOfIESlaves: Int = _

  private var numOfDataGridMasters: Int = _
  private var numOfDataGridSlaves: Int = _

  protected var admin: Admin = _

  private val parser = new JSONParser()

  def loadInsightEdgeSlaveContainer(masterIp: String): String = {
    val hostConfig = HostConfig
      .builder()
      .appendBinds(IE_HOME + ":/opt/insightedge")
      .build()

    val containerConfig = ContainerConfig.builder()
      .hostConfig(hostConfig)
      .image(ImageName)
      .cmd("bash", "-c", "/opt/insightedge/sbin/insightedge.sh --mode slave --master " + masterIp + " && sleep 2h")
      .env("XAP_LOOKUP_LOCATORS=" + masterIp)
      .env("XAP_NIC_ADDRESS=#local:ip#")
      .build()

    ieSlaveCounter += 1

    val creation = docker.createContainer(containerConfig, "slave" + ieSlaveCounter)
    val containerId = creation.id()

    containersId += ("slave" + ieSlaveCounter -> containerId)

    // Start container
    docker.startContainer(containerId)

    containerId
  }

  def loadInsightEdgeMasterContainer(): String = {
    val randomPort = Seq(PortBinding.randomPort("0.0.0.0")).asJava
    val portBindings = Map(ZeppelinPort -> randomPort).asJava
    val hostConfig = HostConfig
      .builder()
      .portBindings(portBindings)

      .appendBinds(IE_HOME + ":/opt/insightedge")
      .build()

    val containerConfig = ContainerConfig.builder()
      .hostConfig(hostConfig)
      .image(ImageName)
      .exposedPorts(ZeppelinPort, "4174")
      .env("XAP_NIC_ADDRESS=#local:ip#")
      .cmd("bash", "-c", "export MY_IP=`hostname -I | cut -d\" \" -f 1` && /opt/insightedge/sbin/insightedge.sh --mode master --master $MY_IP && sleep 2h")
      .build()

    ieMasterCounter += 1

    val creation = docker.createContainer(containerConfig, "master" + ieMasterCounter)
    val containerId = creation.id()
    containersId += ("master" + ieMasterCounter -> containerId)

    // Start container
    docker.startContainer(containerId)

    val execCreation = docker.execCreate(containerId, Array("bash", "-c", "export MY_IP=`hostname -I | cut -d\" \" -f 1` && /opt/insightedge/sbin/insightedge.sh --mode zeppelin --master $MY_IP"))
    val execId = execCreation.id()
    val stream = docker.execStart(execId)
    stream.readFully()
    stream.close()

    startSparkHistoryServer(containerId)

    val containerInfo = docker.inspectContainer(containerId)
    val bindings = containerInfo.networkSettings().ports().get(ZeppelinPort + "/tcp")
    val binding = bindings.asScala.head
    zeppelinMappedPort = binding.hostPort()


    Try {
      retry(30000 millis, 1000 millis) {
        println("ping zeppelin")
        val resp = wsClient.url(zeppelinUrl).get()
        Await.result(resp, 1.second)
      }
    }match {
      case Success(_)  => println("Zeppling started")
      case Failure(_) => fail("image [ " + ImageName + " ] start failed with timeout")
    }

    containerId
  }

  def zeppelinUrl = {
    s"http://127.0.0.1:$zeppelinMappedPort"
  }

  def startSparkHistoryServer(masterContainerId: String): Unit = {
    val execCreationHistoryServer = docker.execCreate(masterContainerId, Array("bash", "-c", "/opt/insightedge/sbin/start-history-server.sh"))
    val execIdHistoryServer = execCreationHistoryServer.id()
    val streamHistoryServer = docker.execStart(execIdHistoryServer)
    streamHistoryServer.readFully()
    streamHistoryServer.close()
  }

  def restartSparkHistoryServer(): Unit = {
    var historyServerPid = InsightEdgeAdminUtils.execAndReturnProcessStdout(InsightEdgeAdminUtils.getMasterId(), "pgrep -f HistoryServer").stripLineEnd
    println("history server pid " + historyServerPid)
    InsightEdgeAdminUtils.execAndReturnProcessStdout(InsightEdgeAdminUtils.getMasterId(), "kill -9 " + historyServerPid)
    InsightEdgeAdminUtils.startSparkHistoryServer(InsightEdgeAdminUtils.getMasterId())
  }

  def retry[T](timeout: FiniteDuration, sleepBetweenAttempts: FiniteDuration)(fn: => T): T = {
    def startTime = System.currentTimeMillis()
    retryRec(timeout, sleepBetweenAttempts, startTime){
      fn
    }
  }

  @annotation.tailrec
  private def retryRec[T](timeout: FiniteDuration, sleepBetweenAttempts: FiniteDuration, start: Long)(fn: => T): T = {
    def startTime = start

    def timeoutElapsed() = {
      val running = System.currentTimeMillis - startTime
      running < timeout.toMillis
    }

    def sleep() = Thread.sleep(sleepBetweenAttempts.toMillis)

    sleep()

    util.Try { fn } match {
      case util.Success(x) => x
      case _  if timeoutElapsed() =>
        retryRec(timeout, sleepBetweenAttempts, startTime)(fn)
      case Failure(e) =>
        if(timeoutElapsed()) {
          println(e)
          retryRec(timeout, sleepBetweenAttempts, startTime)(fn)
        }
        else
          throw e
    }
  }

  def execAndWaitFor(containerId: String, command: String): Int = {
    val execCreation = docker.execCreate(containerId, Array("bash", "-c", command), DockerClient.ExecCreateParam.attachStdout(), DockerClient.ExecCreateParam.attachStderr())
    val execId = execCreation.id()
    val output = docker.execStart(execId)
    val execOutput = output.readFully()

    val state = docker.execInspect(execId)
    output.close()
    state.exitCode()
  }

  def execAndReturnProcessStdout(containerId: String, command: String): String = {
    val execCreation = docker.execCreate(containerId, Array("bash", "-c", command), DockerClient.ExecCreateParam.attachStdout(), DockerClient.ExecCreateParam.attachStderr())
    val execId = execCreation.id()
    val output = docker.execStart(execId)
    output.readFully()
  }

  def exec(containerId: String, command: String): Unit = {
    val execCreation = docker.execCreate(containerId, Array("bash", "-c", command))
    val execId = execCreation.id()
    val output = docker.execStart(execId)
    output.close()
  }

  protected def getContainerIp(containerId: String): String = {
    var info = docker.inspectContainer(containerId)
    info.networkSettings().ipAddress()
  }

  def deployDataGrid(containerId: String, masterIp: String, topology: String): Unit = {
    val execCreation = docker.execCreate(containerId, Array("bash", "-c", "/opt/insightedge/sbin/insightedge.sh --mode deploy --topology " + topology + " --master " + masterIp))
    val execId = execCreation.id()
    var stream = docker.execStart(execId)
    stream.close()
  }


  def unDeployDataGrid(containerId: String, masterIp: String): Unit = {
    val execCreation = docker.execCreate(containerId, Array("bash", "-c", "/opt/insightedge/sbin/insightedge.sh --mode undeploy --master " + masterIp))
    val execId = execCreation.id()
    var stream = docker.execStart(execId)
    stream.close()
  }

  def getMasterIp(): String = {
    getContainerIp(getMasterId())
  }

  def getSlaveIp(name: String): String = {
    getContainerIp(getSlaveId(name))
  }

  def getMasterId(): String = {
    containersId.get("master1").get
  }

  def getSlaveId(name: String): String = {
    containersId.get("name").get
  }

  def destroyContainerByName(name: String): Unit = {
    val id = containersId.get(name).get
    docker.killContainer(id)
    docker.removeContainer(id)
    containersId -= name
  }

  def numberOfInsightEdgeMasters(numberOfIeMasters: Int): this.type = {
    numOfIEMasters = numberOfIeMasters
    this
  }

  def numberOfInsightEdgeSlaves(numberOfIeSlaves: Int): this.type = {
    numOfIESlaves = numberOfIeSlaves
    this
  }

  def numberOfDataGridMasters(numberOfDataGridMasters: Int): this.type = {
    numOfDataGridMasters = numberOfDataGridMasters
    this
  }

  def numberOfDataGridSlaves(numberOfDataGridSlaves: Int): this.type = {
    numOfDataGridSlaves = numberOfDataGridSlaves
    this
  }

  private def createDataGridAdmin(locator: String): Admin = {
    new AdminFactory().addLocator(locator).create()
  }

  def getDataGridAdmin: Admin = {
    admin
  }

  def assertAllJobsSucceeded(masterIp: String, appId: String): Unit = {
    val jobs: JSONArray = getBody(wsClient.url(s"http://$masterIp:18080/api/v1/applications/$appId/jobs").get())
    val jobsArr = jobs.toArray(new Array[JSONObject](0))
    jobsArr.map((o: JSONObject) => assert(!o.get("status").equals("FAILED")))
  }

  def isAppCompletedHistoryServer(masterIp: String, appId: String): JSONArray = {
    getBody(wsClient.url(s"http://$masterIp:18080/api/v1/applications/$appId/jobs").get())
  }

  def getSparkAppsFromHistoryServer(masterIp: String): JSONArray = {
    getBody(wsClient.url(s"http://$masterIp:18080/api/v1/applications/").get())
  }

  private def getBody(future: Future[WSResponse]): JSONArray = {
    val response = Await.result(future, Duration.Inf)
    if (response.status != 200)
      throw new Exception(response.statusText)
    jsonStrToMap(response.body)
  }

  private def jsonStrToMap(jsonStr: String): JSONArray = {
    parser.parse(jsonStr).asInstanceOf[JSONArray]
  }


  def create(): this.type = {
    for (i <- 0 until numOfIEMasters) {
      loadInsightEdgeMasterContainer()
    }
    for (i <- 0 until numOfIESlaves) {
      loadInsightEdgeSlaveContainer(getContainerIp(containersId.get("master1").get))
    }
    deployDataGrid(containersId.get("master1").get, getContainerIp(containersId.get("master1").get), numOfDataGridMasters.toString + "," + numOfDataGridSlaves)

    admin = createDataGridAdmin(getMasterIp())

    admin.getProcessingUnits.waitFor("insightedge-space", 60, TimeUnit.SECONDS).waitForSpace(60, TimeUnit.SECONDS)

    this
  }

  def getAppId: String = {
    var appId = ""
    retry(30000 millis, 100 millis) {
      val f = Future {
        if (getSparkAppsFromHistoryServer(getMasterIp()).size() > 0)
          appId = getSparkAppsFromHistoryServer(getMasterIp()).get(0).asInstanceOf[JSONObject].get("id").toString
        if(appId == null ||  appId.equals(""))
          fail("Failed to get app id from Spark History Server")
        else {
          println(s"App Id [ $appId ]")
          appId
        }
      }
      Await.result(f, 1.seconds)
      f.value.get.get
    }
  }

  def destroyMachineWhenAppIsRunning(appId: String, containerName: String): Unit = {
    retry(30000 millis, 100 millis) {
      val f = Future {
        val status = InsightEdgeAdminUtils.isAppCompletedHistoryServer(getMasterIp(), appId).get(0).asInstanceOf[JSONObject].get("status").toString
        if ("RUNNING".equals(status)) {
          destroyContainerByName(containerName)
          containersId -= containerName
          println(s"Container $containerName destroyed")
        } else {
          fail(s"job of app [$appId] is not on status RUNNING")
        }
      }
      Await.result(f, 5 seconds)
    }
  }

  def waitForAppSuccess(appId: String, sec: Int): Unit = {
    retry(30000 millis, 100 millis) {
      val f = Future {
        val status = InsightEdgeAdminUtils.isAppCompletedHistoryServer(getMasterIp(), appId).get(0).asInstanceOf[JSONObject].get("status").toString
        if (!"SUCCEEDED".equals(status)) {
          fail(s"job of app [$appId] is not on status SUCCEEDED")
        }
      }
      Await.result(f, 5 seconds)
    }
  }

  def datagridNodes(): Map[ProcessingUnitInstance, List[String]] ={
    var spacesOnMachines: Map[ProcessingUnitInstance, List[String]] = Map[ProcessingUnitInstance, List[String]]()

    admin
      .getMachines.waitFor(4, 30, TimeUnit.SECONDS)

    admin
      .getSpaces
      .waitFor("insightedge-space", 30, TimeUnit.SECONDS)
      .waitFor(6, 30, TimeUnit.SECONDS)

    admin
      .getSpaces
      .waitFor("insightedge-space", 30, TimeUnit.SECONDS)
      .waitFor(3, SpaceMode.PRIMARY, 30, TimeUnit.SECONDS)

    admin
      .getSpaces
      .waitFor("insightedge-space", 30, TimeUnit.SECONDS)
      .waitFor(3, SpaceMode.BACKUP, 30, TimeUnit.SECONDS)

    admin.getMachines.getMachines.foreach(
      m => m.getProcessingUnitInstances.foreach(
        puInstance => spacesOnMachines += (puInstance -> List(m.getHostAddress, puInstance.getSpaceInstance.getMode.name())))
    )
    spacesOnMachines
  }

  def shutdown(): Unit = {
    println("Stopping docker container")
    containersId foreach ((t2) => docker.removeContainer(t2._1, RemoveContainerParam.forceKill()))
    docker.close()
    wsClient.close()
  }
}


