package org.apache.spark.sql.insightedge

import com.gigaspaces.spark.implicits._
import com.gigaspaces.spark.rdd.{Data, JData}
import com.gigaspaces.spark.utils._
import org.apache.commons.lang3.RandomStringUtils
import org.apache.spark.sql.{AnalysisException, SaveMode}
import org.scalatest.FlatSpec

class GigaSpacesDataFrameSpec extends FlatSpec with GsConfig with GigaSpaces with Spark {

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

  it should "read empty classes" taggedAs ScalaSpaceClass in {
    sql.sql(
      s"""
         |create temporary table dataTable
         |using org.apache.spark.sql.insightedge
         |options (class "${classOf[Data].getName}")
      """.stripMargin)

    assert(sql.sql("select * from dataTable where data is null").collect().length == 0)
  }

  it should "select one field" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)

    val df = sql.read.grid.loadClass[Data]
    val increasedRouting = df.select(df("routing") + 10).first().getAs[Long](0)
    assert(increasedRouting >= 10)
  }

  it should "select one field [java]" taggedAs JavaSpaceClass in {
    writeJDataSeqToDataGrid(1000)

    val df = sql.read.grid.loadClass[JData]
    df.printSchema()

    val increasedRouting = df.select(df("routing") + 10).first().getAs[Long](0)
    assert(increasedRouting >= 10)
  }

  it should "filter by one field" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)

    val df = sql.read.grid.loadClass[Data]
    val count = df.filter(df("routing") > 500).count()
    assert(count == 500)
  }

  it should "group by field" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)

    val df = sql.read.grid.loadClass[Data]
    val count = df.groupBy("routing").count().count()
    assert(count == 1000)
  }

  it should "group by field [java]" taggedAs JavaSpaceClass in {
    writeJDataSeqToDataGrid(1000)

    val df = sql.read.grid.loadClass[JData]
    val count = df.groupBy("routing").count().count()
    assert(count == 1000)
  }

  it should "fail to resolve column that's not in class" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)

    val df = sql.read.grid.loadClass[Data]
    intercept[AnalysisException] {
      val count = df.select(df("abc")).count()
    }
  }

  it should "persist with simplified syntax" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)
    val table = RandomStringUtils.random(10, "abcdefghijklmnopqrst")

    val df = sql.read.grid.loadClass[Data]
    df.filter(df("routing") > 500).write.grid.mode(SaveMode.Overwrite).save(table)

    val readDf = sql.read.grid.load(table)
    val count = readDf.select("routing").count()
    assert(count == 500)

    readDf.printSchema()
  }

  it should "persist with simplified syntax [java]" taggedAs JavaSpaceClass in {
    writeJDataSeqToDataGrid(1000)
    val table = RandomStringUtils.random(10, "abcdefghijklmnopqrst")

    val df = sql.read.grid.loadClass[JData]
    df.filter(df("routing") > 500).write.grid.mode(SaveMode.Overwrite).save(table)

    val readDf = sql.read.grid.load(table)
    val count = readDf.select("routing").count()
    assert(count == 500)

    readDf.printSchema()
  }

  it should "persist without imports" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)
    val table = RandomStringUtils.random(10, "abcdefghijklmnopqrst")

    val df = sql.read
      .format("org.apache.spark.sql.insightedge")
      .option("class", classOf[Data].getName)
      .load()
    df.filter(df("routing") > 500)
      .write
      .mode(SaveMode.Overwrite)
      .format("org.apache.spark.sql.insightedge")
      .save(table)

    val readDf = sql.read.grid.load(table)
    val count = readDf.select("routing").count()
    assert(count == 500)

    readDf.printSchema()
  }



  it should "fail to write with ErrorIfExists mode" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)
    val table = RandomStringUtils.random(10, "abcdefghijklmnopqrst")

    val df = sql.read.grid.loadClass[Data]
    df.filter(df("routing") > 500).write.grid.mode(SaveMode.ErrorIfExists).save(table)

    val thrown = intercept[IllegalStateException] {
      df.filter(df("routing") < 500).write.grid.mode(SaveMode.ErrorIfExists).save(table)
    }
    println(thrown.getMessage)
  }

  it should "clear before write with Overwrite mode" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)
    val table = RandomStringUtils.random(10, "abcdefghijklmnopqrst")

    val df = sql.read.grid.loadClass[Data]
    df.filter(df("routing") > 500).write.grid.mode(SaveMode.Append).save(table)
    assert(sql.read.grid.load(table).count() == 500)

    df.filter(df("routing") <= 200).write.grid.mode(SaveMode.Overwrite).save(table)
    assert(sql.read.grid.load(table).count() == 200)
  }

  it should "not write with Ignore mode" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)
    val table = RandomStringUtils.random(10, "abcdefghijklmnopqrst")

    val df = sql.read.grid.loadClass[Data]
    df.filter(df("routing") > 500).write.grid.mode(SaveMode.Append).save(table)
    assert(sql.read.grid.load(table).count() == 500)

    df.filter(df("routing") <= 200).write.grid.mode(SaveMode.Ignore).save(table)
    assert(sql.read.grid.load(table).count() == 500)
  }

  it should "support nested properties" taggedAs ScalaSpaceClass in {
    sc.parallelize(Seq(
      new Person(id = null, name = "Paul", age = 30, address = new Address(city = "Columbus", state = "OH")),
      new Person(id = null, name = "Mike", age = 25, address = new Address(city = "Buffalo", state = "NY")),
      new Person(id = null, name = "John", age = 20, address = new Address(city = "Charlotte", state = "NC")),
      new Person(id = null, name = "Silvia", age = 27, address = new Address(city = "Charlotte", state = "NC"))
    )).saveToGrid()

    val df = sql.read.grid.loadClass[Person]
    assert(df.count() == 4)
    assert(df.filter(df("address.city") equalTo "Buffalo").count() == 1)
    df.printSchema()

    df.registerTempTable("people")

    val unwrapDf = sql.sql("select address.city as city, address.state as state from people")
    val countByCity = unwrapDf.groupBy("city").count().collect().map(row => row.getString(0) -> row.getLong(1)).toMap
    assert(countByCity("Charlotte") == 2)
    unwrapDf.printSchema()
  }

  ignore should "support nested properties [java]" taggedAs JavaSpaceClass in {
    sc.parallelize(Seq(
      new JPerson(null, "Paul", 30, new JAddress("Columbus", "OH")),
      new JPerson(null, "Mike", 25, new JAddress("Buffalo", "NY")),
      new JPerson(null, "John", 20, new JAddress("Charlotte", "NC")),
      new JPerson(null, "Silvia", 27, new JAddress("Charlotte", "NC"))
    )).saveToGrid()

    val df = sql.read.grid.loadClass[JPerson]
    assert(df.count() == 4)
    assert(df.filter(df("address.city") equalTo "Buffalo").count() == 1)
    df.printSchema()

    df.registerTempTable("people")

    val unwrapDf = sql.sql("select address.city as city, address.state as state from people")
    val countByCity = unwrapDf.groupBy("city").count().collect().map(row => row.getString(0) -> row.getLong(1)).toMap
    assert(countByCity("Charlotte") == 2)
    unwrapDf.printSchema()
  }

  it should "override document schema" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)
    val table = RandomStringUtils.random(10, "abcdefghijklmnopqrst")

    val df = sql.read.grid.loadClass[Data]
    // persist with modified schema
    df.select("id", "data").write.grid.save(table)
    // persist with original schema
    df.write.grid.mode(SaveMode.Overwrite).save(table)
    // persist with modified schema again
    df.select("id").write.grid.mode(SaveMode.Overwrite).save(table)
  }

  it should "fail to load class" taggedAs ScalaSpaceClass in {
    val thrown = intercept[ClassNotFoundException] {
      sql.read.grid.option("class", "non.existing.Class").load()
    }
    assert(thrown.getMessage equals "non.existing.Class")
  }

  it should "fail to work with class that is not GridModel" taggedAs ScalaSpaceClass in {
    val thrown = intercept[IllegalArgumentException] {
      sql.read.grid.option("class", classOf[NotGridModel].getName).load()
    }
    assert(thrown.getMessage equals "'class' must extend com.gigaspaces.spark.model.GridModel")
  }

}

case class NotGridModel()