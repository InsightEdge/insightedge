package com.gigaspaces.spark.streaming

import org.apache.spark.streaming.dstream.DStream

import scala.reflect.ClassTag

/**
  * Enables GigaSpaces Streaming API
  *
  * @author Oleksiy_Dyagilev
  */
trait StreamingImplicits {

  implicit def saveDStreamToGridExtension[T: ClassTag](dStream: DStream[T]): SaveDStreamToGridExtension[T] = {
    new SaveDStreamToGridExtension(dStream)
  }

}
