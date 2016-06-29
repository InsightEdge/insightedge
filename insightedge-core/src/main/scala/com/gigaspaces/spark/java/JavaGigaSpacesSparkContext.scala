package com.gigaspaces.spark.java

import com.gigaspaces.spark.implicits
import com.gigaspaces.spark.model.GridModel
import org.apache.spark.api.java.{JavaRDD, JavaSparkContext}
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

/**
  * @author Oleksiy_Dyagilev
  */
class JavaGigaSpacesSparkContext(javaSparkContext: JavaSparkContext) {

  val gsSparkContext = implicits.gigaSpacesSparkContext(javaSparkContext.sc)

  // TODO: overloaded methods
  def gridRdd[R <: GridModel](gridClass: Class[R]): JavaRDD[R] = {
    implicit val classTag: ClassTag[R] = ClassTag(gridClass)
    val rdd: RDD[R] = gsSparkContext.gridRdd()
    JavaRDD.fromRDD(rdd)
  }

//  def gridRdd[R <: GridModel](gridClass: Class[R], splitCount: Int, ): JavaRDD[R] = {
//    implicit val classTag: ClassTag[R] = ClassTag(gridClass)
//    val rdd: RDD[R] = gsSparkContext.gridRdd()
//    JavaRDD.fromRDD(rdd)
//  }

}
