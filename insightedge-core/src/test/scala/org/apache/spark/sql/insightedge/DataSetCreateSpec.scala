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
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.insightedge.model.{Address, DummyPerson}
import org.apache.spark.sql.types._
import org.insightedge.spark.fixture.{IEConfig, InsightEdge, Spark}
import org.insightedge.spark.implicits.all._
import org.insightedge.spark.rdd.{BucketedData, Data, JBucketedData, JData}
import org.insightedge.spark.utils.{JavaSpaceClass, ScalaSpaceClass}
import org.scalatest.fixture

import scala.collection.JavaConversions._

class DataSetCreateSpec extends fixture.FlatSpec with IEConfig with InsightEdge with Spark {

  it should "create dataset with insightedge format" taggedAs ScalaSpaceClass in { f =>
    writeDataSeqToDataGrid(1000)
    val spark = f.spark
    import spark.implicits._
    val ds = spark.read
        .grid
        .loadClass[Data]
      .as[Data]

    val filter = ds.filter(d => d.routing > 10).count()

    assert(ds.count() == 1000)
    assert(ds.rdd.partitions.length == NumberOfGridPartitions)
  }

  it should "create dataset with insightedge format [java]" taggedAs JavaSpaceClass in { f=>
    writeJDataSeqToDataGrid(1000)
    implicit val jDataEncoder = org.apache.spark.sql.Encoders.bean(classOf[JData])
    val spark = f.spark
    val ds = spark.read
        .grid
        .loadClass[JData]
        .as[JData]

    val filter = ds.filter(d => d.getRouting > -1).count()

    assert(ds.count() == 1000)
    assert(ds.rdd.partitions.length == NumberOfGridPartitions)
  }

  it should "fail to create dataset with insightedge format without class or collection provided" taggedAs ScalaSpaceClass in { f=>
    val thrown = intercept[IllegalArgumentException] {
      val spark = f.spark
      val df = spark.read
        .format("org.apache.spark.sql.insightedge")
        .load()
      //TODO
    }
    assert(thrown.getMessage == "'path', 'collection' or 'class' must be specified")
  }

  it should "create dataset with implicits" taggedAs ScalaSpaceClass in { f=>
    writeDataSeqToDataGrid(1000)
    val spark = f.spark
    import spark.implicits._
    val ds = spark.read.grid.loadClass[Data].as[Data]
    assert(ds.count() == 1000)
  }

  it should "create dataset with implicits [java]" taggedAs JavaSpaceClass in { f=>
    writeJDataSeqToDataGrid(1000)

    implicit val jDataEncoder = org.apache.spark.sql.Encoders.bean(classOf[JData])
    val spark = f.spark
    val ds = spark.read.grid.loadClass[JData].as[JData]
    assert(ds.count() == 1000)
  }

  it should "load dataset with 'collection' or 'path' option" taggedAs ScalaSpaceClass in { f=>
    writeDataSeqToDataGrid(1000)
    val spark = f.spark
    import spark.implicits._
    val collectionName = randomString()
    val ds = spark.read.grid.loadClass[Data].as[Data]
    ds.write.grid.save(collectionName)

    val fromGridDataSet = spark.read.format("org.apache.spark.sql.insightedge").option("collection", collectionName).load().as[Data]
    assert(fromGridDataSet.count() == 1000)
    //collection == path
    val fromGrid2DataSet = spark.read.format("org.apache.spark.sql.insightedge").load(collectionName).as[Data]
    assert(fromGrid2DataSet.count() == 1000)
  }

  it should "create dataframe from bucketed type with 'splitCount' option" taggedAs ScalaSpaceClass in { f=>
    writeBucketedDataSeqToDataGrid(1000)
    val spark = f.spark
    import spark.implicits._
    val ds = spark.read.grid
      .option("class", classOf[BucketedData].getName)
      .option("splitCount", "4")
      .load().as[BucketedData]
    //loadClass

    assert(ds.count() == 1000)
    assert(ds.rdd.partitions.length == 4 * NumberOfGridPartitions)
  }

  it should "create dataframe from bucketed type with 'splitCount' option [java]" taggedAs ScalaSpaceClass in { f=>
    writeJBucketedDataSeqToDataGrid(1000)
    implicit val jBucketDataEncoder = org.apache.spark.sql.Encoders.bean(classOf[JBucketedData])
    val spark = f.spark
    val ds = spark.read.grid
      .option("class", classOf[JBucketedData].getName)
      .option("splitCount", "4")
      .load()
      .as[JBucketedData]
    assert(ds.count() == 1000)
    assert(ds.rdd.partitions.length == 4 * NumberOfGridPartitions)
  }


  it should "load dataset from existing space documents with provided schema" in { f=>
    val collectionName = randomString()
    val spark = f.spark
    import spark.implicits._

    spaceProxy.getTypeManager.registerTypeDescriptor(
      new SpaceTypeDescriptorBuilder(collectionName)
        .idProperty("personId")
        .routingProperty("name")
        .create()
    )

    spaceProxy.writeMultiple(Array(
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

    val ds = spark.read.grid.schema(
      StructType(Seq(
        StructField("personId", StringType, nullable = false),
        StructField("name", StringType, nullable = true),
        StructField("surname", StringType, nullable = true),
        StructField("age", IntegerType, nullable = false),
        StructField("address", addressType.copy(), nullable = true, nestedClass[Address])
      ))
    ).load(collectionName).as[DummyPerson]
    //df.filter( r => r. )

    ds.printSchema()

    // check schema
    schemaAsserts(ds.schema)
    // check content
    dataSetAsserts(ds)

    // check if dataframe can be persisted
    val tableName = randomString()
    ds.write.grid.save(tableName)
    dataSetAsserts(spark.read.grid.load(tableName).as[DummyPerson])
  }
}