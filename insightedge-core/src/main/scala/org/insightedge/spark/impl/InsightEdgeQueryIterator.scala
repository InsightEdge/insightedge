package org.insightedge.spark.impl

import com.j_spaces.core.client.GSIterator

private[spark] class InsightEdgeQueryIterator[T](cur: GSIterator) extends Iterator[T] {

  override def hasNext: Boolean = cur.hasNext

  override def next(): T = cur.next().asInstanceOf[T]

}