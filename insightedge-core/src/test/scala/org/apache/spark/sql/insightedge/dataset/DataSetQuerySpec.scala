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
import org.apache.spark.sql.insightedge.JPerson
import org.insightedge.spark.fixture.InsightEdge
import org.insightedge.spark.implicits.all._
import org.insightedge.spark.rdd.{Data, JData}
import org.insightedge.spark.utils.{JavaSpaceClass, ScalaSpaceClass}
import org.scalatest.fixture

class DataSetQuerySpec extends fixture.FlatSpec with InsightEdge {

  it should "read empty classes" taggedAs ScalaSpaceClass in { ie=>
    val spark = ie.spark
    spark.sql(
      s"""
         |create temporary table dataTable
         |using org.apache.spark.sql.insightedge
         |options (class "${classOf[Data].getName}")
      """.stripMargin)

    assert(spark.sql("select * from dataTable where data is null").collect().length == 0)
  }

  it should "select one field" taggedAs ScalaSpaceClass in { ie=>
    writeDataSeqToDataGrid(1000)
    val spark = ie.spark
    import spark.implicits._
    val ds = spark.read.grid.loadDF[Data].as[Data]
    val increasedRouting = ds.select(ds("routing") + 10).first().getAs[Long](0)
    assert(increasedRouting >= 10)
  }

  it should "select one field [java]" taggedAs JavaSpaceClass in { ie=>
    writeJDataSeqToDataGrid(1000)
    val spark = ie.spark
    val ds = spark.read.grid.loadDF[JData]
    ds.printSchema()

    val increasedRouting = ds.select(ds("routing") + 10).first().getAs[Long](0)
    assert(increasedRouting >= 10)
  }

  it should "filter by one field" taggedAs ScalaSpaceClass in { ie=>
    writeDataSeqToDataGrid(1000)
    val spark = ie.spark
    import spark.implicits._
    val ds = spark.read.grid.loadDF[Data].as[Data]
    val count = ds.filter(ds("routing") > 500).count()
    assert(count == 500)
  }

  it should "group by field" taggedAs ScalaSpaceClass in { ie=>
    writeDataSeqToDataGrid(1000)
    val spark = ie.spark
    import spark.implicits._
    val ds = spark.read.grid.loadDF[Data].as[Data]
    val count = ds.groupBy("routing").count().count()
    assert(count == 1000)
  }

  it should "group by field [java]" taggedAs JavaSpaceClass in { ie=>
    writeJDataSeqToDataGrid(1000)
    val spark = ie.spark
    implicit val jDataEncoder = org.apache.spark.sql.Encoders.bean(classOf[JData])
    val ds = spark.read.grid.loadDF[JData].as[JData]
    val count = ds.groupBy("routing").count().count()
    assert(count == 1000)
  }

  it should "fail to resolve column that's not in class" taggedAs ScalaSpaceClass in { ie=>
    writeDataSeqToDataGrid(1000)
    val spark = ie.spark
    import spark.implicits._
    val ds = spark.read.grid.loadDF[Data].as[Data]
    intercept[AnalysisException] {
      val count = ds.select(ds("abc")).count()
    }
  }

  it should "fail to load class" taggedAs ScalaSpaceClass in { ie=>
    val thrown = intercept[ClassNotFoundException] {
      val spark = ie.spark
      spark.read.grid.option("class", "non.existing.Class").load()
    }
    assert(thrown.getMessage equals "non.existing.Class")
  }


}
