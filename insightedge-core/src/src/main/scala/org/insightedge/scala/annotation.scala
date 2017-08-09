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

package org.insightedge.scala

object annotation {

  import com.gigaspaces.annotation.pojo
  import org.openspaces.spatial

  import scala.annotation.meta.beanGetter

  type SpaceClass = pojo.SpaceClass
  type SpaceClassConstructor = pojo.SpaceClassConstructor

  // Enhance space annotations with @beanGetter property
  type SpaceDynamicProperties = pojo.SpaceDynamicProperties@beanGetter
  type SpaceExclude = pojo.SpaceExclude@beanGetter
  type SpaceFifoGroupingIndex = pojo.SpaceFifoGroupingIndex@beanGetter
  type SpaceFifoGroupingProperty = pojo.SpaceFifoGroupingProperty@beanGetter
  type SpaceId = pojo.SpaceId@beanGetter
  type SpaceIndex = pojo.SpaceIndex@beanGetter
  type SpaceIndexes = pojo.SpaceIndexes@beanGetter
  type SpaceLeaseExpiration = pojo.SpaceLeaseExpiration@beanGetter
  type SpacePersist = pojo.SpacePersist@beanGetter
  type SpaceProperty = pojo.SpaceProperty@beanGetter
  type SpaceRouting = pojo.SpaceRouting@beanGetter
  type SpaceStorageType = pojo.SpaceStorageType@beanGetter
  type SpaceVersion = pojo.SpaceVersion@beanGetter
  type SpaceSpatialIndex = spatial.SpaceSpatialIndex@beanGetter
  type SpaceSpatialIndexes = spatial.SpaceSpatialIndexes@beanGetter

}
