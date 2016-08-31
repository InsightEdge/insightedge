package org.insightedge.spark.impl

import org.apache.spark.Partition

private[spark] case class InsightEdgePartition(
                                                id: Int,
                                                hostName: String,
                                                gridContainerName: String,
                                                bucketRangeBottom: Option[Int] = None,
                                                bucketRangeTop: Option[Int] = None
                                              ) extends Partition {
  override def index: Int = id

  /**
    * override equals and hashcode since it's already overridden in Partition and case class doesn't regenerate own implementation
    */
  override def equals(o: Any): Boolean = o match {
    case that: InsightEdgePartition =>
      that.id == id &&
        that.hostName == hostName &&
        that.gridContainerName == gridContainerName
      that.bucketRangeBottom == bucketRangeBottom
      that.bucketRangeTop == bucketRangeTop
    case _ => false
  }

  override def hashCode(): Int = id
}
