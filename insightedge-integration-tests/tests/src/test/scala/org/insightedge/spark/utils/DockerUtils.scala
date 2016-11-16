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

import scala.language.postfixOps
import sys.process._

/**
  * @author Oleksiy_Dyagilev
  */
object DockerUtils {

  /**
    * runs command in container, blocks until process is finished and returns the exit code
    */
  def dockerExec(containerId: String, command: String): Int = {
    val processCommand = s"docker exec $containerId $command"
    println(s"running command: $processCommand")
    processCommand !
  }

}
