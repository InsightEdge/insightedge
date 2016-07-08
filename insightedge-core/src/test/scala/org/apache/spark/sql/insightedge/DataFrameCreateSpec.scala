package org.apache.spark.sql.insightedge

import com.gigaspaces.document.SpaceDocument
import com.gigaspaces.metadata.SpaceTypeDescriptorBuilder
import com.gigaspaces.spark.fixture.{GigaSpaces, GsConfig, Spark}
import com.gigaspaces.spark.implicits.all._
import com.gigaspaces.spark.rdd.{Data, JData}
import com.gigaspaces.spark.utils.{JavaSpaceClass, ScalaSpaceClass}
import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.functions._
import org.apache.spark.sql.insightedge.model.Address
import org.apache.spark.sql.types.{StructType, IntegerType, StringType}
import org.scalatest.FlatSpec

import scala.collection.JavaConversions._

class DataFrameCreateSpec extends FlatSpec with GsConfig with GigaSpaces with Spark {

  it should "create dataframe with gigaspaces format" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)

    val df = sql.read
      .format("org.apache.spark.sql.insightedge")
      .option("class", classOf[Data].getName)
      .load()
    assert(df.count() == 1000)
  }

  it should "create dataframe with gigaspaces format [java]" taggedAs JavaSpaceClass in {
    writeJDataSeqToDataGrid(1000)

    val df = sql.read
      .format("org.apache.spark.sql.insightedge")
      .option("class", classOf[JData].getName)
      .load()
    assert(df.count() == 1000)
  }

  it should "fail to create dataframe with gigaspaces format without class or collection provided" taggedAs ScalaSpaceClass in {
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

  it should "load dataframe from existing space documents" in {
    val collectionName = randomString()

    spaceProxy.getTypeManager.registerTypeDescriptor(
      new SpaceTypeDescriptorBuilder(collectionName)
        .addFixedProperty("name", classOf[String])
        .addFixedProperty("surname", classOf[String])
        .addFixedProperty("age", classOf[Integer])
        .addFixedProperty("address", classOf[Address])
        .addFixedProperty("jaddress", classOf[JAddress])
        .create()
    )

    spaceProxy.writeMultiple(Array(
      new SpaceDocument(collectionName, Map(
        "name" -> "John", "surname" -> "Wind", "age" -> Integer.valueOf(32),
        "address" -> Address("New York", "NY"), "jaddress" -> new JAddress("New York", "NY")
      )),
      new SpaceDocument(collectionName, Map(
        "name" -> "Mike", "surname" -> "Green", "age" -> Integer.valueOf(20),
        "address" -> Address("Charlotte", "NC"), "jaddress" -> new JAddress("Charlotte", "NC")
      ))
    ))

    val df = sql.read.grid.load(collectionName)
    df.printSchema()

    // check schema
    val s = df.schema

    assert(s.fieldNames.contains("name"))
    assert(s.fieldNames.contains("surname"))
    assert(s.fieldNames.contains("age"))
    assert(s.fieldNames.contains("address"))
    assert(s.fieldNames.contains("jaddress"))

    assert(s.get(s.getFieldIndex("name").get).dataType == StringType)
    assert(s.get(s.getFieldIndex("surname").get).dataType == StringType)
    assert(s.get(s.getFieldIndex("age").get).dataType == IntegerType)
    assert(s.get(s.getFieldIndex("address").get).dataType.isInstanceOf[StructType])
    assert(s.get(s.getFieldIndex("jaddress").get).dataType.isInstanceOf[StructType])

    assert(df.filter(df("name") equalTo "John").count() == 1)
    assert(df.filter(df("age") < 30).count() == 1)
    assert(df.filter(df("address.state") equalTo "NY").count() == 1)
    assert(df.filter(df("jaddress.city") equalTo "Charlotte").count() == 1)

    // check if dataframe can be persisted
    val tableName = randomString()
    df.write.grid.save(tableName)
    assert(sql.read.grid.load(tableName).count() == 2)

    // check if we can write more objects to the same collection
    val savingDf = df
      .withColumn("name", udf { name: String => name + "!" }.apply(df("name")))
      .withColumn("surname", udf { name: String => name + "?" }.apply(df("surname")))
      .withColumn("age", udf { age: Integer => age + 100 }.apply(df("age")))
    savingDf.printSchema()
    savingDf.write.grid.mode(SaveMode.Append).save(collectionName)

    val newDf = sql.read.grid.load(collectionName)
    assert(newDf.count() == 4)
    assert(newDf.filter(newDf("name") endsWith "!").count() == 2)
  }

}
