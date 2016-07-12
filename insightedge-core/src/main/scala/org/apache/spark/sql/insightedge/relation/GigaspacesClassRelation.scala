package org.apache.spark.sql.insightedge.relation

import com.gigaspaces.spark.rdd.GigaSpacesSqlRDD
import org.apache.spark.rdd.RDD
import org.apache.spark.sql._
import org.apache.spark.sql.insightedge.InsightEdgeSourceOptions
import org.apache.spark.sql.types._

import scala.reflect.ClassTag

private[insightedge] case class GigaspacesClassRelation(
                                                         context: SQLContext,
                                                         clazz: ClassTag[AnyRef],
                                                         options: InsightEdgeSourceOptions
                                                       )
  extends GigaspacesAbstractRelation(context, options) with Serializable {

  override lazy val inferredSchema: StructType = {
    val schema = SchemaInference.schemaFor(clazz.runtimeClass, (c: Class[_]) => GigaspacesAbstractRelation.udtFor(c))
    schema.dataType.asInstanceOf[StructType]
  }

  override def insert(data: DataFrame, overwrite: Boolean): Unit = throw new UnsupportedOperationException("saving classes is unsupported")

  override def insert(data: DataFrame, mode: SaveMode): Unit = throw new UnsupportedOperationException("saving classes is unsupported")

  override def buildScan(query: String, params: Seq[Any], fields: Seq[String]): RDD[Row] = {
    val clazzName = clazz.runtimeClass.getName

    val rdd = new GigaSpacesSqlRDD(gsConfig, sc, query, params, fields, options.splitCount, options.readBufferSize)(clazz)

    rdd.mapPartitions { data => GigaspacesAbstractRelation.beansToRows(data, clazzName, schema, fields) }
  }

}

