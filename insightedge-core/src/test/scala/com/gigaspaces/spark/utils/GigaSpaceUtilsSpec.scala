package com.gigaspaces.spark.utils

import com.gigaspaces.spark.impl.GigaSpacesPartition
import org.scalatest.FunSpec

/**
 * @author Oleksiy_Dyagilev
 */
class GigaSpaceUtilsSpec extends FunSpec {

  it("should equally split 10 by 2") {
    val result = GigaSpaceUtils.equallySplit(10, 5)
    val expected = Seq(2, 2, 2, 2, 2)
    assert(result == expected)
  }

  it("should equally split 10 by 3") {
    val result = GigaSpaceUtils.equallySplit(10, 3)
    val expected = Seq(4, 3, 3)
    assert(result == expected)
  }

  it("should split 2 grid partitions into 8 spark partitions") {
    val gridPartitions = Seq(
      GigaSpacesPartition(0, "host", "cont1"),
      GigaSpacesPartition(1, "host", "cont2")
    )
    val result = GigaSpaceUtils.splitPartitionsByBuckets(gridPartitions, None)
    val expected = Seq(
      GigaSpacesPartition(0, "host", "cont1", Some(0), Some(32)),
      GigaSpacesPartition(1, "host", "cont1", Some(32), Some(64)),
      GigaSpacesPartition(2, "host", "cont1", Some(64), Some(96)),
      GigaSpacesPartition(3, "host", "cont1", Some(96), Some(128)),

      GigaSpacesPartition(4, "host", "cont2", Some(0), Some(32)),
      GigaSpacesPartition(5, "host", "cont2", Some(32), Some(64)),
      GigaSpacesPartition(6, "host", "cont2", Some(64), Some(96)),
      GigaSpacesPartition(7, "host", "cont2", Some(96), Some(128))
    )
    assert(result == expected)
  }

  it("should split 3 grid partitions into 9 spark partitions (x3)") {
    val gridPartitions = Seq(
      GigaSpacesPartition(0, "host1", "cont1"),
      GigaSpacesPartition(1, "host1", "cont2"),
      GigaSpacesPartition(2, "host2", "cont3")
    )
    val result = GigaSpaceUtils.splitPartitionsByBuckets(gridPartitions, Some(3))
    // 128 buckets split 3 = 43,43,42 buckets per spark node
    val expected = Seq(
      GigaSpacesPartition(0, "host1", "cont1", Some(0), Some(43)),
      GigaSpacesPartition(1, "host1", "cont1", Some(43), Some(86)),
      GigaSpacesPartition(2, "host1", "cont1", Some(86), Some(128)),

      GigaSpacesPartition(3, "host1", "cont2", Some(0), Some(43)),
      GigaSpacesPartition(4, "host1", "cont2", Some(43), Some(86)),
      GigaSpacesPartition(5, "host1", "cont2", Some(86), Some(128)),

      GigaSpacesPartition(6, "host2", "cont3", Some(0), Some(43)),
      GigaSpacesPartition(7, "host2", "cont3", Some(43), Some(86)),
      GigaSpacesPartition(8, "host2", "cont3", Some(86), Some(128))
    )
    assert(result == expected)
  }

}
