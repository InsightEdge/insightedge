package org.apache.spark.sql.insightedge.udt

import org.apache.spark.sql.catalyst.util.ArrayData
import org.apache.spark.sql.types._
import org.openspaces.spatial.shapes.Polygon

class PolygonUDT extends UserDefinedType[Polygon] with Serializable {

  override def sqlType: DataType = ArrayType(DoubleType, containsNull = false)

  override def userClass: Class[Polygon] = classOf[Polygon]

  override def serialize(obj: Any): ArrayData = GeoUtils.pack(obj)

  override def deserialize(datum: Any): Polygon = GeoUtils.unpackXapPolygon(datum.asInstanceOf[ArrayData])

}