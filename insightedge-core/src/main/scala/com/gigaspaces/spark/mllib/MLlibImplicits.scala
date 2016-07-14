package com.gigaspaces.spark.mllib

import com.gigaspaces.spark.context.GigaSpacesSparkContext
import org.apache.spark.mllib.util.Saveable

trait MLlibImplicits {

  implicit class SaveToGridExtension(model: Saveable) {
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