package com.gigaspaces.spark.packager

import com.gigaspaces.spark.packager.model.{Valid, Invalid, Topology, Host}
import org.scalatest._
import collection.immutable._

/**
  * @author Danylo_Hurin.
  */
class ValidatorSpec extends FlatSpec with Matchers {

  val validator = new Validator()
  val HOST_1 = Host("10.0.0.1")
  val HOST_2 = Host("10.0.0.2")
  val HOST_3 = Host("10.0.0.3")
  val hosts = Set(HOST_1, HOST_2, HOST_3)

  "Validator" should "return an error if hosts count <= 0" in {
    val res = validator.validateInput(Set(), Topology(2, 1))
    res should matchPattern { case Invalid(_) => }
  }

  "Validator" should "return an error if space instances count <= 0" in {
    val res = validator.validateInput(Set(HOST_1, HOST_2), Topology(0, 1))
    res should matchPattern { case Invalid(_) => }
  }

  "Validator" should "return an error if backup space instances count < 0" in {
    val res = validator.validateInput(Set(HOST_1, HOST_2), Topology(2, -1))
    res should matchPattern { case Invalid(_) => }
  }

  "Validator" should "return an error if space instances count > hosts count" in {
    val res = validator.validateInput(Set(HOST_1, HOST_2), Topology(3, 1))
    res should matchPattern { case Invalid(_) => }
  }

  "Validator" should "return an error if backup space instances count > hosts count minus one" in {
    val res = validator.validateInput(Set(HOST_1, HOST_2), Topology(2, 2))
    res should matchPattern { case Invalid(_) => }
  }

  "Validation" should "be successful if there is no backup space instances" in {
    val res = validator.validateInput(Set(HOST_1, HOST_2), Topology(2, 0))
    res should matchPattern { case Valid() => }
  }

  "Validation" should "be successful if there are at least one backup space instance" in {
    val res = validator.validateInput(Set(HOST_1, HOST_2), Topology(2, 1))
    res should matchPattern { case Valid() => }
  }

  "Validation" should "be unsuccessful if space instance count is the same as backup space instance count" in {
    val res = validator.validateInput(Set(HOST_1, HOST_2), Topology(2, 2))
    res should matchPattern { case Invalid(_) => }
  }

  "Validation " should " be successful if space instance count is bigger than backup space instance count" in {
    val res = validator.validateInput(Set(HOST_1, HOST_2, HOST_3), Topology(3, 2))
    res should matchPattern { case Valid() => }
  }


}
