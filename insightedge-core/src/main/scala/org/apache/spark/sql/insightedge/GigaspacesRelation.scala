package org.apache.spark.sql.insightedge

import com.gigaspaces.document.SpaceDocument
import com.gigaspaces.metadata.SpaceTypeDescriptorBuilder
import com.gigaspaces.spark.context.GigaSpacesConfig
import com.gigaspaces.spark.implicits._
import com.gigaspaces.spark.rdd.{GigaSpacesDocumentDataFrameRDD, GigaSpacesAbstractRDD, GigaSpacesClassDataFrameRDD}
import com.j_spaces.core.client.SQLQuery
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.rdd.RDD
import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.ScalaReflection
import org.apache.spark.sql.insightedge.GigaspacesRelation._
import org.apache.spark.sql.sources._
import org.apache.spark.sql.types._
import org.apache.spark.{TaskContext, Partition, Logging, SparkContext}

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.reflect._
import scala.reflect.runtime.universe._

private[insightedge] case class GigaspacesRelation(
                                                    override val sqlContext: SQLContext,
                                                    options: InsightEdgeSourceOptions
                                                  )
  extends BaseRelation
    with InsertableRelation
    with PrunedFilteredScan
    with Logging {

  private def sc: SparkContext = sqlContext.sparkContext

  private def gsConfig: GigaSpacesConfig = GigaSpacesConfig.fromSparkConf(sc.getConf)

  override def schema: StructType = {
    if (options.schema.nonEmpty) {
      options.schema.get
    } else if (options.clazz.nonEmpty) {
      buildSchemaFromClass(options.clazz.get)
    } else if (options.collection.nonEmpty) {
      buildSchemaFromDocument(options.collection.get)
    } else {
      throw new Exception("'clazz' or 'collection' must be specified")
    }
  }

  override def insert(data: DataFrame, overwrite: Boolean): Unit = {
    logInfo("trying to insert")
  }

  def save(data: DataFrame, mode: SaveMode): Unit = {
    if (options.collection.isEmpty) {
      throw new IllegalStateException("saving without 'collection' specified is not supported")
    }

    val collection = options.collection.get

    val query = new SQLQuery[SpaceDocument](collection, "", Seq()).setProjections("")
    mode match {
      case SaveMode.Overwrite => sc.gigaSpace.takeMultiple(query)
      case SaveMode.ErrorIfExists => if (sc.gigaSpace.read(query) != null) sys.error(s"'$collection' already exists")
      case SaveMode.Ignore => if (sc.gigaSpace.read(query) != null) return
      case SaveMode.Append =>
    }

    sc.gigaSpace.getTypeManager.registerTypeDescriptor(schema.fields.foldLeft(new SpaceTypeDescriptorBuilder(collection)) { (builder, field) =>
      builder.addFixedProperty(field.name, dataTypeToJava(field.dataType))
    }.create())

    data.rdd.map(row => {
      new SpaceDocument(collection, row.getValuesMap(schema.fieldNames))
    }).saveToGrid()
  }

  override def buildScan(requiredColumns: Array[String], filters: Array[Filter]): RDD[Row] = {
    val fields = if (requiredColumns.nonEmpty) requiredColumns else schema.fieldNames
    val (query, params) = filtersToSql(filters)

    if (options.clazz.nonEmpty) {
      def converter(element: Any): Row = {
        Row.fromSeq(fields.map(getValueByName(element, _)))
      }
      new GigaSpacesClassDataFrameRDD(gsConfig, sc, query, params, requiredColumns.toSeq, converter, options.readBufferSize)(options.clazz.get)
    } else {
      def converter(document: SpaceDocument): Row = {
        Row.fromSeq(fields.map(document.getProperty))
      }
      new GigaSpacesDocumentDataFrameRDD(gsConfig, sc, options.collection.get, query, params, requiredColumns.toSeq, converter, options.readBufferSize)
    }
  }

  override def unhandledFilters(filters: Array[Filter]): Array[Filter] = GigaspacesRelation.unsupportedFilters(filters)

  private def buildSchemaFromClass[R: ClassTag]: StructType = ScalaReflection.schemaFor(readType[R]()).dataType.asInstanceOf[StructType]

  private def buildSchemaFromDocument(name: String): StructType = {
    val descriptor = sqlContext.sparkContext.gigaSpace.getTypeManager.getTypeDescriptor(name)
    if (descriptor == null) throw new Exception("collection 'name' does not exist, have you written it before?")

    val structFields = (0 until descriptor.getNumOfFixedProperties).map(descriptor.getFixedProperty).map { f =>
      new StructField(f.getName, dataTypeFromJava(f.getType), nullable = true)
    }
    new StructType(structFields.toArray)
  }

  private def getValueByName[R](element: R, fieldName: String): AnyRef = {
    element.getClass.getMethod(fieldName).invoke(element)
  }

  private def readType[R: ClassTag](): Type = runtimeMirror(this.getClass.getClassLoader).classSymbol(classTag[R].runtimeClass).toType

}

private[insightedge] object GigaspacesRelation {

  def unsupportedFilters(filters: Array[Filter]): Array[Filter] = {
    filters.filterNot(GigaspacesRelation.canHandleFilter)
  }

  def canHandleFilter(filter: Filter): Boolean = {
    filter match {
      case _: EqualTo => true
      case _: EqualNullSafe => true
      case _: GreaterThan => true
      case _: GreaterThanOrEqual => true
      case _: LessThan => true
      case _: LessThanOrEqual => true
      case _: In => true
      case _: IsNull => true
      case _: IsNotNull => true
      case f: And => canHandleFilter(f.left) && canHandleFilter(f.right)
      case f: Or => canHandleFilter(f.left) && canHandleFilter(f.right)
      case f: Not => false
      case _: StringStartsWith => false
      case _: StringEndsWith => false
      case _: StringContains => false
      case other => false
    }
  }

  def filtersToSql(filters: Array[Filter]): (String, Seq[Any]) = {
    val parameters = ListBuffer.empty[Any]
    val builder = filters
      .filter(canHandleFilter)
      .foldLeft(new StringBuilder) { (builder, filter) => builder -> (if (builder.isEmpty) "(" else " and (") ->(filter, parameters) -> ")" }
    (builder.toString(), parameters.toSeq)
  }

  def appendFilter(filter: Filter, builder: StringBuilder, params: ListBuffer[Any]): Unit = {
    filter match {
      case f: EqualTo =>
        builder -> f.attribute -> " = ?"
        params += f.value

      case f: EqualNullSafe =>
        builder -> f.attribute -> " = ?"
        params += f.value

      case f: GreaterThan =>
        builder -> f.attribute -> " > ?"
        params += f.value

      case f: GreaterThanOrEqual =>
        builder -> f.attribute -> " >= ?"
        params += f.value

      case f: LessThan =>
        builder -> f.attribute -> " < ?"
        params += f.value

      case f: LessThanOrEqual =>
        builder -> f.attribute -> " <= ?"
        params += f.value

      case f: In =>
        builder -> f.attribute
        f.values.map(value => "?").addString(builder, " in (", ",", ")")
        params ++= f.values

      case f: IsNull =>
        builder -> f.attribute -> " is null"

      case f: IsNotNull =>
        builder -> f.attribute -> " is not null"

      case f: And =>
        builder -> "(" ->(f.left, params) -> ") and (" ->(f.right, params) -> ")"

      case f: Or =>
        builder -> "(" ->(f.left, params) -> ") or (" ->(f.right, params) -> ")"
    }
  }

  implicit class BuilderExtension(val builder: StringBuilder) {
    def ->(any: Any): StringBuilder = {
      builder.append(any)
    }

    def ->(filter: Filter, params: ListBuffer[Any]): StringBuilder = {
      GigaspacesRelation.appendFilter(filter, builder, params)
      builder
    }
  }

  def dataTypeToJava(dataType: DataType): Class[_] = {
    classOf[Object]
  }

  def dataTypeFromJava(clazz: Class[_]): DataType = {
    ObjectType(clazz)
  }

}