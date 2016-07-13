package com.gigaspaces.spark.ml

import com.gigaspaces.spark.context.GigaSpacesSparkContext
import com.gigaspaces.spark.mllib.MLInstance
import org.apache.spark.ml.util.MLWritable
import org.apache.spark.mllib.util.Saveable

trait MLImplicits {

  implicit class SaveToGridExtension(model: MLWritable) {
    def saveToGrid(sc: GigaSpacesSparkContext, name: String) = {
      sc.gigaSpace.write(MLInstance(name, model))
    }
  }

}