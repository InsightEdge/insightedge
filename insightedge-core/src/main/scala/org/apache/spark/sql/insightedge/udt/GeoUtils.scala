package org.apache.spark.sql.insightedge.udt

import com.spatial4j.core.context.SpatialContext
import com.spatial4j.core.context.jts.{JtsSpatialContext, JtsSpatialContextFactory}
import com.spatial4j.core.shape.impl.{CircleImpl => SCircleImpl, PointImpl => SPointImpl}
import com.spatial4j.core.shape.{Circle => SCircle, Point => SPoint, Shape => SShape}
import org.apache.spark.sql.catalyst.util.{ArrayData, GenericArrayData}
import org.openspaces.spatial.shapes.{Circle => XCircle, Point => XPoint, Shape => XShape}
import org.openspaces.spatial.spatial4j.Spatial4jShapeProvider
import org.openspaces.spatial.{ShapeFactory => factory}

/**
  * Packs XAP spatial shapes into Spark ArrayData and unpacks them into Spark4j shapes.
  */
object GeoUtils {
  val pointId = 0
  val circleId = 1

  lazy val defaultContext = createDefaultSpatialContext()

  def createDefaultSpatialContext(): SpatialContext = {
    val factory = new JtsSpatialContextFactory()
    factory.geo = true
    new JtsSpatialContext(factory)
  }

  def pack(shape: Any): ArrayData = {
    val array = shape match {
      case point: XPoint =>
        Array(pointId, point.getX, point.getY)
      case circle: XCircle =>
        Array(circleId, circle.getCenterX, circle.getCenterY, circle.getRadius)
    }
    new GenericArrayData(array)
  }

  def unpackSpatialShape(anyData: Any, context: SpatialContext): SShape = {
    anyData match {
      case data: ArrayData =>
        data.getInt(0) match {
          case this.pointId =>
            new SPointImpl(data.getDouble(1), data.getDouble(2), context)
          case this.circleId =>
            new SCircleImpl(new SPointImpl(data.getDouble(1), data.getDouble(2), context), data.getDouble(3), context)
        }

      case data: XShape =>
        data.asInstanceOf[Spatial4jShapeProvider].getSpatial4jShape(context)
    }
  }

  def unpackXapPoint(data: ArrayData): XPoint = {
    factory.point(data.getDouble(1), data.getDouble(2))
  }

  def unpackXapCircle(data: ArrayData): XCircle = {
    factory.circle(factory.point(data.getDouble(1), data.getDouble(2)), data.getDouble(3))
  }

}