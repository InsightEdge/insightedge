package org.apache.spark.sql.insightedge

import org.apache.spark.Logging
import org.apache.spark.rdd.RDD
import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.CatalystTypeConverters._
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.planning.PhysicalOperation
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.catalyst.{InternalRow, expressions}
import org.apache.spark.sql.execution.PhysicalRDD._
import org.apache.spark.sql.execution.datasources.{DataSourceStrategy, LogicalRelation}
import org.apache.spark.sql.sources._
import org.openspaces.spatial.shapes.Shape

import scala.collection.mutable.ArrayBuffer

/**
  * @author Leonid_Poliakov
  */
object InsightEdgeSourceStrategy extends Strategy with Logging {

  /**
    * Catches GridPrunedFilteredScan which is implemented by dataframe extension for grid datasource.
    */
  def apply(plan: LogicalPlan): Seq[execution.SparkPlan] = plan match {
    case PhysicalOperation(projects, filters, relation@LogicalRelation(t: GridPrunedFilteredScan, _)) =>
      pruneFilterProject(
        relation,
        projects,
        filters,
        (attributes, filters) => toCatalystRDD(relation, attributes, t.buildScan(attributes.map(_.name).toArray, filters))) :: Nil

    case _ => Nil
  }

  /**
    * Translates grid expressions to grid filters, for others - uses default Spark code.
    */
  protected[sql] def translateFilter(predicate: Expression): Option[Filter] = {
    predicate match {
      case expression.SubtypeOf(a: Attribute, Literal(v, t)) => Some(filter.SubtypeOf(a.name, convertToScala(v, t).asInstanceOf[Class[_]]))
      case expression.GeoIntersects(a: Attribute, Literal(v, t)) => Some(filter.GeoIntersects(a.name, convertToScala(v, t).asInstanceOf[Shape]))
      case _ => DataSourceStrategy.translateFilter(predicate)
    }
  }


  // Code below is copied as-is from the DataSourceStrategy


  // Based on Public API.
  protected def pruneFilterProject(
                                    relation: LogicalRelation,
                                    projects: Seq[NamedExpression],
                                    filterPredicates: Seq[Expression],
                                    scanBuilder: (Seq[Attribute], Array[Filter]) => RDD[InternalRow]) = {
    pruneFilterProjectRaw(
      relation,
      projects,
      filterPredicates,
      (requestedColumns, _, pushedFilters) => {
        scanBuilder(requestedColumns, pushedFilters.toArray)
      })
  }

  // Based on Catalyst expressions. The `scanBuilder` function accepts three arguments:
  //
  //  1. A `Seq[Attribute]`, containing all required column attributes. Used to handle relation
  //     traits that support column pruning (e.g. `PrunedScan` and `PrunedFilteredScan`).
  //
  //  2. A `Seq[Expression]`, containing all gathered Catalyst filter expressions, only used for
  //     `CatalystScan`.
  //
  //  3. A `Seq[Filter]`, containing all data source `Filter`s that are converted from (possibly a
  //     subset of) Catalyst filter expressions and can be handled by `relation`.  Used to handle
  //     relation traits (`CatalystScan` excluded) that support filter push-down (e.g.
  //     `PrunedFilteredScan` and `HadoopFsRelation`).
  //
  // Note that 2 and 3 shouldn't be used together.
  protected def pruneFilterProjectRaw(
                                       relation: LogicalRelation,
                                       projects: Seq[NamedExpression],
                                       filterPredicates: Seq[Expression],
                                       scanBuilder: (Seq[Attribute], Seq[Expression], Seq[Filter]) => RDD[InternalRow]) = {

    val projectSet = AttributeSet(projects.flatMap(_.references))
    val filterSet = AttributeSet(filterPredicates.flatMap(_.references))

    val candidatePredicates = filterPredicates.map {
      _ transform {
        case a: AttributeReference => relation.attributeMap(a) // Match original case of attributes.
      }
    }

    val (unhandledPredicates, pushedFilters) =
      selectFilters(relation.relation, candidatePredicates)

    // A set of column attributes that are only referenced by pushed down filters.  We can eliminate
    // them from requested columns.
    val handledSet = {
      val handledPredicates = filterPredicates.filterNot(unhandledPredicates.contains)
      val unhandledSet = AttributeSet(unhandledPredicates.flatMap(_.references))
      AttributeSet(handledPredicates.flatMap(_.references)) --
        (projectSet ++ unhandledSet).map(relation.attributeMap)
    }

    // Combines all Catalyst filter `Expression`s that are either not convertible to data source
    // `Filter`s or cannot be handled by `relation`.
    val filterCondition = unhandledPredicates.reduceLeftOption(expressions.And)

    val metadata: Map[String, String] = {
      val pairs = ArrayBuffer.empty[(String, String)]

      if (pushedFilters.nonEmpty) {
        pairs += (PUSHED_FILTERS -> pushedFilters.mkString("[", ", ", "]"))
      }

      relation.relation match {
        case r: HadoopFsRelation => pairs += INPUT_PATHS -> r.paths.mkString(", ")
        case _ =>
      }

      pairs.toMap
    }

    if (projects.map(_.toAttribute) == projects &&
      projectSet.size == projects.size &&
      filterSet.subsetOf(projectSet)) {
      // When it is possible to just use column pruning to get the right projection and
      // when the columns of this projection are enough to evaluate all filter conditions,
      // just do a scan followed by a filter, with no extra project.
      val requestedColumns = projects
        // Safe due to if above.
        .asInstanceOf[Seq[Attribute]]
        // Match original case of attributes.
        .map(relation.attributeMap)
        // Don't request columns that are only referenced by pushed filters.
        .filterNot(handledSet.contains)

      val scan = execution.PhysicalRDD.createFromDataSource(
        projects.map(_.toAttribute),
        scanBuilder(requestedColumns, candidatePredicates, pushedFilters),
        relation.relation, metadata)
      filterCondition.map(execution.Filter(_, scan)).getOrElse(scan)
    } else {
      // Don't request columns that are only referenced by pushed filters.
      val requestedColumns =
        (projectSet ++ filterSet -- handledSet).map(relation.attributeMap).toSeq

      val scan = execution.PhysicalRDD.createFromDataSource(
        requestedColumns,
        scanBuilder(requestedColumns, candidatePredicates, pushedFilters),
        relation.relation, metadata)
      execution.Project(
        projects, filterCondition.map(execution.Filter(_, scan)).getOrElse(scan))
    }
  }

  /**
    * Selects Catalyst predicate [[Expression]]s which are convertible into data source [[Filter]]s
    * and can be handled by `relation`.
    *
    * @return A pair of `Seq[Expression]` and `Seq[Filter]`. The first element contains all Catalyst
    *         predicate [[Expression]]s that are either not convertible or cannot be handled by
    *         `relation`. The second element contains all converted data source [[Filter]]s that
    *         will be pushed down to the data source.
    */
  protected[sql] def selectFilters(
                                    relation: BaseRelation,
                                    predicates: Seq[Expression]): (Seq[Expression], Seq[Filter]) = {

    // For conciseness, all Catalyst filter expressions of type `expressions.Expression` below are
    // called `predicate`s, while all data source filters of type `sources.Filter` are simply called
    // `filter`s.

    val translated: Seq[(Expression, Filter)] =
      for {
        predicate <- predicates
        filter <- translateFilter(predicate)
      } yield predicate -> filter

    // A map from original Catalyst expressions to corresponding translated data source filters.
    val translatedMap: Map[Expression, Filter] = translated.toMap

    // Catalyst predicate expressions that cannot be translated to data source filters.
    val unrecognizedPredicates = predicates.filterNot(translatedMap.contains)

    // Data source filters that cannot be handled by `relation`. The semantic of a unhandled filter
    // at here is that a data source may not be able to apply this filter to every row
    // of the underlying dataset.
    val unhandledFilters = relation.unhandledFilters(translatedMap.values.toArray).toSet

    val (unhandled, handled) = translated.partition {
      case (predicate, filter) =>
        unhandledFilters.contains(filter)
    }

    // Catalyst predicate expressions that can be translated to data source filters, but cannot be
    // handled by `relation`.
    val (unhandledPredicates, _) = unhandled.unzip

    // Translated data source filters that can be handled by `relation`
    val (_, handledFilters) = handled.unzip

    // translated contains all filters that have been converted to the public Filter interface.
    // We should always push them to the data source no matter whether the data source can apply
    // a filter to every row or not.
    val (_, translatedFilters) = translated.unzip

    (unrecognizedPredicates ++ unhandledPredicates, translatedFilters)
  }

  /**
    * Convert RDD of Row into RDD of InternalRow with objects in catalyst types
    */
  private[this] def toCatalystRDD(
                                   relation: LogicalRelation,
                                   output: Seq[Attribute],
                                   rdd: RDD[Row]): RDD[InternalRow] = {
    if (relation.relation.needConversion) {
      execution.RDDConversions.rowToRowRdd(rdd, output.map(_.dataType))
    } else {
      rdd.asInstanceOf[RDD[InternalRow]]
    }
  }
}
