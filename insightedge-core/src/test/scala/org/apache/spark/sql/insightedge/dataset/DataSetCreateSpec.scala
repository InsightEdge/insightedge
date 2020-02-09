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

import com.gigaspaces.document.SpaceDocument
import com.gigaspaces.metadata.SpaceTypeDescriptorBuilder
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.insightedge.model.{Address, DummyPerson}
import org.apache.spark.sql.types._
import org.insightedge.spark.fixture.InsightEdge
import org.insightedge.spark.implicits.all._
import org.insightedge.spark.rdd.{BucketedData, Data, JBucketedData, JData}
import org.insightedge.spark.utils.{JavaSpaceClass, ScalaSpaceClass}
import org.scalatest.{ShouldMatchers, fixture}

import scala.collection.JavaConversions._

class DataSetCreateSpec extends fixture.FlatSpec with InsightEdge with ShouldMatchers {

  it should "create dataset with insightedge format" taggedAs ScalaSpaceClass in { ie =>
    writeDataSeqToDataGrid(1000)
    val spark = ie.spark
    import spark.implicits._
    val ds = spark.read
        .grid[Data].as[Data]

    val filteredResult = ds.filter(d => d.routing > 10).count()
    filteredResult should equal(990)
    ds.count() should equal(1000)
    ds.rdd.partitions.length should equal(NumberOfGridPartitions)
  }

  it should "create dataset with insightedge format [java]" taggedAs JavaSpaceClass in { ie=>
    writeJDataSeqToDataGrid(1000)
    implicit val jDataEncoder = org.apache.spark.sql.Encoders.bean(classOf[JData])
    val spark = ie.spark
    val ds = spark.read
        .grid[JData].as[JData]

    val filteredResult = ds.filter(d => d.getRouting > 10).count()
    filteredResult should equal(990)
    ds.count() should equal(1000)
    ds.rdd.partitions.length should equal(NumberOfGridPartitions)
  }

  it should "create dataset with implicits" taggedAs ScalaSpaceClass in { ie=>
    writeDataSeqToDataGrid(1000)
    val spark = ie.spark
    import spark.implicits._
    val ds = spark.read.grid[Data].as[Data]
    assert(ds.count() == 1000)
  }

  it should "create dataset with implicits [java]" taggedAs JavaSpaceClass in { ie=>
    writeJDataSeqToDataGrid(1000)

    implicit val jDataEncoder = org.apache.spark.sql.Encoders.bean(classOf[JData])
    val spark = ie.spark
    val ds = spark.read.grid[JData].as[JData]
    assert(ds.count() == 1000)
  }

  it should "load dataset with 'collection' or 'path' option" taggedAs ScalaSpaceClass in { ie=>
    writeDataSeqToDataGrid(1000)
    val spark = ie.spark
    import spark.implicits._
    val collectionName = randomString()
    val ds = spark.read.grid[Data].as[Data]
    ds.write.grid(collectionName)

    val fromGridDataSetLong = spark.read.format("org.apache.spark.sql.insightedge").option("collection", collectionName).load().as[Data]
    val fromGridDataSetShort = spark.read.grid(collectionName).as[Data]
    assert(fromGridDataSetShort.count() == 1000)
    assert(fromGridDataSetLong.count() == 1000)
    //collection == path
    val fromGrid2DataSet = spark.read.format("org.apache.spark.sql.insightedge").load(collectionName).as[Data]
    assert(fromGrid2DataSet.count() == 1000)
  }

  it should "create dataframe from bucketed type with 'splitCount' option" taggedAs ScalaSpaceClass in { ie=>
    writeBucketedDataSeqToDataGrid(1000)
    val spark = ie.spark
    import spark.implicits._
    val ds = spark.read.format("org.apache.spark.sql.insightedge")
      .option("class", classOf[BucketedData].getName)
      .option("splitCount", "4")
      .load().as[BucketedData]

    assert(ds.count() == 1000)
    assert(ds.rdd.partitions.length == 4 * NumberOfGridPartitions)
  }

  it should "create dataframe from bucketed type with 'splitCount' option [java]" taggedAs ScalaSpaceClass in { ie=>
    writeJBucketedDataSeqToDataGrid(1000)
    implicit val jBucketDataEncoder = org.apache.spark.sql.Encoders.bean(classOf[JBucketedData])
    val spark = ie.spark
    val ds = spark.read
      .option("splitCount", "4")
      .grid[JBucketedData].as[JBucketedData]
    assert(ds.count() == 1000)
    assert(ds.rdd.partitions.length == 4 * NumberOfGridPartitions)
  }


  it should "load dataset from existing space documents with provided schema" in { ie =>

    val collectionName = randomString()
    val spark = ie.spark
    import spark.implicits._

    ie.spaceProxy.getTypeManager.registerTypeDescriptor(
      new SpaceTypeDescriptorBuilder(collectionName)
        .idProperty("personId")
        .routingProperty("name")
        .create()
    )

    ie.spaceProxy.writeMultiple(Array(
      new SpaceDocument(collectionName, Map(
        "personId" -> "111",
        "name" -> "John",
        "surname" -> "Wind",
        "age" -> Integer.valueOf(32),
        "address" -> Address("New York", "NY")
      )),
      new SpaceDocument(collectionName, Map(
        "personId" -> "222",
        "name" -> "Mike",
        "surname" -> "Green",
        "age" -> Integer.valueOf(20),
        "address" -> Address("Charlotte", "NC")
      ))
    ))

    val dataSetAsserts = (dataSet: Dataset[DummyPerson]) => {
      assert(dataSet.count() == 2)
      assert(dataSet.filter(dataSet("name") equalTo "John").count() == 1)
      assert(dataSet.filter(dataSet("age") < 30).count() == 1)
      assert(dataSet.filter(dataSet("address.state") equalTo "NY").count() == 1)

      assert(dataSet.filter( "age < 30").count() == 1)

      assert(dataSet.filter( o => o.name == "John").count() == 1)
      assert(dataSet.filter( o => o.age < 30 ).count() == 1)
      assert(dataSet.filter( o => o.address.state == "NY").count() == 1)
    }

    val schemaAsserts = (schema: StructType) => {
      assert(schema.fieldNames.contains("name"))
      assert(schema.fieldNames.contains("surname"))
      assert(schema.fieldNames.contains("age"))
      assert(schema.fieldNames.contains("address"))

      assert(schema.get(schema.getFieldIndex("name").get).dataType == StringType)
      assert(schema.get(schema.getFieldIndex("surname").get).dataType == StringType)
      assert(schema.get(schema.getFieldIndex("age").get).dataType == IntegerType)
      assert(schema.get(schema.getFieldIndex("address").get).dataType.isInstanceOf[StructType])
    }

    val addressType = StructType(Seq(
      StructField("state", StringType, nullable = true),
      StructField("city", StringType, nullable = true)
    ))

    val ds = spark.read.schema(
      StructType(Seq(
        StructField("personId", StringType, nullable = false),
        StructField("name", StringType, nullable = true),
        StructField("surname", StringType, nullable = true),
        StructField("age", IntegerType, nullable = false),
        StructField("address", addressType.copy(), nullable = true, nestedClass[Address])
      ))
    ).grid(collectionName).as[DummyPerson]

    ds.printSchema()

    // check schema
    schemaAsserts(ds.schema)
    // check content
    dataSetAsserts(ds)

    // check if dataframe can be persisted
    val tableName = randomString()
    ds.write.grid(tableName)
    dataSetAsserts(spark.read.grid(tableName).as[DummyPerson])
  }
}