package com.gigaspaces.spark.utils

import scala.reflect.ClassTag
import scala.util.Try

/**
  * Class loading utils.
  *
  * @author Leonid_Poliakov
  * @author Oleksiy_Dyagilev
  */
object ClassLoadUtils {

  def loadClass(fullClassName: String): ClassTag[Any] = {
    loadClass(Thread.currentThread().getContextClassLoader, fullClassName)
      .orElse(loadClass(this.getClass.getClassLoader, fullClassName))
      .getOrElse {
        throw new ClassNotFoundException(fullClassName)
      }
  }

  private def loadClass(classLoader: ClassLoader, fullClassName: String): Try[ClassTag[Any]] = {
    Try {
      ClassTag[Any](classLoader.loadClass(fullClassName))
    }
  }

}
