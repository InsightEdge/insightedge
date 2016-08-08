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
