package com.gigaspaces.spark.mllib

import org.apache.spark.mllib.util.Saveable

object implicits {

  implicit def saveModelToDataGridExtension(model: Saveable): SaveModelToGridExtension = {
    new SaveModelToGridExtension(model)
  }

}
