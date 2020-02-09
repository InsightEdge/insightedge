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

package org.apache.spark.sql.insightedge.dataframe

import org.insightedge.spark.fixture.InsightEdge
import org.insightedge.spark.implicits.all._
import org.insightedge.spark.rdd.Data
import org.insightedge.spark.utils.ScalaSpaceClass
import org.scalatest.fixture

import scala.reflect.classTag

class DataFrameGetWithStringSpec extends fixture.FlatSpec with InsightEdge {

  it should "Read data frame as String which was written as POJO with a valid struct Type " taggedAs ScalaSpaceClass in { ie =>

    writeDataSeqToDataGrid(1000)
    val spark= ie.spark
    val df = spark.read.grid("org.insightedge.spark.rdd.Data")
    val fields = classTag[Data].runtimeClass.getDeclaredFields

    // Make sure all the original fields exist.
    assert(fields.size == df.schema.fields.length, "Dataframe should have exactly the number of fields as the class it represents")
    for (field <- fields) {
      assert(df.schema.fieldNames.contains(field.getName))
    }
  }

  it should "Read data frame as Class which was written as POJO with a valid struct Type " taggedAs ScalaSpaceClass in { ie =>

    writeDataSeqToDataGrid(1000)
    val spark= ie.spark
    val df = spark.read.grid[Data]
    val fields = classTag[Data].runtimeClass.getDeclaredFields

    // Make sure all the original fields exist.
    assert(fields.size == df.schema.fields.length, "Dataframe should have exactly the number of fields as the class it represents")
    for (field <- fields) {
      assert(df.schema.fieldNames.contains(field.getName))
    }
  }

}
