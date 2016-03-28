package com.gigaspaces.spark.rdd

import com.gigaspaces.spark.utils.{GigaSpaces, Spark, GsConfig}
import org.scalatest.{Matchers, FunSpec}
import com.gigaspaces.spark.implicits._

class GigaSpacesSqlRDDSpec extends FunSpec with GsConfig with GigaSpaces with Spark {

  it("should successfully query data from Data Grid with a help of SQL") {
    writeDataSeqToDataGrid(1000)
    val sqlRdd = sc.gridSql[Data]("data IN (?,?,?)", Seq("data100", "data101", "data102"))

    val count = sqlRdd.count()
    assert(count == 3, "Wrong objects count")
  }

}
