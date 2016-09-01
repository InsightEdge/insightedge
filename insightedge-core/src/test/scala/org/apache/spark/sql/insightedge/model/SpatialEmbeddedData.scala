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

package org.apache.spark.sql.insightedge.model

import org.insightedge.scala.annotation
import annotation._
import org.openspaces.spatial.shapes.Point

import scala.beans.BeanProperty

/**
  * Space class for tests
  */
case class SpatialEmbeddedData(
                                @BeanProperty
                                @SpaceId(autoGenerate = true)
                                var id: String,

                                @BeanProperty
                                @SpaceSpatialIndex(path = "point")
                                var location: Location
                              ) {

  def this() = this(null, null)

}

case class Location(@BeanProperty point: Point)