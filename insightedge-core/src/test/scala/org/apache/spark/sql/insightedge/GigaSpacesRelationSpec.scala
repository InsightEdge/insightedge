package org.apache.spark.sql.insightedge

import com.gigaspaces.spark.rdd.Data
import com.gigaspaces.spark.utils.{GigaSpaces, GsConfig, Spark}
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.insightedge.GigaspacesRelation.filtersToSql
import org.apache.spark.sql.sources._
import org.scalatest.FunSpec

import scala.collection.mutable.ListBuffer

class GigaSpacesRelationSpec extends FunSpec with GsConfig with GigaSpaces with Spark {

  it("should handle simple filters") {
    val relation = new GigaspacesRelation(new SQLContext(sc))
    val unhandled = relation.unhandledFilters(Array(
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

  it("should handle and/or filters with handled children") {
    val relation = new GigaspacesRelation(new SQLContext(sc))
    val unhandled = relation.unhandledFilters(Array(
      And(GreaterThan("key", 0), LessThan("key", 1)),
      Or(GreaterThan("key", 1), LessThan("key", 0)),
      And(
        Or(GreaterThan("key", 0), LessThan("key", 0)),
        Or(GreaterThan("key", 0), LessThan("key", 0))
      )
    ))
    assert(unhandled.length == 0)
  }

  it("should not handle some filters") {
    val relation = new GigaspacesRelation(new SQLContext(sc))
    val unhandled = relation.unhandledFilters(Array(
      StringStartsWith("key", "value"),
      StringEndsWith("key", "value"),
      StringContains("key", "value"),
      Not(IsNull("key"))
    ))
    assert(unhandled.length == 4)
  }

  it("should not handle and/or filters with unhandled children") {
    val relation = new GigaspacesRelation(new SQLContext(sc))
    val unhandled = relation.unhandledFilters(Array(
      And(GreaterThan("key", 0), StringContains("key", "value")),
      Or(GreaterThan("key", 1), StringContains("key", "value")),
      And(
        Or(GreaterThan("key", 0), Not(EqualTo("key", 0))),
        Or(GreaterThan("key", 0), LessThan("key", 0))
      )
    ))
    assert(unhandled.length == 3)
  }

  it("should create simple sql query") {
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
  }

  it("should create sql query ignoring unhandled filters") {
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

  it("should create empty sql query") {
    assert(filtersToSql(Array()) equals("", Seq()))
  }
}
