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

package org.apache.spark.sql.insightedge

import com.gigaspaces.document.SpaceDocument
import com.gigaspaces.metadata.SpaceTypeDescriptorBuilder
import com.j_spaces.core.client.SQLQuery
import org.apache.spark.sql.SaveMode
import org.insightedge.spark.fixture.InsightEdge
import org.insightedge.spark.implicits.all._
import org.insightedge.spark.rdd.{Data, JData}
import org.insightedge.spark.utils.{JavaSpaceClass, ScalaSpaceClass}
import org.scalatest.fixture


//TODO check
class DataSetPersistSpec extends fixture.FlatSpec with InsightEdge {

  it should "persist with simplified syntax" taggedAs ScalaSpaceClass in { f =>
    writeDataSeqToDataGrid(1000)
    val table = randomString()
    val spark = f.spark
    import spark.implicits._
    val ds = spark.read.grid.loadClass[Data].as[Data]
    ds.filter(ds("routing") > 500).write.grid.mode(SaveMode.Overwrite).save(table)

    val readDf = spark.read.grid.load(table)
    val count = readDf.select("routing").count()
    assert(count == 500)

    readDf.printSchema()
  }

  it should "persist with simplified syntax [java]" taggedAs JavaSpaceClass in { f=>
    writeJDataSeqToDataGrid(1000)
    val table = randomString()
    val spark = f.spark
    implicit val jDataEncoder = org.apache.spark.sql.Encoders.bean(classOf[JData])
    val df = spark.read.grid.loadClass[JData].as[JData]
    df.filter(df("routing") > 500).write.grid.mode(SaveMode.Overwrite).save(table)

    val readDf = spark.read.grid.load(table)
    val count = readDf.select("routing").count()
    assert(count == 500)

    readDf.printSchema()
  }

  it should "persist without implicits" taggedAs ScalaSpaceClass in { f=>
    writeDataSeqToDataGrid(1000)
    val table = randomString()

    val spark = f.spark
    import spark.implicits._
    val ds = spark.read
      .format("org.apache.spark.sql.insightedge")
      .option("class", classOf[Data].getName)
      .load()
      .as[Data]
    ds.filter(ds("routing") > 500)
      .write
      .mode(SaveMode.Overwrite)
      .format("org.apache.spark.sql.insightedge")
      .save(table)

    val readDf = spark.read.grid.load(table)
    val count = readDf.select("routing").count()
    assert(count == 500)

    readDf.printSchema()
  }

  it should "fail to persist with ErrorIfExists mode" taggedAs ScalaSpaceClass in { f=>
    writeDataSeqToDataGrid(1000)
    val table = randomString()
    val spark = f.spark
    import spark.implicits._
    val ds = spark.read.grid.loadClass[Data].as[Data]
    ds.filter(ds("routing") > 500).write.grid.mode(SaveMode.ErrorIfExists).save(table)

    val thrown = intercept[IllegalStateException] {
      ds.filter(ds("routing") < 500).write.grid.mode(SaveMode.ErrorIfExists).save(table)
    }
    println(thrown.getMessage)
  }

  it should "clear before write with Overwrite mode" taggedAs ScalaSpaceClass in { f=>
    writeDataSeqToDataGrid(1000)
    val table = randomString()
    val spark = f.spark
    import spark.implicits._
    val ds = spark.read.grid.loadClass[Data].as[Data]
    ds.filter(ds("routing") > 500).write.grid.mode(SaveMode.Append).save(table)
    assert(spark.read.grid.load(table).count() == 500)

    ds.filter(ds("routing") <= 200).write.grid.mode(SaveMode.Overwrite).save(table)
    assert(spark.read.grid.load(table).count() == 200)
  }

  it should "not write with Ignore mode" taggedAs ScalaSpaceClass in { f=>
    writeDataSeqToDataGrid(1000)
    val table = randomString()
    val spark = f.spark
    import spark.implicits._
    val ds = spark.read.grid.loadClass[Data].as[Data]
    ds.filter(ds("routing") > 500).write.grid.mode(SaveMode.Append).save(table)
    assert(spark.read.grid.load(table).count() == 500)

    ds.filter(ds("routing") <= 200).write.grid.mode(SaveMode.Ignore).save(table)
    assert(spark.read.grid.load(table).count() == 500)
  }

  it should "override document schema" taggedAs ScalaSpaceClass in { f=>
    writeDataSeqToDataGrid(1000)
    val table = randomString()
    val spark = f.spark
    import spark.implicits._
    val ds = spark.read.grid.loadClass[Data].as[Data]
    // persist with modified schema
    ds.select("id", "data").write.grid.save(table)
    // persist with original schema
    ds.write.grid.mode(SaveMode.Overwrite).save(table)
    // persist with modified schema again
    ds.select("id").write.grid.mode(SaveMode.Overwrite).save(table)
  }
}