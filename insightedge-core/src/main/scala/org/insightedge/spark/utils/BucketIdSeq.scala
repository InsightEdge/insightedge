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
    if (bucketId >= InsightEdgeConstants.BucketsCount) {
      bucketId = 0
    }
    bucketId
  }

}
