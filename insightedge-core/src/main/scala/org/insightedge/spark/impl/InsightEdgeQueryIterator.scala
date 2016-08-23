package org.insightedge.spark.impl

import com.gigaspaces.client.iterator.SpaceIterator

private[spark] class InsightEdgeQueryIterator[T](cur: SpaceIterator[T]) extends Iterator[T] {

  override def hasNext: Boolean = cur.hasNext

  override def next(): T = cur.next()

}