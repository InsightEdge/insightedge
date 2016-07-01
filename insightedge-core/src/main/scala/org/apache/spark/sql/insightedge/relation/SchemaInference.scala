package org.apache.spark.sql.insightedge.relation

import java.beans.Introspector

import com.google.common.reflect.TypeToken
import org.apache.spark.sql.catalyst.ScalaReflectionLock
import org.apache.spark.sql.types._
import org.apache.spark.util.Utils

import scala.collection.Map

/**
  * Converts scala, java or mixed types to dataframe schema.
  *
  * Is extended version of {@link org.apache.spark.sql.catalyst.ScalaReflection}
  */
object SchemaInference {
  /**
    * The universe we work in (runtime or macro)
    */
  val universe: scala.reflect.runtime.universe.type = scala.reflect.runtime.universe

  /**
    * The mirror used to access types in the universe
    *
    * Since we are creating a runtime mirror usign the class loader of current thread,
    * we need to use def at here. So, every time we call mirror, it is using the
    * class loader of the current thread.
    */
  def mirror: universe.Mirror = universe.runtimeMirror(Thread.currentThread().getContextClassLoader)

  import universe._

  /**
    * Used to simplify getting datatype and nullable
    */
  case class Schema(dataType: DataType, nullable: Boolean)

  /**
    * @return datatype for a class
    */
  def schemaFor(clazz: Class[_], udtResolver: Class[_] => Option[UserDefinedType[_]]): Schema = schemaFor(classToType(clazz), udtResolver)

  /**
    * @return datatype for a given root or embedded type
    */
  def schemaFor(tpe: Type, udts: Class[_] => Option[UserDefinedType[_]]): Schema = ScalaReflectionLock.synchronized {
    val className = getClassNameFromType(tpe)

    tpe match {
      case t if Utils.classIsLoadable(className) && Utils.classForName(className).isAnnotationPresent(classOf[SQLUserDefinedType]) =>
        // Note: We check for classIsLoadable above since Utils.classForName uses Java reflection,
        //       whereas className is from Scala reflection.  This can make it hard to find classes
        //       in some cases, such as when a class is enclosed in an object (in which case
        //       Java appends a '$' to the object name but Scala does not).
        val udt = Utils.classForName(className).getAnnotation(classOf[SQLUserDefinedType]).udt().newInstance()
        Schema(udt, nullable = true)

      // TODO: remove double-resolve of UDT
      case t if Utils.classIsLoadable(className) && udts(Utils.classForName(className)).isDefined =>
        val udt = udts(Utils.classForName(className)).get
        Schema(udt, nullable = true)

      case t if t <:< localTypeOf[Option[_]] =>
        val TypeRef(_, _, Seq(optType)) = t
        Schema(schemaFor(optType, udts).dataType, nullable = true)

      case t if t <:< localTypeOf[Array[Byte]] =>
        Schema(BinaryType, nullable = true)

      case t if t <:< localTypeOf[Array[_]] =>
        val TypeRef(_, _, Seq(elementType)) = t
        val Schema(dataType, nullable) = schemaFor(elementType, udts)
        Schema(ArrayType(dataType, containsNull = nullable), nullable = true)

      case t if t <:< localTypeOf[Seq[_]] =>
        val TypeRef(_, _, Seq(elementType)) = t
        val Schema(dataType, nullable) = schemaFor(elementType, udts)
        Schema(ArrayType(dataType, containsNull = nullable), nullable = true)

      case t if t <:< localTypeOf[Map[_, _]] =>
        val TypeRef(_, _, Seq(keyType, valueType)) = t
        val Schema(valueDataType, valueNullable) = schemaFor(valueType, udts)
        Schema(MapType(schemaFor(keyType, udts).dataType, valueDataType, valueContainsNull = valueNullable), nullable = true)

      case t if t <:< localTypeOf[Product] =>
        val formalTypeArgs = t.typeSymbol.asClass.typeParams
        val TypeRef(_, _, actualTypeArgs) = t
        val constructorSymbol = t.member(nme.CONSTRUCTOR)

        val params = if (constructorSymbol.isMethod) {
          constructorSymbol.asMethod.paramss
        } else {
          // Find the primary constructor, and use its parameter ordering.
          val primaryConstructorSymbol: Option[Symbol] = constructorSymbol.asTerm.alternatives.find(
            s => s.isMethod && s.asMethod.isPrimaryConstructor)
          if (primaryConstructorSymbol.isEmpty) {
            sys.error("Internal SQL error: Product object did not have a primary constructor.")
          } else {
            primaryConstructorSymbol.get.asMethod.paramss
          }
        }

        val fields = params.head
          .map { p =>
            val Schema(dataType, nullable) = schemaFor(p.typeSignature.substituteTypes(formalTypeArgs, actualTypeArgs), udts)
            StructField(p.name.toString, dataType, nullable)
          }

        Schema(StructType(fields), nullable = true)

      case t if t <:< localTypeOf[String] =>
        Schema(StringType, nullable = true)

      case t if t <:< localTypeOf[java.sql.Timestamp] =>
        Schema(TimestampType, nullable = true)

      case t if t <:< localTypeOf[java.sql.Date] =>
        Schema(DateType, nullable = true)

      case t if t <:< localTypeOf[BigDecimal] =>
        Schema(DecimalType.SYSTEM_DEFAULT, nullable = true)

      case t if t <:< localTypeOf[java.math.BigDecimal] =>
        Schema(DecimalType.SYSTEM_DEFAULT, nullable = true)

      case t if t <:< localTypeOf[Decimal] =>
        Schema(DecimalType.SYSTEM_DEFAULT, nullable = true)

      case t if t <:< localTypeOf[java.lang.Integer] =>
        Schema(IntegerType, nullable = true)

      case t if t <:< localTypeOf[java.lang.Long] =>
        Schema(LongType, nullable = true)

      case t if t <:< localTypeOf[java.lang.Double] =>
        Schema(DoubleType, nullable = true)

      case t if t <:< localTypeOf[java.lang.Float] =>
        Schema(FloatType, nullable = true)

      case t if t <:< localTypeOf[java.lang.Short] =>
        Schema(ShortType, nullable = true)

      case t if t <:< localTypeOf[java.lang.Byte] =>
        Schema(ByteType, nullable = true)

      case t if t <:< localTypeOf[java.lang.Boolean] =>
        Schema(BooleanType, nullable = true)

      case t if t <:< definitions.IntTpe =>
        Schema(IntegerType, nullable = false)

      case t if t <:< definitions.LongTpe =>
        Schema(LongType, nullable = false)

      case t if t <:< definitions.DoubleTpe =>
        Schema(DoubleType, nullable = false)

      case t if t <:< definitions.FloatTpe =>
        Schema(FloatType, nullable = false)

      case t if t <:< definitions.ShortTpe =>
        Schema(ShortType, nullable = false)

      case t if t <:< definitions.ByteTpe =>
        Schema(ByteType, nullable = false)

      case t if t <:< definitions.BooleanTpe =>
        Schema(BooleanType, nullable = false)

      case other =>
        // assume the given type is a java type
        val typeToken = getTypeTokenFromClassName(className)
        val beanInfo = Introspector.getBeanInfo(typeToken.getRawType)
        val properties = beanInfo.getPropertyDescriptors.filterNot(_.getName == "class")
        val fields = properties.map { property =>
          val returnType = typeToken.method(property.getReadMethod).getReturnType
          val Schema(dataType, nullable) = schemaFor(classToType(returnType.getRawType), udts)
          new StructField(property.getName, dataType, nullable)
        }
        Schema(StructType(fields), nullable = true)
    }
  }

  def classToType(clazz: Class[_]): Type = mirror.classSymbol(clazz).toType

  def localTypeOf[T: TypeTag]: Type = typeTag[T].in(mirror).tpe

  /**
    * Returns the full class name for a type.
    */
  def getClassNameFromType(tpe: Type): String = tpe.erasure.typeSymbol.asClass.fullName

  /**
    * Returns the type token for a class name.
    */
  def getTypeTokenFromClassName(clazz: String): TypeToken[_] = TypeToken.of(Utils.classForName(clazz))

}