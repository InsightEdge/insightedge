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

object Profiler {


  /**
    * Profiles your code work time.
    */
  def profile[R](message: String)(logFunction: String => Unit)(block: => R): R = {
    val start = System.nanoTime()
    val result = block
    val stop = System.nanoTime()
    val time = (BigDecimal(stop - start) / 1000000000).setScale(5, BigDecimal.RoundingMode.HALF_UP)
    logFunction(s"$message took " + time + " seconds")
    result
  }

}
