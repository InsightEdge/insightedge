package org.apache.spark.sql.insightedge

import com.gigaspaces.spark.fixture.{GigaSpaces, GsConfig, Spark}
import com.gigaspaces.spark.implicits.all._
import com.gigaspaces.spark.rdd.{Data, JData}
import com.gigaspaces.spark.utils.{JavaSpaceClass, ScalaSpaceClass}
import com.j_spaces.core.client.SQLQuery
import org.apache.spark.sql.AnalysisException
import org.apache.spark.sql.insightedge.model.NotGridModel
import org.scalatest.FlatSpec

class DataFrameQuerySpec extends FlatSpec with GsConfig with GigaSpaces with Spark {

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
    SQLQuery

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
