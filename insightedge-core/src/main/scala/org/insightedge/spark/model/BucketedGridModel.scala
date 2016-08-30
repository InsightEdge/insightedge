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

package org.insightedge.spark.model

import com.gigaspaces.metadata.index.SpaceIndexType
import org.insightedge.scala.annotation
import annotation._

import scala.annotation.meta.{beanGetter, getter}
import scala.beans.BeanProperty

/**
 * Trait used to define bucketed space classes. Bucketing allows to have more Spark partitions than Data Grid partitions, i.e.
 * splitting Data Grid partitions into several buckets and assigning a bucket per Spark partition.
 *
 * @author Leonid_Poliakov
 */

trait BucketedGridModel {

  // TODO: check if index is applied
  // compilation warning is due to https://issues.scala-lang.org/browse/SI-8813
  @BeanProperty
  @SpaceIndex(`type` = SpaceIndexType.EXTENDED)
  var metaBucketId: Integer = _
}