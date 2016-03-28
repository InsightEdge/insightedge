package com.gigaspaces.spark.impl

import org.apache.spark.Partition

private[spark] case class GigaSpacesPartition(
                                               id: Int,
                                               hostName: String,
                                               gigaSpaceContainerName: String,
                                               bucketRangeBottom: Option[Int] = None,
                                               bucketRangeTop: Option[Int] = None
                                               ) extends Partition {
  override def index: Int = id
}
