/*
 * Copyright (c) 2016, GigaSpaces Technologies, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.insightedge.relation

import java.beans.Introspector

import com.google.common.reflect.TypeToken
import javax.activation.UnsupportedDataTypeException
import org.apache.spark.sql.catalyst.ScalaReflection._
import org.apache.spark.sql.types.{StructType, _}
import org.apache.spark.util.Utils

import scala.collection.Map

/**
  * Converts scala, java or mixed types to dataframe schema.
  *
  * Is extended version of {@link org.apache.spark.sql.catalyst.ScalaReflection}
  */
object SchemaInference {
  val universe: scala.reflect.runtime.universe.type = scala.reflect.runtime.universe
  // Since we are creating a runtime mirror using the class loader of current thread,
  // we need to use def at here. So, every time we call mirror, it is using the
  // class loader of the current thread.
  // SPARK-13640: Synchronize this because universe.runtimeMirror is not thread-safe in Scala 2.10.
  def mirror: universe.Mirror = {
    universe.runtimeMirror(Thread.currentThread().getContextClassLoader)
  }

  import universe._

  /**
    * Used to simplify getting datatype and nullable
    */
  case class Schema(dataType: DataType, nullable: Boolean)

  /**
    * @param clazz          class from which the schema will be inferred
    * @return datatype for a class
    */
  def schemaFor(clazz: Class[_]): Schema = {
    schemaFor(classToType(clazz))
  }

  /**
    * @return datatype for a given root or embedded type
    */
  def schemaFor(tpe: Type): Schema = {
    val className = getClassNameFromType(tpe)

    tpe match {
      case t if t.typeSymbol.annotations.exists(_.tpe =:= typeOf[SQLUserDefinedType]) =>
        val udt = getClassFromType(t).getAnnotation(classOf[SQLUserDefinedType]).udt().newInstance()
        Schema(udt, nullable = true)
      case t if UDTRegistration.exists(getClassNameFromType(t)) =>
        val udt = UDTRegistration.getUDTFor(getClassNameFromType(t)).get.newInstance()
          .asInstanceOf[UserDefinedType[_]]
        Schema(udt, nullable = true)
      case t if t <:< localTypeOf[Option[_]] =>
        val TypeRef(_, _, Seq(optType)) = t
        Schema(schemaFor(optType).dataType, nullable = true)
      case t if t <:< localTypeOf[Array[Byte]] => Schema(BinaryType, nullable = true)
      case t if t <:< localTypeOf[Array[_]] =>
        val TypeRef(_, _, Seq(elementType)) = t
        val Schema(dataType, nullable) = schemaFor(elementType)
        Schema(ArrayType(dataType, containsNull = nullable), nullable = true)
      case t if t <:< localTypeOf[Seq[_]] =>
        val TypeRef(_, _, Seq(elementType)) = t
        val Schema(dataType, nullable) = schemaFor(elementType)
        Schema(ArrayType(dataType, containsNull = nullable), nullable = true)
      case t if t <:< localTypeOf[java.util.List[_]] =>
        val TypeRef(_, _, Seq(elementType)) = t
        val Schema(dataType, nullable) = schemaFor(elementType)
        Schema(ArrayType(dataType, containsNull = nullable), nullable = true)
      case t if t <:< localTypeOf[Map[_, _]] =>
        val TypeRef(_, _, Seq(keyType, valueType)) = t
        val Schema(valueDataType, valueNullable) = schemaFor(valueType)
        Schema(MapType(schemaFor(keyType).dataType,
          valueDataType, valueContainsNull = valueNullable), nullable = true)
      case t if t <:< localTypeOf[String] => Schema(StringType, nullable = true)
      case t if t <:< localTypeOf[java.sql.Timestamp] => Schema(TimestampType, nullable = true)
      case t if t <:< localTypeOf[java.sql.Date] => Schema(DateType, nullable = true)
      case t if t <:< localTypeOf[BigDecimal] => Schema(DecimalType.SYSTEM_DEFAULT, nullable = true)
      case t if t <:< localTypeOf[java.math.BigDecimal] =>
        Schema(DecimalType.SYSTEM_DEFAULT, nullable = true)
      case t if t <:< localTypeOf[java.math.BigInteger] =>
        Schema(DecimalType.BigIntDecimal, nullable = true)
      case t if t <:< localTypeOf[scala.math.BigInt] =>
        Schema(DecimalType.BigIntDecimal, nullable = true)
      case t if t <:< localTypeOf[Decimal] => Schema(DecimalType.SYSTEM_DEFAULT, nullable = true)
      case t if t <:< localTypeOf[java.lang.Integer] => Schema(IntegerType, nullable = true)
      case t if t <:< localTypeOf[java.lang.Long] => Schema(LongType, nullable = true)
      case t if t <:< localTypeOf[java.lang.Double] => Schema(DoubleType, nullable = true)
      case t if t <:< localTypeOf[java.lang.Float] => Schema(FloatType, nullable = true)
      case t if t <:< localTypeOf[java.lang.Short] => Schema(ShortType, nullable = true)
      case t if t <:< localTypeOf[java.lang.Byte] => Schema(ByteType, nullable = true)
      case t if t <:< localTypeOf[java.lang.Boolean] => Schema(BooleanType, nullable = true)
      case t if t <:< localTypeOf[org.apache.spark.sql.Row] => Schema(StructType.defaultConcreteType, nullable = true)
      case t if t <:< definitions.IntTpe => Schema(IntegerType, nullable = false)
      case t if t <:< definitions.LongTpe => Schema(LongType, nullable = false)
      case t if t <:< definitions.DoubleTpe => Schema(DoubleType, nullable = false)
      case t if t <:< definitions.FloatTpe => Schema(FloatType, nullable = false)
      case t if t <:< definitions.ShortTpe => Schema(ShortType, nullable = false)
      case t if t <:< definitions.ByteTpe => Schema(ByteType, nullable = false)
      case t if t <:< definitions.BooleanTpe => Schema(BooleanType, nullable = false)
      case t if definedByConstructorParams(t) =>
        val params = getConstructorParameters(t)
          Schema(StructType(
            params.map { case (fieldName, fieldType) =>
              val Schema(dataType, nullable) = schemaFor(fieldType)
              StructField(fieldName, dataType, nullable)
            }), nullable = true)
      case other =>
        // assume the given type is a java type
        val typeToken = getTypeTokenFromClassName(className)
        val rawType = typeToken.getRawType
        val declaredFields = typeToken.getRawType.getDeclaredFields
        if (rawType.getSuperclass != null && rawType.getSuperclass.getName.equals("java.lang.Enum")) {
          val fields = Array(StructField("name", StringType))
          Schema(StructType(fields), nullable = true)
        } else {
          val beanInfo = Introspector.getBeanInfo(typeToken.getRawType)
          val properties = beanInfo.getPropertyDescriptors.filterNot(_.getName == "class")
          val fields = properties.map { property =>
            val returnType = typeToken.method(property.getReadMethod).getReturnType
            val Schema(dataType, nullable) = schemaFor(classToType(returnType.getRawType))
            StructField(property.getName, dataType, nullable)
          }
          Schema(StructType(fields), nullable = true)
        }
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