package org.apache.spark.sql.insightedge.udt

import org.apache.spark.sql.catalyst.util.ArrayData
import org.apache.spark.sql.types._
import org.openspaces.spatial.shapes.Point

class PointUDT extends UserDefinedType[Point] with Serializable {

  override def sqlType: DataType = ArrayType(DoubleType, containsNull = false)

  override def userClass: Class[Point] = classOf[Point]

  override def serialize(obj: Any): ArrayData = GeoUtils.pack(obj)

  override def deserialize(datum: Any): Point = GeoUtils.unpackXapPoint(datum.asInstanceOf[ArrayData])

}