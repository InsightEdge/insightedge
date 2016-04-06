package org.apache.spark.sql.insightedge

import com.gigaspaces.spark.rdd.Data
import com.gigaspaces.spark.utils.{Spark, GigaSpaces, GsConfig}
import org.apache.spark.sql.SQLContext
import org.scalatest.FunSpec

class GigaSpacesDataFrameSpec extends FunSpec with GsConfig with GigaSpaces with Spark {

  it("should create dataframe with gigaspaces format") {
    writeDataSeqToDataGrid(1000)

    val sql = new SQLContext(sc)
    val df = sql.read
      .format("org.apache.spark.sql.insightedge")
      .option("class", classOf[Data].getName)
      .load()
    assert(df.count() == 1000)
  }

  it("should create dataframe with implicits") {
    writeDataSeqToDataGrid(1000)

    import org.apache.spark.sql.insightedge._

    val sql = new SQLContext(sc)
    val df = sql.read
      .grid[Data]
      .load()
    assert(df.count() == 1000)
  }

}
