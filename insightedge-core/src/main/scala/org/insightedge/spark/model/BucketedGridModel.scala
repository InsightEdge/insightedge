package org.insightedge.spark.model

import com.gigaspaces.metadata.index.SpaceIndexType
import org.insightedge.spark.annotation
import annotation._

import scala.beans.BeanProperty

/**
 * Trait used to define bucketed space classes. Bucketing allows to have more Spark partitions than Data Grid partitions, i.e.
 * splitting Data Grid partitions into several buckets and assigning a bucket per Spark partition.
 *
 * @author Leonid_Poliakov
 */
trait BucketedGridModel {
  @BeanProperty
  @SpaceIndex(`type` = SpaceIndexType.EXTENDED)
  var metaBucketId: Integer = null
}