package com.gigaspaces.spark.impl

import com.j_spaces.core.client.GSIterator

private[spark] class GigaSpacesQueryIterator[T, R](cur: GSIterator, convertFunc: T => R) extends Iterator[R] {

  override def hasNext: Boolean = cur.hasNext

  override def next(): R = {
    convertFunc(cur.next().asInstanceOf[T])
  }
}
