package org.apache.spark.sql.insightedge.udt

import org.apache.spark.sql.catalyst.util.ArrayData
import org.apache.spark.sql.types._
import org.openspaces.spatial.shapes.LineString

class LineStringUDT extends UserDefinedType[LineString] with Serializable {

  override def sqlType: DataType = ArrayType(DoubleType, containsNull = false)

  override def userClass: Class[LineString] = classOf[LineString]

  override def serialize(obj: Any): ArrayData = GeoUtils.pack(obj)

  override def deserialize(datum: Any): LineString = GeoUtils.unpackXapLineString(datum.asInstanceOf[ArrayData])

}