package org.insightedge.spark.utils

import java.io.File

/**
  * @author Danylo_Hurin.
  */
object FsUtils {

  val PackagerDirName = "insightedge-packager"

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
