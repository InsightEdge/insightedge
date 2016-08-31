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
