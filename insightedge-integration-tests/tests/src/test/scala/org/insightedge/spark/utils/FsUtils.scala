/*
 * Copyright (c) 2016, GigaSpaces Technologies, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.insightedge.spark.utils

import java.io.File
import org.insightedge.spark.utils.TestUtils.printLnWithTimestamp

/**
  * @author Danylo_Hurin.
  */
object FsUtils {

  val PackagerDirName = "insightedge-packager"

  /**
    * Looks for `packager` directory no matter where this test executed from ... command line, IDE, etc
    */
  def findPackagerDir(findFrom: File): Option[File] = {
    def log(s: File) = printLnWithTimestamp(s"Looking for $PackagerDirName ... checking $s")
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
