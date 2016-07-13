package com.gigaspaces.spark.mllib

import com.gigaspaces.spark.context.GigaSpacesSparkContext
import org.apache.spark.mllib.util.Saveable

trait MLlibImplicits {

  implicit class SaveToGridExtension(model: Saveable) {
    def saveToGrid(sc: GigaSpacesSparkContext, name: String) = {
      sc.gigaSpace.write(MLInstance(name, model))
    }
  }

}