package org.apache.spark.sql.insightedge.dataframe

import org.insightedge.spark.fixture.InsightEdge
import org.insightedge.spark.rdd.Data
import org.insightedge.spark.utils.ScalaSpaceClass
import org.scalatest.fixture
import org.insightedge.spark.implicits.all._

import scala.reflect.classTag

class GetDataFrameWithStringSpec extends fixture.FlatSpec with InsightEdge {

  it should "Read data frame as String with a valid struct Type " taggedAs ScalaSpaceClass in { ie =>

    writeDataSeqToDataGrid(1000)
    val spark= ie.spark
    val df = spark.read.grid("org.insightedge.spark.rdd.Data")
    val fields = classTag[Data].runtimeClass.getDeclaredFields

    // Make sure all the original fields exist.
    assert(fields.size == df.schema.fields.length, "Dataframe should have exactly the number of fields as the class it represents")
    for (field <- fields) {
      assert(df.schema.fieldNames.contains(field.getName))
    }
  }

  it should "Read data frame as Class with a valid struct Type " taggedAs ScalaSpaceClass in { ie =>

    writeDataSeqToDataGrid(1000)
    val spark= ie.spark
    val df = spark.read.grid[Data]
    val fields = classTag[Data].runtimeClass.getDeclaredFields

    // Make sure all the original fields exist.
    assert(fields.size == df.schema.fields.length, "Dataframe should have exactly the number of fields as the class it represents")
    for (field <- fields) {
      assert(df.schema.fieldNames.contains(field.getName))
    }
  }

}
