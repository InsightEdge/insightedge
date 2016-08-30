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

package org.insightedge.spark.impl

private[spark] class ProfilingIterator[T](delegate: Iterator[T]) extends Iterator[T] {
  private var time: Long = 0
  private var finished: Boolean = false
  private var _count = 0

  def count() = _count


  override def hasNext: Boolean = {
    val start = System.nanoTime()
    val result = delegate.hasNext
    time += System.nanoTime() - start

    if (!result) {
      if (finished) {
        println("iterator hasNext called after finished")
      } else {
        finished = true
        val shortTime = (BigDecimal(time) / 1000000000).setScale(5, BigDecimal.RoundingMode.HALF_UP)
        println("iterator accumulated " + shortTime + " seconds")
      }
    }

    result
  }

  override def next(): T = {
    val start = System.nanoTime()
    val result = delegate.next()
    time += System.nanoTime() - start
    _count +=1
    result
  }
}
