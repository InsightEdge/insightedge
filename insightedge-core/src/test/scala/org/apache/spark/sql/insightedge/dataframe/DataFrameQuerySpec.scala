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

import org.apache.spark.sql.AnalysisException
import org.insightedge.spark.fixture.InsightEdge
import org.insightedge.spark.implicits.all._
import org.insightedge.spark.rdd.{Data, JData}
import org.insightedge.spark.utils.{JavaSpaceClass, ScalaSpaceClass}
import org.scalatest.fixture

class DataFrameQuerySpec extends fixture.FlatSpec with InsightEdge {

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
    val df = spark.read.grid.loadClass[Data]
    val increasedRouting = df.select(df("routing") + 10).first().getAs[Long](0)
    assert(increasedRouting >= 10)
  }

  it should "select one field [java]" taggedAs JavaSpaceClass in { ie=>
    writeJDataSeqToDataGrid(1000)
    val spark = ie.spark
    val df = spark.read.grid.loadClass[JData]
    df.printSchema()

    val increasedRouting = df.select(df("routing") + 10).first().getAs[Long](0)
    assert(increasedRouting >= 10)
  }

  it should "filter by one field" taggedAs ScalaSpaceClass in { ie=>
    writeDataSeqToDataGrid(1000)
    val spark = ie.spark
    val df = spark.read.grid.loadClass[Data]
    val count = df.filter(df("routing") > 500).count()
    assert(count == 500)
  }

  it should "group by field" taggedAs ScalaSpaceClass in { ie=>
    writeDataSeqToDataGrid(1000)
    val spark = ie.spark
    val df = spark.read.grid.loadClass[Data]
    val count = df.groupBy("routing").count().count()
    assert(count == 1000)
  }

  it should "group by field [java]" taggedAs JavaSpaceClass in { ie=>
    writeJDataSeqToDataGrid(1000)
    val spark = ie.spark
    val df = spark.read.grid.loadClass[JData]
    val count = df.groupBy("routing").count().count()
    assert(count == 1000)
  }

  it should "fail to resolve column that's not in class" taggedAs ScalaSpaceClass in { ie=>
    writeDataSeqToDataGrid(1000)
    val spark = ie.spark
    val df = spark.read.grid.loadClass[Data]
    intercept[AnalysisException] {
      val count = df.select(df("abc")).count()
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
