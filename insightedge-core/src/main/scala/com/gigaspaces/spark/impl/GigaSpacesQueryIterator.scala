package com.gigaspaces.spark.impl

import com.j_spaces.core.client.GSIterator

private[spark] class GigaSpacesQueryIterator[T](cur: GSIterator) extends Iterator[T] {

  override def hasNext: Boolean = cur.hasNext

  override def next(): T = cur.next().asInstanceOf[T]

}