package org.apache.spark.sql.insightedge.udt

import org.apache.spark.sql.catalyst.util.ArrayData
import org.apache.spark.sql.types._
import org.openspaces.spatial.shapes.Rectangle

class RectangleUDT extends UserDefinedType[Rectangle] with Serializable {

  override def sqlType: DataType = ArrayType(DoubleType, containsNull = false)

  override def userClass: Class[Rectangle] = classOf[Rectangle]

  override def serialize(obj: Rectangle): ArrayData = GeoUtils.pack(obj)

  override def deserialize(datum: Any): Rectangle = GeoUtils.unpackXapRectangle(datum.asInstanceOf[ArrayData])

}