package com.gigaspaces.spark.rdd

import com.gigaspaces.spark.implicits.basic._
import com.gigaspaces.spark.fixture.{GigaSpaces, GsConfig, Spark}
import com.gigaspaces.spark.utils._
import org.scalatest.FlatSpec

class GigaSpacesSqlRDDSpec extends FlatSpec with GsConfig with GigaSpaces with Spark {

  it should "query data from Data Grid with a help of SQL" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)
    val sqlRdd = sc.gridSql[Data]("data IN (?,?,?)", Seq("data100", "data101", "data102"))

    val count = sqlRdd.count()
    assert(count == 3, "Wrong objects count")
  }

  it should "query data from Data Grid with a help of SQL [java]" taggedAs JavaSpaceClass in {
    writeJDataSeqToDataGrid(1000)
    val sqlRdd = sc.gridSql[JData]("data IN (?,?,?)", Seq("data100", "data101", "data102"))

    val count = sqlRdd.count()
    assert(count == 3, "Wrong objects count")
  }

}
