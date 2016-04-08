package org.apache.spark.sql.insightedge

import com.gigaspaces.spark.rdd.Data
import com.gigaspaces.spark.utils.{Spark, GigaSpaces, GsConfig}
import org.apache.spark.sql.{AnalysisException, SQLContext}
import org.scalatest.FunSpec
import org.apache.spark.sql.insightedge._

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
    assert(thrown.getMessage == "'class' or 'collection' must be specified")
  }

  it("should create dataframe with implicits") {
    writeDataSeqToDataGrid(1000)

    val df = sql.read.grid[Data].load()
    assert(df.count() == 1000)
  }

  it("should select one field") {
    writeDataSeqToDataGrid(1000)

    val df = sql.read.grid[Data].load()
    val increasedRouting = df.select(df("routing") + 10).first().getAs[Long](0)
    assert(increasedRouting >= 10)
  }

  it("should filter by one field") {
    writeDataSeqToDataGrid(1000)

    val df = sql.read.grid[Data].load()
    val count = df.filter(df("routing") > 500).count()
    assert(count == 500)
  }

  it("should group by field") {
    writeDataSeqToDataGrid(1000)

    val df = sql.read.grid[Data].load()
    val count = df.groupBy("routing").count().count()
    assert(count == 1000)
  }

  it("should fail to resolve column that's not in class") {
    writeDataSeqToDataGrid(1000)

    val df = sql.read.grid[Data].load()
    intercept[AnalysisException] {
      val count = df.select(df("abc")).count()
    }
  }

}
