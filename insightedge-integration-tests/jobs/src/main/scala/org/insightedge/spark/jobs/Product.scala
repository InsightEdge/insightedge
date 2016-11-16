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

package org.insightedge.spark.jobs

import org.insightedge.scala.annotation._

import scala.beans.{BeanProperty, BooleanBeanProperty}

case class Product(
                    @BeanProperty
                    @SpaceId
                    var id: Long,

                    @BeanProperty
                    var description: String,

                    @BeanProperty
                    var quantity: Int,

                    @BooleanBeanProperty
                    var featuredProduct: Boolean
                  ) {

  def this() = this(-1, null, -1, false)

}