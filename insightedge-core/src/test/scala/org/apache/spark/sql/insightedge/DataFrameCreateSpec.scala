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
import org.insightedge.spark.fixture.{InsightEdge, IEConfig}
import org.insightedge.spark.implicits.all._
import org.insightedge.spark.rdd.{BucketedData, Data, JBucketedData}
import org.insightedge.spark.utils.JavaSpaceClass
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.insightedge.model.Address
import org.apache.spark.sql.types._
import org.insightedge.spark.fixture.Spark
import org.insightedge.spark.rdd.JData
import org.insightedge.spark.utils.{JavaSpaceClass, ScalaSpaceClass}
import org.scalatest.FlatSpec

import scala.collection.JavaConversions._

class DataFrameCreateSpec extends FlatSpec with IEConfig with InsightEdge with Spark {

  it should "create dataframe with insightedge format" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)

    val df = sql.read
      .format("org.apache.spark.sql.insightedge")
      .option("class", classOf[Data].getName)
      .load()
    assert(df.count() == 1000)
    assert(df.rdd.partitions.length == NumberOfGridPartitions)
  }

  it should "create dataframe with insightedge format [java]" taggedAs JavaSpaceClass in {
    writeJDataSeqToDataGrid(1000)

    val df = sql.read
      .format("org.apache.spark.sql.insightedge")
      .option("class", classOf[JData].getName)
      .load()
    assert(df.count() == 1000)
    assert(df.rdd.partitions.length == NumberOfGridPartitions)
  }

  it should "fail to create dataframe with insightedge format without class or collection provided" taggedAs ScalaSpaceClass in {
    val thrown = intercept[IllegalArgumentException] {
      val df = sql.read
        .format("org.apache.spark.sql.insightedge")
        .load()
    }
    assert(thrown.getMessage == "'path', 'collection' or 'class' must be specified")
  }

  it should "create dataframe with implicits" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)

    val df = sql.read.grid.loadClass[Data]
    assert(df.count() == 1000)
  }

  it should "create dataframe with implicits [java]" taggedAs JavaSpaceClass in {
    writeJDataSeqToDataGrid(1000)

    val df = sql.read.grid.loadClass[JData]
    assert(df.count() == 1000)
  }

  it should "create dataframe with SQL syntax" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)

    sql.sql(
      s"""
         |create temporary table dataTable
         |using org.apache.spark.sql.insightedge
         |options (class "${classOf[Data].getName}")
      """.stripMargin)

    val count = sql.sql("select routing from dataTable").collect().length
    assert(count == 1000)
  }

  it should "create dataframe with SQL syntax [java]" taggedAs JavaSpaceClass in {
    writeJDataSeqToDataGrid(1000)

    sql.sql(
      s"""
         |create temporary table dataTable
         |using org.apache.spark.sql.insightedge
         |options (class "${classOf[JData].getName}")
      """.stripMargin)

    val count = sql.sql("select routing from dataTable").collect().length
    assert(count == 1000)
  }

  it should "load dataframe with 'collection' or 'path' option" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)

    val collectionName = randomString()
    val df = sql.read.grid.loadClass[Data]
    df.write.grid.save(collectionName)

    val fromGrid = sql.read.format("org.apache.spark.sql.insightedge").option("collection", collectionName).load()
    assert(fromGrid.count() == 1000)

    val fromGrid2 = sql.read.format("org.apache.spark.sql.insightedge").load(collectionName)
    assert(fromGrid2.count() == 1000)
  }

  it should "create dataframe from bucketed type with 'splitCount' option" taggedAs ScalaSpaceClass in {
    writeBucketedDataSeqToDataGrid(1000)

    val df = sql.read.grid
      .option("class", classOf[BucketedData].getName)
      .option("splitCount", "4")
      .load()
    assert(df.count() == 1000)
    assert(df.rdd.partitions.length == 4 * NumberOfGridPartitions)
  }

  it should "create dataframe from bucketed type with 'splitCount' option [java]" taggedAs ScalaSpaceClass in {
    writeJBucketedDataSeqToDataGrid(1000)

    val df = sql.read.grid
      .option("class", classOf[JBucketedData].getName)
      .option("splitCount", "4")
      .load()
    assert(df.count() == 1000)
    assert(df.rdd.partitions.length == 4 * NumberOfGridPartitions)
  }

  it should "load dataframe from existing space documents with provided schema" in {
    val collectionName = randomString()

    spaceProxy.getTypeManager.registerTypeDescriptor(
      new SpaceTypeDescriptorBuilder(collectionName)
        .idProperty("personId")
        .routingProperty("name")
        .create()
    )

    spaceProxy.writeMultiple(Array(
      new SpaceDocument(collectionName, Map(
        "personId" -> "111",
        "name" -> "John", "surname" -> "Wind", "age" -> Integer.valueOf(32),
        "address" -> Address("New York", "NY"), "jaddress" -> new JAddress("New York", "NY")
      )),
      new SpaceDocument(collectionName, Map(
        "personId" -> "222",
        "name" -> "Mike", "surname" -> "Green", "age" -> Integer.valueOf(20),
        "address" -> Address("Charlotte", "NC"), "jaddress" -> new JAddress("Charlotte", "NC")
      ))
    ))

    val dataFrameAsserts = (dataFrame: DataFrame) => {
      assert(dataFrame.count() == 2)
      assert(dataFrame.filter(dataFrame("name") equalTo "John").count() == 1)
      assert(dataFrame.filter(dataFrame("age") < 30).count() == 1)
      assert(dataFrame.filter(dataFrame("address.state") equalTo "NY").count() == 1)
      assert(dataFrame.filter(dataFrame("jaddress.city") equalTo "Charlotte").count() == 1)
    }

    val schemaAsserts = (schema: StructType) => {
      assert(schema.fieldNames.contains("name"))
      assert(schema.fieldNames.contains("surname"))
      assert(schema.fieldNames.contains("age"))
      assert(schema.fieldNames.contains("address"))
      assert(schema.fieldNames.contains("jaddress"))

      assert(schema.get(schema.getFieldIndex("name").get).dataType == StringType)
      assert(schema.get(schema.getFieldIndex("surname").get).dataType == StringType)
      assert(schema.get(schema.getFieldIndex("age").get).dataType == IntegerType)
      assert(schema.get(schema.getFieldIndex("address").get).dataType.isInstanceOf[StructType])
      assert(schema.get(schema.getFieldIndex("jaddress").get).dataType.isInstanceOf[StructType])
    }

    val addressType = StructType(Seq(
      StructField("state", StringType, nullable = true),
      StructField("city", StringType, nullable = true)
    ))

    val df = sql.read.grid.schema(
      StructType(Seq(
        StructField("personId", StringType, nullable = false),
        StructField("name", StringType, nullable = true),
        StructField("surname", StringType, nullable = true),
        StructField("age", IntegerType, nullable = false),
        StructField("address", addressType.copy(), nullable = true, nestedClass[Address]),
        StructField("jaddress", addressType.copy(), nullable = true, nestedClass[JAddress])
      ))
    ).load(collectionName)
    df.printSchema()

    // check schema
    schemaAsserts(df.schema)
    // check content
    dataFrameAsserts(df)

    // check if dataframe can be persisted
    val tableName = randomString()
    df.write.grid.save(tableName)
    dataFrameAsserts(sql.read.grid.load(tableName))
  }

  it should "load dataframe from existing space documents with empty schema" in {
    val collectionName = randomString()

    spaceProxy.getTypeManager.registerTypeDescriptor(
      new SpaceTypeDescriptorBuilder(collectionName).create()
    )

    spaceProxy.writeMultiple(Array(
      new SpaceDocument(collectionName, Map("name" -> "John", "surname" -> "Wind", "age" -> Integer.valueOf(32))),
      new SpaceDocument(collectionName, Map("name" -> "Mike", "surname" -> "Green", "age" -> Integer.valueOf(20)))
    ))

    val df = sql.read.grid.load(collectionName)
    assert(df.count() == 2)
    assert(df.schema.fields.length == 0)
  }

}
