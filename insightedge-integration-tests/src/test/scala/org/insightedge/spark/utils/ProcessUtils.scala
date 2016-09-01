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

import sys.process._

/**
  * @author Oleksiy_Dyagilev
  */
object ProcessUtils {


  /**
    * Executes given command, blocks until it exits, asserts zero exit code
    */
  def execAssertSucc(cmd: String) = {
    println(s"Executing: $cmd")
    val exitCode = cmd.!
    assert(exitCode == 0, s"Non zero exit code executing $cmd")
  }

}
