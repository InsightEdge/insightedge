package com.gigaspaces.spark.rdd

import com.gigaspaces.spark.fixture.{GigaSpaces, GsConfig, Spark}
import com.gigaspaces.spark.implicits._
import com.gigaspaces.spark.utils._
import org.scalatest.{FlatSpec, FunSpec}

class GigaSpacesDataFrameRDDSpec extends FlatSpec with GsConfig with GigaSpaces with Spark {

  it should "filter data via DataFrame API" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)
    val df = sc.gridDataFrame[Data]()
    val count = df.filter(df("routing") > 500).count()
    assert(count == 500, "Wrong filtered objects count")
  }

  it should "filter data via DataFrame API [java]" taggedAs JavaSpaceClass in {
    writeJDataSeqToDataGrid(1000)
    val df = sc.gridDataFrame[JData]()
    val count = df.filter(df("routing") > 500).count()
    assert(count == 500, "Wrong filtered objects count")
  }

  it should "aggregate data via DataFrame API" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid((1 to 1000).map(i => new Data(i % 10, "data" + i)))
    val df = sc.gridDataFrame[Data]()
    val differentRoutingValues = df.groupBy(df("routing")).count().count
    assert(differentRoutingValues == 10, "Wrong aggregation count")
  }

  it should "aggregate data via DataFrame API [java]" taggedAs JavaSpaceClass in {
    writeDataSeqToDataGrid((1 to 1000).map(i => new JData((i % 10).toLong, "data" + i)))
    val df = sc.gridDataFrame[JData]()
    val differentRoutingValues = df.groupBy(df("routing")).count().count
    assert(differentRoutingValues == 10, "Wrong aggregation count")
  }

  it should "be possible to query persisted data frame with SQL" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)

    val df = sc.gridDataFrame[Data]()
    df.persist()

    df.registerTempTable("temp_table")

    val r1 = sc.gridSqlContext.sql("SELECT count(*) FROM temp_table WHERE routing > 500")
    assert(r1.first().getAs[Long](0) == 500)

    df.registerTempTable("temp_table2")
    val r2 = sc.gridSqlContext.sql("SELECT count(*) FROM temp_table t1 JOIN temp_table2 t2 ON t1.id = t2.id")
    assert(r2.first().getAs[Long](0) == 1000)
  }

  it should "be possible to query persisted data frame with SQL [java]" taggedAs JavaSpaceClass in {
    writeJDataSeqToDataGrid(1000)

    val df = sc.gridDataFrame[JData]()
    df.persist()

    df.registerTempTable("temp_table")

    val r1 = sc.gridSqlContext.sql("SELECT count(*) FROM temp_table WHERE routing > 500")
    assert(r1.first().getAs[Long](0) == 500)

    df.registerTempTable("temp_table2")
    val r2 = sc.gridSqlContext.sql("SELECT count(*) FROM temp_table t1 JOIN temp_table2 t2 ON t1.id = t2.id")
    assert(r2.first().getAs[Long](0) == 1000)
  }

}
