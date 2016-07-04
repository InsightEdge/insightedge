package com.gigaspaces.spark.mllib

import org.apache.spark.mllib.util.Saveable

trait MllibImplicits {

  implicit def saveModelToDataGridExtension(model: Saveable): SaveModelToGridExtension = {
    new SaveModelToGridExtension(model)
  }

}