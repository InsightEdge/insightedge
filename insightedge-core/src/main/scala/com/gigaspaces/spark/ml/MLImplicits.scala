package com.gigaspaces.spark.ml

import com.gigaspaces.spark.context.GigaSpacesSparkContext
import com.gigaspaces.spark.mllib.MLInstance
import org.apache.spark.ml.util.MLWritable
import org.apache.spark.mllib.util.Saveable

trait MLImplicits {

  implicit class SaveToGridExtension(model: MLWritable) {

    /**
      * Save ML instance to the grid. Limited to non-distributed models, i.e. those that can be serialized with Java serialization mechanism.
      *
      * @param sc spark context
      * @param name unique name of the ML instance
      */
    def saveToGrid(sc: GigaSpacesSparkContext, name: String): Unit = {
      sc.gigaSpace.write(MLInstance(name, model))
    }
  }

}