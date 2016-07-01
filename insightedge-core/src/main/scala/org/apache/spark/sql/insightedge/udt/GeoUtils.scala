package org.apache.spark.sql.insightedge.udt

import com.spatial4j.core.context.SpatialContext
import com.spatial4j.core.context.jts.{JtsSpatialContext, JtsSpatialContextFactory}
import com.spatial4j.core.shape.{Shape => SShape}
import org.apache.spark.sql.catalyst.util.{ArrayData, GenericArrayData}
import org.openspaces.spatial.shapes.{Circle => XCircle, LineString => XLineString, Point => XPoint, Polygon => XPolygon, Rectangle => XRectangle, Shape => XShape}
import org.openspaces.spatial.spatial4j.Spatial4jShapeProvider
import org.openspaces.spatial.ShapeFactory

import scala.collection.JavaConverters._

object GeoUtils {
  val pointId = 0.0
  val circleId = 1.0
  val rectangleId = 2.0
  val polygonId = 3.0
  val lineStringId = 4.0

  lazy val defaultContext = createDefaultSpatialContext()

  def createDefaultSpatialContext(): SpatialContext = {
    val factory = new JtsSpatialContextFactory()
    factory.geo = true
    new JtsSpatialContext(factory)
  }

  /**
    * Packs XAP spatial shapes into Spark ArrayData
    */
  def pack(shape: Any): ArrayData = {
    val array = shape match {
      case point: XPoint =>
        Array(pointId, point.getX, point.getY)
      case circle: XCircle =>
        Array(circleId, circle.getCenterX, circle.getCenterY, circle.getRadius)
      case rect: XRectangle =>
        Array(rectangleId, rect.getMinX, rect.getMaxX, rect.getMinY, rect.getMaxY)
      case polygon: XPolygon =>
        val list = (polygonId :: polygon.getNumOfPoints :: Nil) ++ (0 until polygon.getNumOfPoints).flatMap(index => Seq(polygon.getX(index), polygon.getY(index)))
        list.toArray
      case line: XLineString =>
        val list = (lineStringId :: line.getNumOfPoints :: Nil) ++ (0 until line.getNumOfPoints).flatMap(index => Seq(line.getX(index), line.getY(index)))
        list.toArray
    }
    new GenericArrayData(array)
  }

  /**
    * Unpacks Spark data into Spark4j shapes.
    */
  def unpackSpatialShape(anyData: Any, context: SpatialContext): SShape = {
    anyData match {
      case data: ArrayData =>
        data.getDouble(0) match {
          case this.pointId =>
            context.makePoint(data.getDouble(1), data.getDouble(2))
          case this.circleId =>
            context.makeCircle(data.getDouble(1), data.getDouble(2), data.getDouble(3))
          case this.rectangleId =>
            context.makeRectangle(data.getDouble(1), data.getDouble(2), data.getDouble(3), data.getDouble(4))
          case this.polygonId =>
            // there is no Polygon in spatial API, only JTS context supports this shape internally, so have to go this long route to parse
            unpackXapPolygon(data).asInstanceOf[Spatial4jShapeProvider].getSpatial4jShape(context)
          case this.lineStringId =>
            // 0 - shape type, 1 - points count, so offset = 2
            val points = (0 until data.getInt(1)).map(index => context.makePoint(data.getDouble(index * 2 + 2), data.getDouble(index * 2 + 3)))
            context.makeLineString(points.asJava)
        }

      case data: XShape =>
        data.asInstanceOf[Spatial4jShapeProvider].getSpatial4jShape(context)
    }
  }

  /** Unpacks Spark data into XAP point. */
  def unpackXapPoint(data: ArrayData): XPoint = {
    ShapeFactory.point(data.getDouble(1), data.getDouble(2))
  }

  /** Unpacks Spark data into XAP circle. */
  def unpackXapCircle(data: ArrayData): XCircle = {
    ShapeFactory.circle(ShapeFactory.point(data.getDouble(1), data.getDouble(2)), data.getDouble(3))
  }

  /** Unpacks Spark data into XAP rectangle. */
  def unpackXapRectangle(data: ArrayData): XRectangle = {
    ShapeFactory.rectangle(data.getDouble(1), data.getDouble(2), data.getDouble(3), data.getDouble(4))
  }

  /** Unpacks Spark data into XAP polygon. */
  def unpackXapPolygon(data: ArrayData): XPolygon = {
    // 0 - shape type, 1 - points count, so offset = 2
    val xapPoints = (0 until data.getInt(1)).map(index => ShapeFactory.point(data.getDouble(index * 2 + 2), data.getDouble(index * 2 + 3)))

    ShapeFactory.polygon(xapPoints.asJava)
  }

  /** Unpacks Spark data into XAP line string. */
  def unpackXapLineString(data: ArrayData): XLineString = {
    // 0 - shape type, 1 - points count, so offset = 2
    val xapPoints = (0 until data.getInt(1)).map(index => ShapeFactory.point(data.getDouble(index * 2 + 2), data.getDouble(index * 2 + 3)))

    ShapeFactory.lineString(xapPoints.asJava)
  }

}