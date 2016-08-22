package org.insightedge.scala

object annotation {

  import com.gigaspaces.annotation.pojo
  import org.openspaces.spatial

  import scala.annotation.meta.beanGetter

  type SpaceClass = pojo.SpaceClass
  type SpaceClassConstructor = pojo.SpaceClassConstructor

  // Enhance space annotations with @beanGetter property
  type SpaceDynamicProperties = pojo.SpaceDynamicProperties@beanGetter
  type SpaceExclude = pojo.SpaceExclude@beanGetter
  type SpaceFifoGroupingIndex = pojo.SpaceFifoGroupingIndex@beanGetter
  type SpaceFifoGroupingProperty = pojo.SpaceFifoGroupingProperty@beanGetter
  type SpaceId = pojo.SpaceId@beanGetter
  type SpaceIndex = pojo.SpaceIndex@beanGetter
  type SpaceIndexes = pojo.SpaceIndexes@beanGetter
  type SpaceLeaseExpiration = pojo.SpaceLeaseExpiration@beanGetter
  type SpacePersist = pojo.SpacePersist@beanGetter
  type SpaceProperty = pojo.SpaceProperty@beanGetter
  type SpaceRouting = pojo.SpaceRouting@beanGetter
  type SpaceStorageType = pojo.SpaceStorageType@beanGetter
  type SpaceVersion = pojo.SpaceVersion@beanGetter
  type SpaceSpatialIndex = spatial.SpaceSpatialIndex@beanGetter
  type SpaceSpatialIndexes = spatial.SpaceSpatialIndexes@beanGetter

}
