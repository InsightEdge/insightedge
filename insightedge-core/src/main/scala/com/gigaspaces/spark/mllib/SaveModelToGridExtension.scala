package com.gigaspaces.spark.mllib

import com.gigaspaces.spark.context.GigaSpacesSparkContext
import org.apache.spark.mllib.util.Saveable

/**
  * Extra functions available on MLlib model through an implicit conversion.
  */
class SaveModelToGridExtension(model: Saveable) {

  def saveToGrid(sc: GigaSpacesSparkContext, name: String) = {
    sc.gigaSpace.write(MLModel(name, model))
  }

}
