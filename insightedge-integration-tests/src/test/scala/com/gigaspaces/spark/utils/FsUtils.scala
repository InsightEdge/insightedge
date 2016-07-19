package com.gigaspaces.spark.utils

import java.io.File

/**
  * @author Danylo_Hurin.
  */
object FsUtils {

  /**
    * Looks for `packager` directory no matter where this test executed from ... command line, IDE, etc
    */
  def findPackagerDir(findFrom: File, packagerDirName: String): Option[File] = {
    def log(s: File) = println(s"Looking for $packagerDirName ... checking $s")
    log(findFrom)

    findFrom.getName match {
      case "" => None
      case `packagerDirName` => Some(findFrom)
      case _ =>
        val parent = new File(findFrom.getAbsoluteFile.getParent)
        parent
          .listFiles()
          .filter(_.isDirectory)
          .find(dir => {log(dir); dir.getName == packagerDirName})
          .orElse(findPackagerDir(parent, packagerDirName))

    }
  }

}
