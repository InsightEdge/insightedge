package com.gigaspaces.spark.utils

/**
  * Bucket id sequence. Generates uniform distribution of bucket ids.
  *
  * The sequence is not thread safe
  *
  * @author Oleksiy_Dyagilev
  */
class BucketIdSeq extends Serializable {

  private var bucketId = -1

  def next(): Int = {
    bucketId += 1
    if (bucketId >= GigaSpaceConstants.BucketsCount) {
      bucketId = 0
    }
    bucketId
  }

}
