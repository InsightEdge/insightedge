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

import com.gigaspaces.document.{DocumentProperties, SpaceDocument}
import com.j_spaces.core.client.SQLQuery
import org.apache.spark.sql.insightedge.model.{Address, Person}
import org.apache.spark.sql.insightedge.{JAddress, JPerson}
import org.insightedge.spark.fixture.InsightEdge
import org.insightedge.spark.implicits.all._
import org.insightedge.spark.utils.{JavaSpaceClass, ScalaSpaceClass}
import org.scalatest.fixture

class DataFrameNestedQuerySpec extends fixture.FlatSpec with InsightEdge {

  it should "support nested properties" taggedAs ScalaSpaceClass in { ie=>

    val spark = ie.spark

    ie.sc.parallelize(Seq(
      Person(id = null, name = "Paul", age = 30, address = Address(city = "Columbus", state = "OH")),
      Person(id = null, name = "Mike", age = 25, address = Address(city = "Buffalo", state = "NY")),
      Person(id = null, name = "John", age = 20, address = Address(city = "Charlotte", state = "NC")),
      Person(id = null, name = "Silvia", age = 27, address = Address(city = "Charlotte", state = "NC"))
    )).saveToGrid()

    val df = spark.read.grid[Person]
    df.printSchema()
    df.show()
    assert(df.count() == 4)
    assert(df.filter(df("address.city") equalTo "Buffalo").count() == 1)

    df.createOrReplaceTempView("people")

    val unwrapDf = spark.sql("select address.city as city, address.state as state from people")
    val countByCity = unwrapDf.groupBy("city").count().collect().map(row => row.getString(0) -> row.getLong(1)).toMap
    assert(countByCity("Charlotte") == 2)
    unwrapDf.printSchema()
  }

  it should "support nested properties [java]" taggedAs JavaSpaceClass in { f=>
    parallelizeJavaSeq(f.sc, () => Seq(
      new JPerson(null, "Paul", 30, new JAddress("Columbus", "OH")),
      new JPerson(null, "Mike", 25, new JAddress("Buffalo", "NY")),
      new JPerson(null, "John", 20, new JAddress("Charlotte", "NC")),
      new JPerson(null, "Silvia", 27, new JAddress("Charlotte", "NC"))
    )).saveToGrid()
    val spark = f.spark
    val df = spark.read.grid[JPerson]
    df.printSchema()
    assert(df.count() == 4)
    assert(df.filter(df("address.city") equalTo "Buffalo").count() == 1)

    df.createOrReplaceTempView("people")

    val unwrapDf = spark.sql("select address.city as city, address.state as state from people")
    val countByCity = unwrapDf.groupBy("city").count().collect().map(row => row.getString(0) -> row.getLong(1)).toMap
    assert(countByCity("Charlotte") == 2)
    unwrapDf.printSchema()
  }

  it should "support nested properties after saving" taggedAs ScalaSpaceClass in { ie =>

    ie.sc.parallelize(Seq(
      Person(id = null, name = "Paul", age = 30, address = Address(city = "Columbus", state = "OH")),
      Person(id = null, name = "Mike", age = 25, address = Address(city = "Buffalo", state = "NY")),
      Person(id = null, name = "John", age = 20, address = Address(city = "Charlotte", state = "NC")),
      Person(id = null, name = "Silvia", age = 27, address = Address(city = "Charlotte", state = "NC"))
    )).saveToGrid()

    val collectionName = randomString()
    val spark = ie.spark
    val dataframeClass = spark.read.grid[Person]
    dataframeClass.write.grid(collectionName)

// TODO fix writing df with nested properties to space when the df was read as a collection like the example below.
//    val dataFrameDocument = spark.read.grid("org.apache.spark.sql.insightedge.model.Person")
//    dataFrameDocument.write.grid(collectionName)

    val person = ie.spaceProxy.read(new SQLQuery[SpaceDocument](collectionName, ""))
    assert(person.getProperty[Any]("address").isInstanceOf[DocumentProperties])
    assert(person.getProperty[Any]("age").isInstanceOf[Integer])
    assert(person.getProperty[Any]("name").isInstanceOf[String])

    val dataframeCollection = spark.read.grid(collectionName)
    dataframeCollection.printSchema()
    dataframeCollection.show()
    assert(dataframeCollection.count() == 4)
    assert(dataframeCollection.filter(dataframeCollection("address.state") equalTo "NC").count() == 2)
    assert(dataframeCollection.filter(dataframeCollection("address.city") equalTo "Nowhere").count() == 0)
  }

  it should "support nested properties after saving [java]" taggedAs ScalaSpaceClass in { ie =>

    parallelizeJavaSeq(ie.sc, () => Seq(
      new JPerson(null, "Paul", 30, new JAddress("Columbus", "OH")),
      new JPerson(null, "Mike", 25, new JAddress("Buffalo", "NY")),
      new JPerson(null, "John", 20, new JAddress("Charlotte", "NC")),
      new JPerson(null, "Silvia", 27, new JAddress("Charlotte", "NC"))
    )).saveToGrid()

    val collectionName = randomString()
    val spark = ie.spark
    spark.read.grid[JPerson].write.grid(collectionName)

    val person = ie.spaceProxy.read(new SQLQuery[SpaceDocument](collectionName, ""))
    assert(person.getProperty[Any]("address").isInstanceOf[DocumentProperties])
    assert(person.getProperty[Any]("age").isInstanceOf[Integer])
    assert(person.getProperty[Any]("name").isInstanceOf[String])

    val df = spark.read.grid(collectionName)
    assert(df.count() == 4)
    assert(df.filter(df("address.state") equalTo "NC").count() == 2)
    assert(df.filter(df("address.city") equalTo "Nowhere").count() == 0)
  }

}
