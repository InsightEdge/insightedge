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
    val schema = SchemaInference.schemaFor(clazz.runtimeClass, (c: Class[_]) => InsightEdgeAbstractRelation.udtFor(c))
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

