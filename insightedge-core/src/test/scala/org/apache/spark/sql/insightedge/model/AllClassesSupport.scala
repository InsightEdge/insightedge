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

import java.sql.{Date, Timestamp}

import org.insightedge.scala.annotation
import annotation._
import org.apache.spark.sql.types.{DateType, MapType, TimestampType}
import org.apache.spark.unsafe.types.CalendarInterval

import scala.beans.BeanProperty

/**
 * Space class for tests
 */
case class AllClassesSupport(

                        @BeanProperty @SpaceId(autoGenerate = true) var id: String
                        ,@BeanProperty @SpaceRouting var routing: Long
                        ,@BeanProperty var decimal1: java.math.BigDecimal
                        ,@BeanProperty var byte1: Array[Byte]
                        ,@BeanProperty var timeStamp1: java.sql.Timestamp
                        ,@BeanProperty var date1: java.sql.Date
                        ,@BeanProperty var arrInt: Array[Int]
                        ,@BeanProperty var list1: List[Int]
                        ,@BeanProperty var list2: java.util.List[Integer]
                        ,@BeanProperty var listString: java.util.List[String]
                            ) {

  def this(routing: Long) = this(null, routing, null, null, null, null, null, null, null, null)

  def this() = this(-1)
}