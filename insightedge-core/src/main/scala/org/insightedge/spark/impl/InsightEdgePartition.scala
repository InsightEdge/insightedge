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
}
