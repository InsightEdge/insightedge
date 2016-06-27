package com.gigaspaces.spark.python

import com.gigaspaces.spark.fixture.{GigaSpaces, GsConfig, Spark}
import com.gigaspaces.spark.rdd.{Data, GigaSpacesRDD}
import org.scalatest.FlatSpec

/**
  * @author Oleksiy_Dyagilev
  */
class PythonGigaSpacesSparkContextSpec extends FlatSpec with GsConfig with GigaSpaces with Spark {

  it should "load rdd" in {
    writeDataSeqToDataGrid(1000)
    val pySc = new PythonHelper().createPythonGigaSpacesSparkContext(sc)
    val rdd = pySc.gridRdd("com.gigaspaces.spark.rdd.Data")

    val typedRdd = rdd.asInstanceOf[GigaSpacesRDD[Data]]
    val sum = typedRdd.map(data => data.routing).sum()
    val expectedSum = (1 to 1000).sum
    assert(sum == expectedSum, "")
  }

}
