package com.gigaspaces.spark.mllib

import org.apache.spark.mllib.util.Saveable

/**
  * Enables GigaSpaces Mllib API
  *
  * @author Danylo_Hurin
  */

trait MllibImplicits {

  implicit def saveModelToDataGridExtension(model: Saveable): SaveModelToGridExtension = {
    new SaveModelToGridExtension(model)
  }

}