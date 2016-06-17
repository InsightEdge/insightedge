package org.apache.spark.sql.insightedge.filter

import org.apache.spark.sql.sources.Filter

/**
  * @author Leonid_Poliakov
  */
case class SubtypeOf(attribute: String, value: Class[_]) extends Filter