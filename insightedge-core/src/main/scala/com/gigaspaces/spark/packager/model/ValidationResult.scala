package com.gigaspaces.spark.packager.model

/**
  * @author Danylo_Hurin.
  */
trait ValidationResult
case class Valid() extends ValidationResult
case class Invalid(reason: String) extends ValidationResult
