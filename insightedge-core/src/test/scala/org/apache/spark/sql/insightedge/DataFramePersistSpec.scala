package org.apache.spark.sql.insightedge

import com.gigaspaces.spark.fixture.{GigaSpaces, GsConfig, Spark}
import com.gigaspaces.spark.rdd.{Data, JData}
import com.gigaspaces.spark.utils.{JavaSpaceClass, ScalaSpaceClass}
import org.apache.spark.sql.SaveMode
import org.scalatest.FlatSpec

class DataFramePersistSpec extends FlatSpec with GsConfig with GigaSpaces with Spark {

  it should "persist with simplified syntax" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)
    val table = randomString()

    val df = sql.read.grid.loadClass[Data]
    df.filter(df("routing") > 500).write.grid.mode(SaveMode.Overwrite).save(table)

    val readDf = sql.read.grid.load(table)
    val count = readDf.select("routing").count()
    assert(count == 500)

    readDf.printSchema()
  }

  it should "persist with simplified syntax [java]" taggedAs JavaSpaceClass in {
    writeJDataSeqToDataGrid(1000)
    val table = randomString()

    val df = sql.read.grid.loadClass[JData]
    df.filter(df("routing") > 500).write.grid.mode(SaveMode.Overwrite).save(table)

    val readDf = sql.read.grid.load(table)
    val count = readDf.select("routing").count()
    assert(count == 500)

    readDf.printSchema()
  }

  it should "persist without implicits" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)
    val table = randomString()

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

  it should "fail to persist with ErrorIfExists mode" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)
    val table = randomString()

    val df = sql.read.grid.loadClass[Data]
    df.filter(df("routing") > 500).write.grid.mode(SaveMode.ErrorIfExists).save(table)

    val thrown = intercept[IllegalStateException] {
      df.filter(df("routing") < 500).write.grid.mode(SaveMode.ErrorIfExists).save(table)
    }
    println(thrown.getMessage)
  }

  it should "clear before write with Overwrite mode" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)
    val table = randomString()

    val df = sql.read.grid.loadClass[Data]
    df.filter(df("routing") > 500).write.grid.mode(SaveMode.Append).save(table)
    assert(sql.read.grid.load(table).count() == 500)

    df.filter(df("routing") <= 200).write.grid.mode(SaveMode.Overwrite).save(table)
    assert(sql.read.grid.load(table).count() == 200)
  }

  it should "not write with Ignore mode" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)
    val table = randomString()

    val df = sql.read.grid.loadClass[Data]
    df.filter(df("routing") > 500).write.grid.mode(SaveMode.Append).save(table)
    assert(sql.read.grid.load(table).count() == 500)

    df.filter(df("routing") <= 200).write.grid.mode(SaveMode.Ignore).save(table)
    assert(sql.read.grid.load(table).count() == 500)
  }

  it should "override document schema" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)
    val table = randomString()

    val df = sql.read.grid.loadClass[Data]
    // persist with modified schema
    df.select("id", "data").write.grid.save(table)
    // persist with original schema
    df.write.grid.mode(SaveMode.Overwrite).save(table)
    // persist with modified schema again
    df.select("id").write.grid.mode(SaveMode.Overwrite).save(table)
  }

}
