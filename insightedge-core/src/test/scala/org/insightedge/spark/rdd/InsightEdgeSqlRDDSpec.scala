/*
 * Copyright (c) 2016, GigaSpaces Technologies, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.insightedge.spark.rdd

import org.insightedge.spark.fixture.{InsightEdge, IEConfig}
import org.insightedge.spark.implicits
import implicits.basic._
import org.insightedge.spark.utils._
import org.insightedge.spark.fixture.Spark
import org.insightedge.spark.utils.{JavaSpaceClass, ScalaSpaceClass}
import org.scalatest.FlatSpec

class InsightEdgeSqlRDDSpec extends FlatSpec with IEConfig with InsightEdge with Spark {

  it should "query data from Data Grid with a help of SQL" taggedAs ScalaSpaceClass in {
    writeDataSeqToDataGrid(1000)
    val sqlRdd = sc.gridSql[Data]("data IN (?,?,?)", Seq("data100", "data101", "data102"))

    val count = sqlRdd.count()
    assert(count == 3, "Wrong objects count")

    assert(!sqlRdd.supportsBuckets())
    assert(sqlRdd.partitions.length == 2)
    assert(GridProxyFactory.directCacheSize() == 2)
  }

  it should "query data from Data Grid with a help of SQL [java]" taggedAs JavaSpaceClass in {
    writeJDataSeqToDataGrid(1000)
    val sqlRdd = sc.gridSql[JData]("data IN (?,?,?)", Seq("data100", "data101", "data102"))

    val count = sqlRdd.count()
    assert(count == 3, "Wrong objects count")

    assert(!sqlRdd.supportsBuckets())
    assert(sqlRdd.partitions.length == 2)
    assert(GridProxyFactory.directCacheSize() == 2)
  }

  it should "query bucketed data from Data Grid with a help of SQL" taggedAs ScalaSpaceClass in {
    writeBucketedDataSeqToDataGrid(1000)
    val sqlRdd = sc.gridSql[BucketedData]("data IN (?,?,?)", Seq("data100", "data101", "data102"))

    val count = sqlRdd.count()
    assert(count == 3, "Wrong objects count")

    assert(sqlRdd.supportsBuckets())
    assert(sqlRdd.partitions.length == 2 * 4)
    assert(GridProxyFactory.directCacheSize() == 2)
  }

  it should "query bucketed data from Data Grid with a help of SQL [java]" taggedAs JavaSpaceClass in {
    writeJBucketedDataSeqToDataGrid(1000)
    val sqlRdd = sc.gridSql[JBucketedData]("data IN (?,?,?)", Seq("data100", "data101", "data102"))

    val count = sqlRdd.count()
    assert(count == 3, "Wrong objects count")

    assert(sqlRdd.supportsBuckets())
    assert(sqlRdd.partitions.length == 2 * 4)
    assert(GridProxyFactory.directCacheSize() == 2)
  }

  it should "have bucketed partitions set by user" taggedAs ScalaSpaceClass in {
    writeBucketedDataSeqToDataGrid(1000)
    val sqlRdd = sc.gridSql[BucketedData]("data IN (?,?,?)", Seq("data100", "data101", "data102"), splitCount = Some(8))

    val count = sqlRdd.count()
    assert(count == 3, "Wrong objects count")

    assert(sqlRdd.supportsBuckets())
    assert(sqlRdd.partitions.length == 2 * 8)
    assert(GridProxyFactory.directCacheSize() == 2)
  }

  it should "have bucketed partitions set by user [java]" taggedAs JavaSpaceClass in {
    writeJBucketedDataSeqToDataGrid(1000)
    val sqlRdd = sc.gridSql[JBucketedData]("data IN (?,?,?)", Seq("data100", "data101", "data102"), splitCount = Some(8))

    val count = sqlRdd.count()
    assert(count == 3, "Wrong objects count")

    assert(sqlRdd.supportsBuckets())
    assert(sqlRdd.partitions.length == 2 * 8)
    assert(GridProxyFactory.directCacheSize() == 2)
  }

}
