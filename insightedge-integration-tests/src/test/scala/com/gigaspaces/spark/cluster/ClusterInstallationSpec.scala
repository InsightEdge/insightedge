package com.gigaspaces.spark.cluster

import java.io.File

import org.scalatest.FlatSpec

import scala.language.postfixOps

/**
  * @author Oleksiy_Dyagilev
  */
class ClusterInstallationSpec extends FlatSpec {

  val PackagerDirName = "insightedge-packager"

  "test" should "test" in {
    val packagerDir = findPackagerDir(new File("."))
      .getOrElse(fail(s"Cannot find $PackagerDirName directory"))

    val targetDir = packagerDir.getAbsolutePath + "/target"
    val zipFileMask = targetDir + "/*.zip"

    import sys.process._
    println(s"Unpacking zip in $targetDir")
    s"unzip $zipFileMask -d $targetDir" !



  }


  /**
    * Looks for `packager` directory no matter where this test executed from ... command line, IDE, etc
    */
  def findPackagerDir(findFrom: File): Option[File] = {
    println(s"Looking for $PackagerDirName ... checking ${findFrom.getAbsolutePath}")
    findFrom.getName match {
      case "" => None
      case PackagerDirName => Some(findFrom)
      case _ =>
        val parent = new File(findFrom.getAbsoluteFile.getParent)
        parent
          .listFiles()
          .filter(_.isDirectory)
          .find(_.getName == PackagerDirName)
          .orElse(findPackagerDir(parent))

    }
  }


}
