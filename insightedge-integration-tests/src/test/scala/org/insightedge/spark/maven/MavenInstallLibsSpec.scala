package org.insightedge.spark.maven

import java.io.File

import org.insightedge.spark.utils.BuildUtils.{BuildEdition, BuildVersion}
import org.insightedge.spark.utils.ProcessUtils._
import org.insightedge.spark.utils.FsUtils._
import org.insightedge.spark.utils.{BuildUtils, LongRunningTest}
import org.scalatest.{BeforeAndAfter, FlatSpec}

/**
  * @author Danylo_Hurin.
  */
class MavenInstallLibsSpec extends FlatSpec with BeforeAndAfter {

  val scriptsDir = getClass.getClassLoader.getResource("docker/maven-install-libs").getFile

  "maven-install-libs.sh" should "install libs into local maven repo" taggedAs LongRunningTest in {
    val packagerDir = findPackagerDir(new File(".")).getOrElse(fail(s"Cannot find $PackagerDirName directory"))
    println(s"Package dir: $packagerDir")
    println(s"Edition: $BuildEdition")
    println(s"Version: $BuildVersion")

    val zipDir = s"$packagerDir/target/$BuildEdition"
    println(s"Zip dir: $zipDir")

    println(s"Scripts dir: $scriptsDir")
    // workaround for maven plugin bug, it doesn't preserve file permissions
    execAssertSucc(s"chmod +x $scriptsDir/run.sh")
    execAssertSucc(s"chmod +x $scriptsDir/stop.sh")

    // run installation
    execAssertSucc(s"$scriptsDir/run.sh $zipDir $BuildVersion")
  }

  after {
    execAssertSucc(s"$scriptsDir/stop.sh")
  }

}
