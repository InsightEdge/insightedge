package org.apache.spark.sql.insightedge

import com.gigaspaces.spark.rdd.Data
import com.gigaspaces.spark.utils.{GigaSpaces, GsConfig, Spark}
import org.apache.spark.sql.{SaveMode, AnalysisException}
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

  it("should work with sql api") {
    writeDataSeqToDataGrid(1000)

    sql.sql(
      """create temporary table mytable
        |using org.apache.spark.sql.insightedge
        |options (
        | class "com.gigaspaces.spark.rdd.Data"
        |)
      """.stripMargin)

    val count = sql.sql("select count(*) from mytable where routing > 500").first().getAs[Long](0)
    assert(count == 500)
  }

  it("should write back half of data") {
    writeDataSeqToDataGrid(1000)

    val df = sql.read.grid.loadClass[Data]
    df.filter(df("routing") > 500).write.grid.mode(SaveMode.Append).save("half")

    val count = sql.read.grid.load("half").select("routing").count()
    assert(count == 500)
  }

}
