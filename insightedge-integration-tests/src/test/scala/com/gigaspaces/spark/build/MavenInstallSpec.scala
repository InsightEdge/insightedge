package com.gigaspaces.spark.build

import java.io.File

import com.gigaspaces.spark.utils.ProcessUtils._
import org.scalatest.{BeforeAndAfter, FlatSpec}


// TODO rename, rename package
/**
  * @author Danylo_Hurin.
  */
class MavenInstallSpec extends FlatSpec with BeforeAndAfter {

  val PackagerDirName = "insightedge-packager"
  val scriptsDir = getClass.getClassLoader.getResource("docker/maven-install").getFile

  "maven-install.sh" should "install libs into local maven repo" in {
    val packagerDir = findPackagerDir(new File(".")).getOrElse(fail(s"Cannot find $PackagerDirName directory"))
    val edition = Option(System.getProperty("dist.edition")).getOrElse("")
    println(s"Edition: $edition")

    println(s"Package dir: $packagerDir")
    val zipDir = s"$packagerDir/target/$edition"
    println(s"Zip dir: $zipDir")

    println(s"Scripts dir: $scriptsDir")

    // workaround for maven plugin bug, it doesn't preserve file permissions
    execAssertSucc(s"chmod +x $scriptsDir/run.sh")
    execAssertSucc(s"chmod +x $scriptsDir/stop.sh")

    // run installation
    execAssertSucc(s"$scriptsDir/run.sh $zipDir $edition")
  }

  after {
    execAssertSucc(s"$scriptsDir/stop.sh")
  }

  /**
    * Looks for `packager` directory no matter where this test executed from ... command line, IDE, etc
    */
  def findPackagerDir(findFrom: File): Option[File] = {
    def log(s: File) = println(s"Looking for $PackagerDirName ... checking $s")
    log(findFrom)

    findFrom.getName match {
      case "" => None
      case PackagerDirName => Some(findFrom)
      case _ =>
        val parent = new File(findFrom.getAbsoluteFile.getParent)
        parent
          .listFiles()
          .filter(_.isDirectory)
          .find(dir => {log(dir); dir.getName == PackagerDirName})
          .orElse(findPackagerDir(parent))

    }
  }


}
