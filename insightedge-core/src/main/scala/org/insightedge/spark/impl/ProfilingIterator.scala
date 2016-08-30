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
