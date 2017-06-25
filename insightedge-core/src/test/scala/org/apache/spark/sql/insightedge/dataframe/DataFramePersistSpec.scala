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

import com.gigaspaces.document.SpaceDocument
import com.gigaspaces.metadata.SpaceTypeDescriptorBuilder
import com.j_spaces.core.client.SQLQuery
import org.apache.spark.sql.SaveMode
import org.insightedge.spark.fixture.InsightEdge
import org.insightedge.spark.implicits.all._
import org.insightedge.spark.rdd.{Data, JData}
import org.insightedge.spark.utils.{JavaSpaceClass, ScalaSpaceClass}
import org.scalatest.fixture

class DataFramePersistSpec extends fixture.FlatSpec with InsightEdge {

  it should "persist with simplified syntax" taggedAs ScalaSpaceClass in { ie=>
    writeDataSeqToDataGrid(1000)
    val table = randomString()
    val spark = ie.spark
    val df = spark.read.grid.loadDF[Data]
    df.filter(df("routing") > 500).write.grid.mode(SaveMode.Overwrite).save(table)

    val readDf = spark.read.grid.load(table)
    val count = readDf.select("routing").count()
    assert(count == 500)

    readDf.printSchema()
  }

  it should "persist with simplified syntax [java]" taggedAs JavaSpaceClass in { ie=>
    writeJDataSeqToDataGrid(1000)
    val table = randomString()
    val spark = ie.spark
    val df = spark.read.grid.loadDF[JData]
    df.filter(df("routing") > 500).write.grid.mode(SaveMode.Overwrite).save(table)

    val readDf = spark.read.grid.load(table)
    val count = readDf.select("routing").count()
    assert(count == 500)

    readDf.printSchema()
  }

  it should "persist without implicits" taggedAs ScalaSpaceClass in { ie=>
    writeDataSeqToDataGrid(1000)
    val table = randomString()
    val spark = ie.spark
    val df = spark.read
      .format("org.apache.spark.sql.insightedge")
      .option("class", classOf[Data].getName)
      .load()
    df.filter(df("routing") > 500)
      .write
      .mode(SaveMode.Overwrite)
      .format("org.apache.spark.sql.insightedge")
      .save(table)

    val readDf = spark.read.grid.load(table)
    val count = readDf.select("routing").count()
    assert(count == 500)

    readDf.printSchema()
  }

  it should "fail to persist with ErrorIfExists mode" taggedAs ScalaSpaceClass in { ie=>
    writeDataSeqToDataGrid(1000)
    val table = randomString()
    val spark = ie.spark
    val df = spark.read.grid.loadDF[Data]
    df.filter(df("routing") > 500).write.grid.mode(SaveMode.ErrorIfExists).save(table)

    val thrown = intercept[IllegalStateException] {
      df.filter(df("routing") < 500).write.grid.mode(SaveMode.ErrorIfExists).save(table)
    }
    println(thrown.getMessage)
  }

  it should "clear before write with Overwrite mode" taggedAs ScalaSpaceClass in { ie=>
    writeDataSeqToDataGrid(1000)
    val table = randomString()
    val spark = ie.spark
    val df = spark.read.grid.loadDF[Data]
    df.filter(df("routing") > 500).write.grid.mode(SaveMode.Append).save(table)
    assert(spark.read.grid.load(table).count() == 500)

    df.filter(df("routing") <= 200).write.grid.mode(SaveMode.Overwrite).save(table)
    assert(spark.read.grid.load(table).count() == 200)
  }

  it should "not write with Ignore mode" taggedAs ScalaSpaceClass in { ie=>
    writeDataSeqToDataGrid(1000)
    val table = randomString()
    val spark = ie.spark
    val df = spark.read.grid.loadDF[Data]
    df.filter(df("routing") > 500).write.grid.mode(SaveMode.Append).save(table)
    assert(spark.read.grid.load(table).count() == 500)

    df.filter(df("routing") <= 200).write.grid.mode(SaveMode.Ignore).save(table)
    assert(spark.read.grid.load(table).count() == 500)
  }

  it should "override document schema" taggedAs ScalaSpaceClass in { ie=>
    writeDataSeqToDataGrid(1000)
    val table = randomString()
    val spark = ie.spark
    val df = spark.read.grid.loadDF[Data]
    // persist with modified schema
    df.select("id", "data").write.grid.save(table)
    // persist with original schema
    df.write.grid.mode(SaveMode.Overwrite).save(table)
    // persist with modified schema again
    df.select("id").write.grid.mode(SaveMode.Overwrite).save(table)
  }

  /**
    * This is not supported in current XAP release.
    * This will enable converting the dataframes schema into space type descriptor when save is executed.
    * Right now schema is stored as DataFrameSchema object in space.
    */
  ignore should "recreate space type with different schema" in { ie =>
    import collection.JavaConversions._

    val types = ie.spaceProxy.getTypeManager

    val typeName = randomString()

    val firstType = new SpaceTypeDescriptorBuilder(typeName)
      .addFixedProperty("id", classOf[String])
      .addFixedProperty("name", classOf[String])
      .create()

    val secondType = new SpaceTypeDescriptorBuilder(typeName)
      .addFixedProperty("id", classOf[String])
      .addFixedProperty("surname", classOf[String])
      .create()

    val firstEntity = new SpaceDocument(typeName, Map("id" -> "111", "name" -> "Joe"))

    val secondEntity = new SpaceDocument(typeName, Map("id" -> "222", "surname" -> "Wind"))

    types.registerTypeDescriptor(firstType)
    ie.spaceProxy.write(firstEntity)
    ie.spaceProxy.takeMultiple(new SQLQuery[SpaceDocument](typeName, "", Seq()).setProjections(""))

    types.registerTypeDescriptor(secondType)
    ie.spaceProxy.write(secondEntity)
  }

}
