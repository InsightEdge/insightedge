package org.insightedge.spark.cluster

import java.io.File

import org.insightedge.spark.utils.BuildUtils
import org.insightedge.spark.utils.BuildUtils.BuildEdition
import org.scalatest.{BeforeAndAfter, FlatSpec}
import org.insightedge.spark.utils.ProcessUtils._
import org.insightedge.spark.utils.FsUtils._

/**
  * Setup a cluster of empty docker containers and install InsightEdge there remotely from another container(named Client).
  * Run smoke test after installation.
  *
  * @author Oleksiy_Dyagilev
  */
class ClusterInstallationSpec extends FlatSpec with BeforeAndAfter {

  val scriptsDir = getClass.getClassLoader.getResource("docker/cluster-install/").getFile

  "insightedge.sh" should "install cluster" in {
    val packagerDir = findPackagerDir(new File(".")).getOrElse(fail(s"Cannot find $PackagerDirName directory"))

    println(s"Package dir: $packagerDir")
    val zipDir = s"$packagerDir/target/$BuildEdition"
    println(s"Zip dir: $zipDir")

    println(s"Scripts dir: $scriptsDir")

    // workaround for maven plugin bug, it doesn't preserve file permissions
    execAssertSucc(s"chmod +x $scriptsDir/run.sh")
    execAssertSucc(s"chmod +x $scriptsDir/stop.sh")

    // run installation
    execAssertSucc(s"$scriptsDir/run.sh $zipDir $BuildEdition")
  }

  after {
    execAssertSucc(s"$scriptsDir/stop.sh")
  }

}
