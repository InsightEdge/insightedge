package com.gigaspaces.spark.utils

import scala.reflect.ClassTag

/**
  * @author Oleksiy_Dyagilev
  */
object JavaApiHelper {

  def getClassTag[T](clazz: Class[T]): ClassTag[T] = ClassTag(clazz)

}
