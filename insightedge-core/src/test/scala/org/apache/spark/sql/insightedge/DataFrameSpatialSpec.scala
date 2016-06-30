package org.apache.spark.sql.insightedge

import com.gigaspaces.spark.fixture.{GigaSpaces, GsConfig, Spark}
import com.gigaspaces.spark.utils._
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.insightedge.model.SpatialData
import org.openspaces.spatial.ShapeFactory.{circle, point, rectangle}
import org.scalatest.FlatSpec

class DataFrameSpatialSpec extends FlatSpec with GsConfig with GigaSpaces with Spark {

  it should "find with spatial operations at xap and spark" taggedAs ScalaSpaceClass in {
    val searchedCircle = circle(point(0, 0), 1.0)
    val searchedRect = rectangle(0, 2, 0, 2)
    spaceProxy.write(randomBucket(SpatialData(id = null, routing = 1, searchedCircle, searchedRect, null)))

    def asserts(df: DataFrame): Unit = {
      assert(df.count() == 1)

      assert(df.filter(df("circle") geoIntersects circle(point(1.0, 0.0), 1.0)).count() == 1)
      assert(df.filter(df("circle") geoIntersects circle(point(3.0, 0.0), 1.0)).count() == 0)
      assert(df.filter(df("circle") geoWithin circle(point(1.0, 0.0), 2.0)).count() == 1)
      assert(df.filter(df("circle") geoWithin circle(point(1.0, 0.0), 1.0)).count() == 0)
      assert(df.filter(df("circle") geoContains circle(point(0.0, 0.0), 0.5)).count() == 1)
      assert(df.filter(df("circle") geoContains circle(point(1.0, 0.0), 1.0)).count() == 0)

      assert(df.filter(df("rect") geoIntersects rectangle(1, 3, 1, 3)).count() == 1)
      assert(df.filter(df("rect") geoIntersects rectangle(3, 5, 0, 2)).count() == 0)
      assert(df.filter(df("rect") geoWithin rectangle(-1, 3, -1, 3)).count() == 1)
      assert(df.filter(df("rect") geoWithin rectangle(1, 3, 1, 3)).count() == 0)
      assert(df.filter(df("rect") geoContains rectangle(0.5, 1.5, 0.5, 1.5)).count() == 1)
      assert(df.filter(df("rect") geoContains rectangle(1, 3, 1, 3)).count() == 0)
    }

    // pushed down to XAP
    val df = sql.read.grid.loadClass[SpatialData]
    asserts(df)

    // executed in expressions on Spark
    val pdf = df.persist()
    asserts(pdf)
  }

}