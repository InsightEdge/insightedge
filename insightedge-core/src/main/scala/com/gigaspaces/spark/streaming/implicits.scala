package com.gigaspaces.spark.streaming

import com.gigaspaces.spark.context.GigaSpacesConfig
import org.apache.spark.streaming.dstream.DStream

import scala.reflect.ClassTag

/**
  * Enables GigaSpaces Streaming API
  *
  * @author Oleksiy_Dyagilev
  */
object implicits {

//  implicit def saveDStreamToGridExtension[T: ClassTag](dStream: DStream[T]): SaveDStreamToGridExtension[T] = {
//    new SaveDStreamToGridExtension(dStream)
//  }

}

trait StreamingImplicits {

  implicit def saveDStreamToGridExtension[T: ClassTag](dStream: DStream[T]): SaveDStreamToGridExtension[T] = {
    new SaveDStreamToGridExtension(dStream)
  }

}
