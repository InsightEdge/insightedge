/*
 * Copyright (c) 2016, GigaSpaces Technologies, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.insightedge.spark.utils

import org.insightedge.spark.impl.InsightEdgePartition
import org.scalatest.FunSpec

/**
 * @author Oleksiy_Dyagilev
 */
class InsightEdgeUtilsSpec extends FunSpec {

  it("should equally split 10 by 2") {
    val result = GridProxyUtils.equallySplit(10, 5)
    val expected = Seq(2, 2, 2, 2, 2)
    assert(result == expected)
  }

  it("should equally split 10 by 3") {
    val result = GridProxyUtils.equallySplit(10, 3)
    val expected = Seq(4, 3, 3)
    assert(result == expected)
  }

  it("should split 2 grid partitions into 8 spark partitions") {
    val gridPartitions = Seq(
      InsightEdgePartition(0, "host", "cont1"),
      InsightEdgePartition(1, "host", "cont2")
    )
    val result = GridProxyUtils.splitPartitionsByBuckets(gridPartitions, None)
    val expected = Seq(
      InsightEdgePartition(0, "host", "cont1", Some(0), Some(32)),
      InsightEdgePartition(2, "host", "cont1", Some(32), Some(64)),
      InsightEdgePartition(4, "host", "cont1", Some(64), Some(96)),
      InsightEdgePartition(6, "host", "cont1", Some(96), Some(128)),

      InsightEdgePartition(1, "host", "cont2", Some(0), Some(32)),
      InsightEdgePartition(3, "host", "cont2", Some(32), Some(64)),
      InsightEdgePartition(5, "host", "cont2", Some(64), Some(96)),
      InsightEdgePartition(7, "host", "cont2", Some(96), Some(128))
    )
    assert(result == expected)
  }

  it("should split 3 grid partitions into 9 spark partitions (x3)") {
    val gridPartitions = Seq(
      InsightEdgePartition(0, "host1", "cont1"),
      InsightEdgePartition(1, "host1", "cont2"),
      InsightEdgePartition(2, "host2", "cont3")
    )
    val result = GridProxyUtils.splitPartitionsByBuckets(gridPartitions, Some(3))
    // 128 buckets split 3 = 43,43,42 buckets per spark node
    val expected = Seq(
      InsightEdgePartition(0, "host1", "cont1", Some(0), Some(43)),
      InsightEdgePartition(3, "host1", "cont1", Some(43), Some(86)),
      InsightEdgePartition(6, "host1", "cont1", Some(86), Some(128)),

      InsightEdgePartition(1, "host1", "cont2", Some(0), Some(43)),
      InsightEdgePartition(4, "host1", "cont2", Some(43), Some(86)),
      InsightEdgePartition(7, "host1", "cont2", Some(86), Some(128)),

      InsightEdgePartition(2, "host2", "cont3", Some(0), Some(43)),
      InsightEdgePartition(5, "host2", "cont3", Some(43), Some(86)),
      InsightEdgePartition(8, "host2", "cont3", Some(86), Some(128))
    )
    assert(result == expected)
  }

}
