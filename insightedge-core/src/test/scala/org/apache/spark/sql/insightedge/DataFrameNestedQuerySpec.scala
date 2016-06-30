package org.apache.spark.sql.insightedge

import com.gigaspaces.spark.fixture.{GigaSpaces, GsConfig, Spark}
import com.gigaspaces.spark.implicits._
import com.gigaspaces.spark.utils.{JavaSpaceClass, ScalaSpaceClass}
import org.apache.spark.sql.insightedge.model.{Address, Person}
import org.scalatest.FlatSpec

class DataFrameNestedQuerySpec extends FlatSpec with GsConfig with GigaSpaces with Spark {

  it should "support nested properties" taggedAs ScalaSpaceClass in {
    sc.parallelize(Seq(
      new Person(id = null, name = "Paul", age = 30, address = new Address(city = "Columbus", state = "OH")),
      new Person(id = null, name = "Mike", age = 25, address = new Address(city = "Buffalo", state = "NY")),
      new Person(id = null, name = "John", age = 20, address = new Address(city = "Charlotte", state = "NC")),
      new Person(id = null, name = "Silvia", age = 27, address = new Address(city = "Charlotte", state = "NC"))
    )).saveToGrid()

    val df = sql.read.grid.loadClass[Person]
    df.printSchema()
    assert(df.count() == 4)
    assert(df.filter(df("address.city") equalTo "Buffalo").count() == 1)

    df.registerTempTable("people")

    val unwrapDf = sql.sql("select address.city as city, address.state as state from people")
    val countByCity = unwrapDf.groupBy("city").count().collect().map(row => row.getString(0) -> row.getLong(1)).toMap
    assert(countByCity("Charlotte") == 2)
    unwrapDf.printSchema()
  }

  it should "support nested properties [java]" taggedAs JavaSpaceClass in {
    sc.parallelize(Seq(
      new JPerson(null, "Paul", 30, new JAddress("Columbus", "OH")),
      new JPerson(null, "Mike", 25, new JAddress("Buffalo", "NY")),
      new JPerson(null, "John", 20, new JAddress("Charlotte", "NC")),
      new JPerson(null, "Silvia", 27, new JAddress("Charlotte", "NC"))
    )).saveToGrid()

    val df = sql.read.grid.loadClass[JPerson]
    df.printSchema()
    assert(df.count() == 4)
    assert(df.filter(df("address.city") equalTo "Buffalo").count() == 1)

    df.registerTempTable("people")

    val unwrapDf = sql.sql("select address.city as city, address.state as state from people")
    val countByCity = unwrapDf.groupBy("city").count().collect().map(row => row.getString(0) -> row.getLong(1)).toMap
    assert(countByCity("Charlotte") == 2)
    unwrapDf.printSchema()
  }

  ignore should "support nested properties in udt [java]" taggedAs JavaSpaceClass in {
    sc.parallelize(Seq(
      new JDefinedPerson(null, "Paul", 30, new JDefinedAddress("Columbus", "OH")),
      new JDefinedPerson(null, "Mike", 25, new JDefinedAddress("Buffalo", "NY")),
      new JDefinedPerson(null, "John", 20, new JDefinedAddress("Charlotte", "NC")),
      new JDefinedPerson(null, "Silvia", 27, new JDefinedAddress("Charlotte", "NC"))
    )).saveToGrid()

    val df = sql.read.grid.loadClass[JDefinedPerson]
    df.printSchema()
    assert(df.count() == 4)
    assert(df.filter(df("address.city") equalTo "Buffalo").count() == 1)

    df.registerTempTable("people")

    val unwrapDf = sql.sql("select address.city as city, address.state as state from people")
    val countByCity = unwrapDf.groupBy("city").count().collect().map(row => row.getString(0) -> row.getLong(1)).toMap
    assert(countByCity("Charlotte") == 2)
    unwrapDf.printSchema()
  }

  it should "support nested properties in products [java]" taggedAs JavaSpaceClass in {
    sc.parallelize(Seq(
      new JProductPerson(null, "Paul", 30, new JProductAddress("Columbus", "OH")),
      new JProductPerson(null, "Mike", 25, new JProductAddress("Buffalo", "NY")),
      new JProductPerson(null, "John", 20, new JProductAddress("Charlotte", "NC")),
      new JProductPerson(null, "Silvia", 27, new JProductAddress("Charlotte", "NC"))
    )).saveToGrid()

    val df = sql.read.grid.loadClass[JProductPerson]
    df.printSchema()
    assert(df.count() == 4)
    assert(df.filter(df("address.city") equalTo "Buffalo").count() == 1)

    df.registerTempTable("people")

    val unwrapDf = sql.sql("select address.city as city, address.state as state from people")
    val countByCity = unwrapDf.groupBy("city").count().collect().map(row => row.getString(0) -> row.getLong(1)).toMap
    assert(countByCity("Charlotte") == 2)
    unwrapDf.printSchema()
  }

}
