package org.apache.spark.sql.insightedge.udt

import org.apache.spark.sql.catalyst.util.ArrayData
import org.apache.spark.sql.types._
import org.openspaces.spatial.shapes.Circle

object CircleUDT extends UserDefinedType[Circle] {

  override def sqlType: DataType = ArrayType(DoubleType, containsNull = false)

  override def userClass: Class[Circle] = classOf[Circle]

  override def serialize(obj: Any): ArrayData = GeoUtils.pack(obj)

  override def deserialize(datum: Any): Circle = GeoUtils.unpackXapCircle(datum.asInstanceOf[ArrayData])

}