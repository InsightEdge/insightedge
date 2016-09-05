package org.apache.spark.sql.insightedge.relation

import org.apache.spark.rdd.RDD
import org.apache.spark.sql._
import org.apache.spark.sql.insightedge.InsightEdgeSourceOptions
import org.apache.spark.sql.types._
import org.insightedge.spark.rdd.InsightEdgeSqlRDD

import scala.reflect.ClassTag

private[insightedge] case class InsightEdgeClassRelation(
                                                         context: SQLContext,
                                                         clazz: ClassTag[AnyRef],
                                                         options: InsightEdgeSourceOptions
                                                       )
  extends InsightEdgeAbstractRelation(context, options) with Serializable {

  override lazy val inferredSchema: StructType = {
    val schema = SchemaInference.schemaFor(clazz.runtimeClass)
    schema.dataType.asInstanceOf[StructType]
  }

  override def insert(data: DataFrame, overwrite: Boolean): Unit = throw new UnsupportedOperationException("saving classes is unsupported")

  override def insert(data: DataFrame, mode: SaveMode): Unit = throw new UnsupportedOperationException("saving classes is unsupported")

  override def buildScan(query: String, params: Seq[Any], fields: Seq[String]): RDD[Row] = {
    val clazzName = clazz.runtimeClass.getName

    val rdd = new InsightEdgeSqlRDD(ieConfig, sc, query, params, fields, options.splitCount, options.readBufferSize)(clazz)

    rdd.mapPartitions { data => InsightEdgeAbstractRelation.beansToRows(data, clazzName, schema, fields) }
  }

}

