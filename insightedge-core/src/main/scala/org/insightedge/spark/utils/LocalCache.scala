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

import java.util.concurrent.ConcurrentHashMap

import scala.collection.convert.decorateAsScala._

/**
  * @author Oleksiy_Dyagilev
  */
private[spark] class LocalCache[K, V] {

  protected[utils] val map = new ConcurrentHashMap[K, V]().asScala

  def getOrElseUpdate(k: K, op: => V): V = {
    map.getOrElse(k, updateIfRequired(k, op))
  }

  def get(k: K): Option[V] = {
    map.get(k)
  }

  private def updateIfRequired(k: K, op: => V): V = {
    this.synchronized {
      map.get(k) match {
        case Some(v) => v
        case None =>
          val v = op
          map.put(k, v)
          v
      }
    }
  }

  def size(): Int = map.size

}
