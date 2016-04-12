package org.apache.spark.sql.insightedge

import com.gigaspaces.spark.rdd.Data
import com.gigaspaces.spark.utils.{GigaSpaces, GsConfig, Spark}
import org.apache.commons.lang3.RandomStringUtils
import org.apache.spark.sql.{AnalysisException, SaveMode}
import org.scalatest.FunSpec
import com.gigaspaces.spark.implicits._

class GigaSpacesDataFrameSpec extends FunSpec with GsConfig with GigaSpaces with Spark {
  it("should create dataframe with gigaspaces format") {
    writeDataSeqToDataGrid(1000)

    val df = sql.read
      .format("org.apache.spark.sql.insightedge")
      .option("class", classOf[Data].getName)
      .load()
    assert(df.count() == 1000)
  }

  it("should fail to create dataframe with gigaspaces format without class or collection provided") {
    val thrown = intercept[IllegalArgumentException] {
      val df = sql.read
        .format("org.apache.spark.sql.insightedge")
        .load()
    }
    assert(thrown.getMessage == "'path', 'collection' or 'class' must be specified")
  }

  it("should create dataframe with implicits") {
    writeDataSeqToDataGrid(1000)

    val df = sql.read.grid.loadClass[Data]
    assert(df.count() == 1000)
  }

  it("should work with SQL syntax") {
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

  it("should read empty classes") {
    sql.sql(
      s"""
         |create temporary table dataTable
         |using org.apache.spark.sql.insightedge
         |options (class "${classOf[Data].getName}")
      """.stripMargin)

    assert(sql.sql("select * from dataTable where data is null").collect().length == 0)
  }

  it("should select one field") {
    writeDataSeqToDataGrid(1000)

    val df = sql.read.grid.loadClass[Data]
    val increasedRouting = df.select(df("routing") + 10).first().getAs[Long](0)
    assert(increasedRouting >= 10)
  }

  it("should filter by one field") {
    writeDataSeqToDataGrid(1000)

    val df = sql.read.grid.loadClass[Data]
    val count = df.filter(df("routing") > 500).count()
    assert(count == 500)
  }

  it("should group by field") {
    writeDataSeqToDataGrid(1000)

    val df = sql.read.grid.loadClass[Data]
    val count = df.groupBy("routing").count().count()
    assert(count == 1000)
  }

  it("should fail to resolve column that's not in class") {
    writeDataSeqToDataGrid(1000)

    val df = sql.read.grid.loadClass[Data]
    intercept[AnalysisException] {
      val count = df.select(df("abc")).count()
    }
  }

  it("should write back half of data") {
    writeDataSeqToDataGrid(1000)
    val table = RandomStringUtils.random(10, "abcdefghijklmnopqrst")

    val df = sql.read.grid.loadClass[Data]
    df.filter(df("routing") > 500).write.grid.mode(SaveMode.Append).save(table)

    val readDf = sql.read.grid.load(table)
    val count = readDf.select("routing").count()
    assert(count == 500)

    readDf.printSchema()
  }

  it("should fail to write with ErrorIfExists mode") {
    writeDataSeqToDataGrid(1000)
    val table = RandomStringUtils.random(10, "abcdefghijklmnopqrst")

    val df = sql.read.grid.loadClass[Data]
    df.filter(df("routing") > 500).write.grid.mode(SaveMode.ErrorIfExists).save(table)

    val thrown = intercept[IllegalStateException] {
      df.filter(df("routing") < 500).write.grid.mode(SaveMode.ErrorIfExists).save(table)
    }
    println(thrown.getMessage)
  }

  it("should clear before write with Overwrite mode") {
    writeDataSeqToDataGrid(1000)
    val table = RandomStringUtils.random(10, "abcdefghijklmnopqrst")

    val df = sql.read.grid.loadClass[Data]
    df.filter(df("routing") > 500).write.grid.mode(SaveMode.Append).save(table)
    assert(sql.read.grid.load(table).count() == 500)

    df.filter(df("routing") <= 200).write.grid.mode(SaveMode.Overwrite).save(table)
    assert(sql.read.grid.load(table).count() == 200)
  }

  it("should not write with Ignore mode") {
    writeDataSeqToDataGrid(1000)
    val table = RandomStringUtils.random(10, "abcdefghijklmnopqrst")

    val df = sql.read.grid.loadClass[Data]
    df.filter(df("routing") > 500).write.grid.mode(SaveMode.Append).save(table)
    assert(sql.read.grid.load(table).count() == 500)

    df.filter(df("routing") <= 200).write.grid.mode(SaveMode.Ignore).save(table)
    assert(sql.read.grid.load(table).count() == 500)
  }

  it("should support nested properties") {
    sc.parallelize(Seq(
      new Person(id = null, name = "Paul", age = 30, address = new Address(city = "Columbus", state = "OH")),
      new Person(id = null, name = "Mike", age = 25, address = new Address(city = "Buffalo", state = "NY")),
      new Person(id = null, name = "John", age = 20, address = new Address(city = "Charlotte", state = "NC")),
      new Person(id = null, name = "Silvia", age = 27, address = new Address(city = "Charlotte", state = "NC"))
    )).saveToGrid()

    val df = sql.read.grid.loadClass[Person]
    assert(df.count() == 4)
    assert(df.filter(df("address.city") equalTo "Buffalo").count() == 1)

    df.registerTempTable("people")
    val sqlDf = sql.sql("select address.city as city, address.state as state from people")
    val countByCity = sqlDf.groupBy("city").count().collect().map(row => row.getString(0) -> row.getLong(1)).toMap
    assert(countByCity("Charlotte") == 2)
  }

}
