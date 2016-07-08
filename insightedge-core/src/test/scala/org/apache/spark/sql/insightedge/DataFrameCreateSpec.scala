package org.apache.spark.sql.insightedge

import com.gigaspaces.document.SpaceDocument
import com.gigaspaces.metadata.SpaceTypeDescriptorBuilder
import com.gigaspaces.spark.fixture.{GigaSpaces, GsConfig, Spark}
import com.gigaspaces.spark.implicits.all._
import com.gigaspaces.spark.rdd.{BucketedData, Data, JBucketedData, JData}
import com.gigaspaces.spark.utils.{JavaSpaceClass, ScalaSpaceClass}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.insightedge.model.Address
import org.apache.spark.sql.types.{IntegerType, StringType, StructType}
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
    assert(df.rdd.partitions.length == NumberOfGridPartitions)
  }

  it should "create dataframe with gigaspaces format [java]" taggedAs JavaSpaceClass in {
    writeJDataSeqToDataGrid(1000)

    val df = sql.read
      .format("org.apache.spark.sql.insightedge")
      .option("class", classOf[JData].getName)
      .load()
    assert(df.count() == 1000)
    assert(df.rdd.partitions.length == NumberOfGridPartitions)
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

    val dfAsserts = (dataFrame: DataFrame) => {
      assert(dataFrame.count() == 2)
      assert(dataFrame.filter(dataFrame("name") equalTo "John").count() == 1)
      assert(dataFrame.filter(dataFrame("age") < 30).count() == 1)
      assert(dataFrame.filter(dataFrame("address.state") equalTo "NY").count() == 1)
      assert(dataFrame.filter(dataFrame("jaddress.city") equalTo "Charlotte").count() == 1)
    }

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

    // check content
    dfAsserts(df)

    // check if dataframe can be persisted
    val tableName = randomString()
    df.write.grid.save(tableName)
    dfAsserts(sql.read.grid.load(tableName))
  }

}
