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

package org.apache.spark.sql.insightedge.dataset

import org.apache.spark.sql.AnalysisException
import org.insightedge.spark.fixture.InsightEdge
import org.insightedge.spark.implicits.all._
import org.insightedge.spark.rdd.{Data, JData}
import org.insightedge.spark.utils.{JavaSpaceClass, ScalaSpaceClass}
import org.scalatest.fixture

class DataSetQuerySpec extends fixture.FlatSpec with InsightEdge {

  it should "select one field" taggedAs ScalaSpaceClass in { ie=>
    writeDataSeqToDataGrid(1000)
    val spark = ie.spark
    import spark.implicits._
    val ds = spark.read.grid[Data].as[Data]
    val increasedRouting = ds.select( ds("routing") + 10).first().getAs[Long](0)
    assert(increasedRouting >= 10)
  }

  it should "select one field [java]" taggedAs JavaSpaceClass in { ie=>
    writeJDataSeqToDataGrid(1000)
    val spark = ie.spark
    implicit val jDataEncoder = org.apache.spark.sql.Encoders.bean(classOf[JData])
    val ds = spark.read.grid[JData].as[JData]
    ds.printSchema()

    val increasedRouting = ds.select(ds("routing") + 10).first().getAs[Long](0)
    assert(increasedRouting >= 10)
  }

  it should "filter by one field" taggedAs ScalaSpaceClass in { ie=>
    writeDataSeqToDataGrid(1000)
    val spark = ie.spark
    import spark.implicits._
    val ds = spark.read.grid[Data].as[Data]
    val count1 = ds.filter(ds("routing") > 500).count()
    val count2 = ds.filter( o => o.routing > 500).count()
    assert(count1 == 500)
    assert(count2 == 500)
  }

  it should "group by field" taggedAs ScalaSpaceClass in { ie=>
    writeDataSeqToDataGrid(1000)
    val spark = ie.spark
    import spark.implicits._
    val ds = spark.read.grid[Data].as[Data]
    val count = ds.groupBy("routing").count().count()
    assert(count == 1000)
  }

  it should "group by field [java]" taggedAs JavaSpaceClass in { ie=>
    writeJDataSeqToDataGrid(1000)
    val spark = ie.spark
    implicit val jDataEncoder = org.apache.spark.sql.Encoders.bean(classOf[JData])
    val ds = spark.read.grid[JData].as[JData]
    val count = ds.groupBy("routing").count().count()
    assert(count == 1000)
  }

  it should "fail to resolve column that's not in class" taggedAs ScalaSpaceClass in { ie=>
    writeDataSeqToDataGrid(1000)
    val spark = ie.spark
    import spark.implicits._
    val ds = spark.read.grid[Data].as[Data]
    intercept[AnalysisException] {
      val count = ds.select(ds("abc")).count()
    }
  }
}