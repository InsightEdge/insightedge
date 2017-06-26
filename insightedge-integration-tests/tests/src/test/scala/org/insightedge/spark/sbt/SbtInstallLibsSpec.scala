package org.insightedge.spark.sbt


/**
  * Created by livnat on 6/25/17.
  */
import java.io.File

import org.insightedge.spark.utils.BuildUtils._
import org.insightedge.spark.utils.ProcessUtils._
import org.insightedge.spark.utils.FsUtils._
import org.insightedge.spark.utils.{BuildUtils, LongRunningTest}
import org.scalatest.{BeforeAndAfter, FlatSpec}

class SbtInstallLibsSpec extends FlatSpec with BeforeAndAfter {

  val scriptsDir = getClass.getClassLoader.getResource("docker/maven-install-libs").getFile

  "maven-install-libs.sh" should "install libs into local maven repo" taggedAs LongRunningTest in {
    println(s"Edition: $BuildEdition")
    println(s"Version: $BuildVersion")
    println(s"Git branch: $GitBranch")
    val zipDir = Option(System.getProperty("dist.dir")).getOrElse{
      val packagerDir = findPackagerDir(new File(".")).getOrElse(fail(s"Cannot find $PackagerDirName directory"))
      println(s"Package dir: $packagerDir")
      s"$packagerDir/target/$BuildEdition"
    }
    println(s"Zip dir: $zipDir")

    println(s"Scripts dir: $scriptsDir")
    // workaround for maven plugin bug, it doesn't preserve file permissions
    execAssertSucc(s"chmod +x $scriptsDir/run.sh")
    execAssertSucc(s"chmod +x $scriptsDir/stop.sh")

    // run installation
    execAssertSucc(s"$scriptsDir/run.sh $zipDir $BuildVersion $GitBranch sbt")
  }

  after {
    execAssertSucc(s"$scriptsDir/stop.sh")
  }

}