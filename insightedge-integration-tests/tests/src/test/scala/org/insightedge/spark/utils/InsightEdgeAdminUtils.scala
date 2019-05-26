package org.insightedge.spark.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

import com.gigaspaces.cluster.activeelection.SpaceMode
import com.spotify.docker.client.DockerClient.RemoveContainerParam
import com.spotify.docker.client.messages.{Container, ContainerConfig, HostConfig, PortBinding}
import com.spotify.docker.client.{DefaultDockerClient, DockerClient, LogStream}
import org.json.simple.parser.JSONParser
import org.json.simple.{JSONArray, JSONObject}
import org.openspaces.admin.pu.ProcessingUnitInstance
import org.openspaces.admin.{Admin, AdminFactory}
import org.scalatest.Assertions
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ning.NingWSClient

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import org.insightedge.spark.utils.TestUtils.printLnWithTimestamp

/**
  * Created by kobikis on 13/11/16.
  *
  * @since 1.1.0
  */

object InsightEdgeAdminUtils extends Assertions{

  def main(args: Array[String]): Unit = {
    //    val cs = docker.listContainers().asScala.filter(c => c.names().contains("/yohanac"))
    //    if (cs.size == 1)
    //      docker.removeContainer(cs.head.id())

    val hostConfig = HostConfig
      .builder()
      .build()

    val containerConfig = ContainerConfig.builder()
      .hostConfig(hostConfig)
      .env("XAP_LICENSE=tryme")
      .image(ImageName)
      .cmd("bash", "-c", "sleep 1h")
      .build()

    val creation = docker.createContainer(containerConfig, "yohanac")
    val containerId = creation.id()

    docker.startContainer(containerId)

    val execCreation = docker.execCreate(containerId, Array("bash", "-c", "sleep 10s ; echo hello there"), DockerClient.ExecCreateParam.attachStdout(), DockerClient.ExecCreateParam.attachStderr())
    val execId = execCreation.id()
    val output = docker.execStart(execId)
    val outputString = output.readFully()
    printLnWithTimestamp(s"output of command on container $containerId is: $outputString")


  }

  private val DockerImageStartTimeout = 3.minutes
  private val ZeppelinPort = "9090"
  private val ImageName = s"insightedge-test"

  private val docker = DefaultDockerClient.fromEnv().build()
  private var zeppelinMappedPort: String = _

  private val IE_HOME = BuildUtils.IEHome

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

  private val testFolder: String = BuildUtils.TestFolder
  private val sharedOutputFolder = s"$testFolder/output"
  private val ieLogsPath = "/opt/insightedge/logs"

  private var managerServers = ""

  def loadInsightEdgeSlaveContainer(id: Int, managerServers : String): String = {
    printLnWithTimestamp(s"slave - sharedOutputFolder: [$sharedOutputFolder] map to ieLogsPath: [$ieLogsPath] ")
    val hostConfig = HostConfig
      .builder()
      .appendBinds(IE_HOME + ":/opt/insightedge")
      .appendBinds(s"$sharedOutputFolder:$ieLogsPath")
      .build()

    val containerConfig = ContainerConfig.builder()
      .hostConfig(hostConfig)
      .image(ImageName)
      .env("XAP_LICENSE=tryme", s"XAP_MANAGER_SERVERS=$managerServers")
      .cmd("bash", "-c", s"cp -rf /opt/insightedge/deploy /")
      .cmd("bash", "-c", s"/opt/insightedge/bin/insightedge host run-agent --spark-worker --containers=2 > $ieLogsPath/worker-$id.log")
      .build()

    ieSlaveCounter += 1

    val creation = docker.createContainer(containerConfig, "slave" + ieSlaveCounter)
    val containerId = creation.id()

    containersId += ("slave" + ieSlaveCounter -> containerId)

    // Start container
    docker.startContainer(containerId)

    containerId
  }

  def startContainers(n: Int): String = {
    for (_ <- 1 to n) {
      printLnWithTimestamp(s"master - sharedOutputFolder: [$sharedOutputFolder] map to ieLogsPath: [$ieLogsPath] ")

      val randomPort = Seq(PortBinding.randomPort("0.0.0.0")).asJava
      val portBindings = Map(ZeppelinPort -> randomPort).asJava
      val hostConfig = HostConfig
        .builder()
        .portBindings(portBindings)
        .appendBinds(IE_HOME + ":/opt/insightedge")
        .appendBinds(s"$sharedOutputFolder:$ieLogsPath")
        .build()

      val containerConfig = ContainerConfig.builder()
        .hostConfig(hostConfig)
        .image(ImageName)
        .exposedPorts(ZeppelinPort, "4174")
        .env("XAP_LICENSE=tryme")
        //      .env("XAP_NIC_ADDRESS=#local:ip#")
        .cmd("bash", "-c", "sleep 1d")
        //      .cmd("bash", "-c", "export MY_IP=`hostname -I | cut -d\" \" -f 1` && /opt/insightedge/insightedge/bin/insightedge --mode master --master $MY_IP && sleep 2h")
        .build()

      ieMasterCounter += 1

      val creation = docker.createContainer(containerConfig, "master" + ieMasterCounter)
      val containerId = creation.id()
      containersId += ("master" + ieMasterCounter -> containerId)

      // Start container
      docker.startContainer(containerId)
    }

    containersId.filterKeys( _.startsWith("master") ).map( entry => getContainerIp(entry._2) ).mkString(",")
  }

  def loadInsightEdgeMasterContainer(id:Int, managerServers:String): String = {
    val containerId = containersId(s"master$id")
    val masterExecCreation = docker.execCreate(containerId, Array("bash", "-c", s"cp -rf /opt/insightedge/deploy / && /opt/insightedge/bin/insightedge host run-agent --manager --spark-master > $ieLogsPath/master-$id.log 2>&1"))
    val masterExecId = masterExecCreation.id()
    docker.execStart(masterExecId)

    val execCreation = docker.execCreate(containerId, Array("bash", "-c", s"/opt/insightedge/bin/insightedge  host run-agent --zeppelin > $ieLogsPath/zeppelin-$id.log 2>&1"))
    val execId = execCreation.id()
    docker.execStart(execId)

    startSparkHistoryServer(containerId)

    val containerInfo = docker.inspectContainer(containerId)
    val bindings = containerInfo.networkSettings().ports().get(ZeppelinPort + "/tcp")
    val binding = bindings.asScala.head
    zeppelinMappedPort = binding.hostPort()


    Try {
      retry(60000 millis, 2000 millis) {
        printLnWithTimestamp("ping zeppelin")
        val resp = wsClient.url(zeppelinUrl).get()
        Await.result(resp, 1.second)
      }
    }match {
      case Success(_)  => printLnWithTimestamp("Zeppling started")
      case Failure(_) => fail("image [ " + ImageName + " ] start failed with timeout")
    }

    containerId
  }

  def zeppelinUrl = {
    s"http://127.0.0.1:$zeppelinMappedPort"
  }

  def startSparkHistoryServer(masterContainerId: String): Unit = {
    printLnWithTimestamp("called startSparkHistoryServer")
    val execCreationHistoryServer = docker.execCreate(masterContainerId, Array("bash", "-c", s"/opt/insightedge/insightedge/spark/sbin/start-history-server.sh >> $ieLogsPath/history-server-$masterContainerId.log 2>&1"))
    val execIdHistoryServer = execCreationHistoryServer.id()
    val streamHistoryServer = docker.execStart(execIdHistoryServer)
  }

  def restartSparkHistoryServer(): Unit = {
    printLnWithTimestamp("called restartSparkHistoryServer")
    InsightEdgeAdminUtils.execAndReturnProcessStdout(InsightEdgeAdminUtils.getMasterId(), s"/opt/insightedge/insightedge/spark/sbin/stop-history-server.sh >> $ieLogsPath/stop-history-server.log 2>&1")
    //    var historyServerPid = InsightEdgeAdminUtils.execAndReturnProcessStdout(InsightEdgeAdminUtils.getMasterId(), "pgrep -f HistoryServer").stripLineEnd
    //    printLnWithTimestamp("killing history server with pid " + historyServerPid)
    //    InsightEdgeAdminUtils.execAndReturnProcessStdout(InsightEdgeAdminUtils.getMasterId(), "kill -9 " + historyServerPid)
    printLnWithTimestamp("starting spark history server")
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
          printLnWithTimestamp(e)
          retryRec(timeout, sleepBetweenAttempts, startTime)(fn)
        }
        else
          throw e
    }
  }

  def execAndWaitFor(containerId: String, command: String): Long = {
    Thread.sleep(5000)

    val execCreation = docker.execCreate(containerId, Array("bash", "-c", command), DockerClient.ExecCreateParam.attachStdout(), DockerClient.ExecCreateParam.attachStderr())
    val execId = execCreation.id()
    val output = docker.execStart(execId)
    val execOutput = output.readFully()

    val state = docker.execInspect(execId)
    output.close()
    state.exitCode()
  }

  def execAndReturnProcessStdout(containerId: String, command: String): String = {
    printLnWithTimestamp(s"executing [$command] on container $containerId, will print its stdout")
    val execCreation = docker.execCreate(containerId, Array("bash", "-c", command), DockerClient.ExecCreateParam.attachStdout(), DockerClient.ExecCreateParam.attachStderr())
    val execId = execCreation.id()
    val output = docker.execStart(execId)
    val outputString = output.readFully()
    printLnWithTimestamp(s"output of command [$command] on container $containerId is: $outputString")
    outputString
  }

  def exec(containerId: String, command: String): Unit = {
    printLnWithTimestamp(s"executing [$command] on container $containerId")
    val execCreation = docker.execCreate(containerId, Array("bash", "-c", command))
    val execId = execCreation.id()
    val output: LogStream = docker.execStart(execId)
  }

  protected def getContainerIp(containerId: String): String = {
    var info = docker.inspectContainer(containerId)
    info.networkSettings().ipAddress()
  }

  def deployDataGrid(containerId: String, partitions: Int, backups: Boolean): Unit = {
    var deployOptions = ""
    if (partitions > 0) {
      deployOptions = s"--partitions=$partitions"
      if (backups) {
        deployOptions += " --ha"
      }
    }
    val execCreation = docker.execCreate(containerId, Array("bash", "-c", s"/opt/insightedge/bin/insightedge space deploy $deployOptions demo >> $ieLogsPath/deploy-space.log 2>&1"))
    val execId = execCreation.id()
    var stream = docker.execStart(execId)
  }


  def unDeployDataGrid(containerId: String, masterIp: String): Unit = {
    val execCreation = docker.execCreate(containerId, Array("bash", "-c", s"/opt/insightedge/bin/insightedge pu undeploy demo >> $ieLogsPath/undeploy-space.log 2>&1"))
    val execId = execCreation.id()
    var stream = docker.execStart(execId)
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
    new AdminFactory().addLocators(locator).create()
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
    managerServers = startContainers(numOfIEMasters)

    printLnWithTimestamp("Backing-up setenv-overrides.sh file")
    execAndWaitFor(containersId("master1"), s"cp -p /opt/insightedge/bin/setenv-overrides.sh /opt/insightedge/bin/setenv-overrides.sh.bck")
    execAndWaitFor(containersId("master1"), s"""echo "" >> /opt/insightedge/bin/setenv-overrides.sh""")
    execAndWaitFor(containersId("master1"), s"""echo "export XAP_MANAGER_SERVERS=$managerServers" >> /opt/insightedge/bin/setenv-overrides.sh""")
    execAndWaitFor(containersId("master1"), """echo "export XAP_NIC_ADDRESS=\$(hostname -I | cut -d \" \" -f 1)" >> /opt/insightedge/bin/setenv-overrides.sh""")

    for (i <- 1 to numOfIEMasters) {
      loadInsightEdgeMasterContainer(i, managerServers)
    }
    for (i <- 1 to numOfIESlaves) {
      loadInsightEdgeSlaveContainer(i, managerServers)
    }
    deployDataGrid(containersId("master1"), numOfDataGridMasters, numOfDataGridSlaves > 0)

    admin = createDataGridAdmin(managerServers)

    admin.getProcessingUnits.waitFor("demo", 60, TimeUnit.SECONDS).waitForSpace(60, TimeUnit.SECONDS)

    this
  }

  def getAppId(index: Int): String = {
    var appId = ""
    retry(30000 millis, 1000 millis) {
      val sparkApps: JSONArray = getSparkAppsFromHistoryServer(getMasterIp())
      if (sparkApps.size() > index)
        appId = sparkApps.get(index).asInstanceOf[JSONObject].get("id").toString
      if(appId == null ||  appId.equals("")) {
        fail("Failed to get app id from Spark History Server")
      }
      else {
        printLnWithTimestamp(s"App Id [ $appId ]")
        appId
      }
    }
  }

  def destroyMachineWhenAppIsRunning(appId: String, containerName: String): Unit = {
    retry(30000 millis, 100 millis) {
      val arr: JSONArray = isAppCompletedHistoryServer(getMasterIp(), appId)
      val status = arr.get(0).asInstanceOf[JSONObject].get("status").toString
      if ("RUNNING".equals(status)) {
        destroyContainerByName(containerName)
        containersId -= containerName
        printLnWithTimestamp(s"Container $containerName destroyed")
      } else {
        fail(s"job of app [$appId] is not on status RUNNING. Status is: $status")
      }
    }
  }

  def waitForAppSuccess(appId: String, sec: Int): Unit = {
    retry(sec * 1000 millis, 100 millis) {
      val status = InsightEdgeAdminUtils.isAppCompletedHistoryServer(getMasterIp(), appId).get(0).asInstanceOf[JSONObject].get("status").toString
      if (!"SUCCEEDED".equals(status)) {
        fail(s"job of app [$appId] is not on status SUCCEEDED. Status is: $status")
      }
    }
  }
  /*
  Executor logs are missing from test output. This function prints to runner output the html of a single executor.
  is not used anywhere yet but will be useful in the future
   */
  def printLogFromExecutor(appId: String): Unit = {
    var body: JSONArray = new JSONArray
    var i: Int = 0

    while (i < 5) {
      retry(10000 millis, 100 millis) {
        body = getBody(wsClient.url(s"http://$getMasterIp:18080/api/v1/applications/$appId/executors").get())
        printLnWithTimestamp(s"~~~~~~body: $body")
        printLnWithTimestamp(s"~~~~~~body size: ${body.size()}")
      }
      i = i + 1
      //      Thread.sleep(1000)
    }

    val executorLogsObj = parser.parse(body.get(1).asInstanceOf[JSONObject].get("executorLogs").toString).asInstanceOf[JSONObject]
    printLnWithTimestamp(s"~~~~~~ executorLogsObj: $executorLogsObj")
    val stoutURL = executorLogsObj.get("stdout").toString
    printLnWithTimestamp(s"~~~~~~ stoutURL: $stoutURL")
    val sterrURL = executorLogsObj.get("stderr").toString
    printLnWithTimestamp(s"~~~~~~ sterrURL: $sterrURL")
    var logOutFromExecutor = ""
    retry(10000 millis, 100 millis) {
      logOutFromExecutor = Await.result(wsClient.url(stoutURL).get(), Duration.Inf).body
    }
    printLnWithTimestamp("logOutFromExecutor: " + logOutFromExecutor)
    var logErrFromExecutor = ""
    retry(10000 millis, 100 millis) {
      logErrFromExecutor = Await.result(wsClient.url(sterrURL).get(), Duration.Inf).body
    }
    printLnWithTimestamp("logErrFromExecutor: " + logErrFromExecutor)

  }

  def datagridNodes(): Map[ProcessingUnitInstance, List[String]] ={
    var spacesOnMachines: Map[ProcessingUnitInstance, List[String]] = Map[ProcessingUnitInstance, List[String]]()

    var result = admin
      .getMachines.waitFor(4, 30, TimeUnit.SECONDS)
    printLnWithTimestamp(s"Found four machines: $result")

    result= admin
      .getSpaces
      .waitFor("demo", 30, TimeUnit.SECONDS)
      .waitFor(6, 60, TimeUnit.SECONDS)
    printLnWithTimestamp(s"Found 6 space instances: $result")

    result = admin
      .getSpaces
      .waitFor("demo", 30, TimeUnit.SECONDS)
      .waitFor(3, SpaceMode.PRIMARY, 60, TimeUnit.SECONDS)
    printLnWithTimestamp(s"Found 3 primaries: $result")

    result= admin
      .getSpaces
      .waitFor("demo", 30, TimeUnit.SECONDS)
      .waitFor(3, SpaceMode.BACKUP, 60, TimeUnit.SECONDS)
    printLnWithTimestamp(s"Found 3 backups: $result")

    admin.getMachines.getMachines.foreach(
      m => m.getProcessingUnitInstances.foreach(
        puInstance => spacesOnMachines += (puInstance -> List(m.getHostAddress, puInstance.getSpaceInstance.getMode.name())))
    )
    spacesOnMachines
  }

  def shutdown(): Unit = {
    printLnWithTimestamp("Resetting setenv-overrides.sh file")
    execAndWaitFor(containersId("master1"), s"cp -p /opt/insightedge/bin/setenv-overrides.sh.bck /opt/insightedge/bin/setenv-overrides.sh")
    printLnWithTimestamp("Stopping docker container")
    containersId foreach ((t2) => docker.removeContainer(t2._1, RemoveContainerParam.forceKill()))
    docker.close()
    wsClient.close()
  }
}