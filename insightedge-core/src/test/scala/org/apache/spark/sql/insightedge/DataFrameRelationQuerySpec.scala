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

package org.apache.spark.sql.insightedge

import org.insightedge.spark.fixture.{IEConfig, InsightEdge}
import org.apache.spark.sql.insightedge.relation.InsightEdgeAbstractRelation
import InsightEdgeAbstractRelation.{filtersToSql, unsupportedFilters}
import org.apache.spark.sql.insightedge.filter.{GeoContains, GeoIntersects, GeoWithin}
import org.apache.spark.sql.sources._
import org.insightedge.spark.fixture.Spark
import org.insightedge.spark.utils.ScalaSpaceClass
import org.openspaces.spatial.ShapeFactory.{circle, point}
import org.scalatest.fixture

class DataFrameRelationQuerySpec extends fixture.FlatSpec with IEConfig with InsightEdge with Spark {

  it should "should handle simple filters" taggedAs ScalaSpaceClass in{ f=>
    val unhandled = unsupportedFilters(Array(
      EqualTo("key", 0),
      EqualNullSafe("key", 0),
      GreaterThan("key", 0),
      GreaterThanOrEqual("key", 0),
      LessThan("key", 0),
      LessThanOrEqual("key", 0),
      In("key", Array(0, 1, 2, 3)),
      IsNull("key"),
      IsNotNull("key")
    ))
    assert(unhandled.length == 0)
  }

  it should "should handle and/or filters with handled children" taggedAs ScalaSpaceClass in{ f=>
    val unhandled = unsupportedFilters(Array(
      And(GreaterThan("key", 0), LessThan("key", 1)),
      Or(GreaterThan("key", 1), LessThan("key", 0)),
      And(
        Or(GreaterThan("key", 0), LessThan("key", 0)),
        Or(GreaterThan("key", 0), LessThan("key", 0))
      )
    ))
    assert(unhandled.length == 0)
  }

  it should "should not handle some filters" taggedAs ScalaSpaceClass in{ f=>
    val unhandled = unsupportedFilters(Array(
      StringStartsWith("key", "value"),
      StringEndsWith("key", "value"),
      StringContains("key", "value"),
      Not(IsNull("key"))
    ))
    assert(unhandled.length == 4)
  }

  it should "should not handle and/or filters with unhandled children" taggedAs ScalaSpaceClass in{ f=>
    val unhandled = unsupportedFilters(Array(
      And(GreaterThan("key", 0), StringContains("key", "value")),
      Or(GreaterThan("key", 1), StringContains("key", "value")),
      And(
        Or(GreaterThan("key", 0), Not(EqualTo("key", 0))),
        Or(GreaterThan("key", 0), LessThan("key", 0))
      )
    ))
    assert(unhandled.length == 3)
  }


  it should "should create simple sql query" taggedAs ScalaSpaceClass in{ f=>
    implicit class FilterAsserts(val filter: Filter) {
      def gives(query: String, params: Seq[Any]): Boolean = {
        val (actualQuery, actualParams) = filtersToSql(Array(filter))
        actualQuery.equals(query) && actualParams.equals(params)
      }
    }

    assert(EqualTo("key", 0)
      gives("(key = ?)", Seq(0))
    )
    assert(EqualNullSafe("key", 0)
      gives("(key = ?)", Seq(0))
    )
    assert(GreaterThan("key", 0)
      gives("(key > ?)", Seq(0))
    )
    assert(GreaterThanOrEqual("key", 0)
      gives("(key >= ?)", Seq(0))
    )
    assert(LessThan("key", 0)
      gives("(key < ?)", Seq(0))
    )
    assert(LessThanOrEqual("key", 0)
      gives("(key <= ?)", Seq(0))
    )
    assert(In("key", Array(0, 1, 2, 3))
      gives("(key in (?,?,?,?))", Seq(0, 1, 2, 3))
    )
    assert(IsNull("key")
      gives("(key is null)", Seq())
    )
    assert(IsNotNull("key")
      gives("(key is not null)", Seq())
    )

    assert(GeoIntersects("key", circle(point(0, 0), 1))
      gives("(key spatial:intersects ?)", Seq(circle(point(0, 0), 1)))
    )
    assert(GeoWithin("key", circle(point(0, 0), 1))
      gives("(key spatial:within ?)", Seq(circle(point(0, 0), 1)))
    )
    assert(GeoContains("key", circle(point(0, 0), 1))
      gives("(key spatial:contains ?)", Seq(circle(point(0, 0), 1)))
    )
  }

  it should "should create sql query ignoring unhandled filters" taggedAs ScalaSpaceClass in{ f=>

    val (query, params) = filtersToSql(
      Array(
        Or(EqualTo("role", "ADMIN"), EqualTo("name", "superuser")),
        And(IsNull("deletionDate"), IsNotNull("creationDate")),
        Not(EqualTo("blocked", true))
      )
    )
    assert(query equals "((role = ?) or (name = ?)) and ((deletionDate is null) and (creationDate is not null))")
    assert(params equals Seq("ADMIN", "superuser"))
  }

  it should "should create empty sql query" taggedAs ScalaSpaceClass in{ f=>
    assert(filtersToSql(Array()) equals("", Seq()))
  }

  it should "should create sql query with nested properties" taggedAs ScalaSpaceClass in{ f=>

    val (query, params) = filtersToSql(
      Array(EqualTo("address.city", "Buffalo"))
    )
    assert(query equals "(address.city = ?)")
    assert(params equals Seq("Buffalo"))
  }
}
