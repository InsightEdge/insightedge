package com.gigaspaces.spark.packager

import com.gigaspaces.spark.packager.model._

/**
  * @author Danylo_Hurin.
  */
class Validator {

  def validateTopology(topology: Topology): ValidationResult = {
    if (topology.spaceInstancesCount < 1) return Invalid("Space partitions count should be at least one")
    if (topology.backupSpaceInstancesCount < 0) return Invalid("Backup space partitions couldn't be negative value")
    Valid()
  }

  def validateInput(hosts: Set[Host], topology: Topology): ValidationResult = {
    if (hosts.size < 1) return Invalid("Should be at least one host")
    validateTopology(topology) match {
      case Invalid(msg) => return Invalid(msg)
      case Valid() =>
    }
    if (topology.backupSpaceInstancesCount >= topology.spaceInstancesCount) return Invalid("Backup space partitions couldn't be negative value")
    if (topology.spaceInstancesCount > hosts.size) return Invalid("Space partitions count cannot be greater that hosts count")
    if (topology.backupSpaceInstancesCount > (hosts.size - 1)) return Invalid("Backup space partitions count cannot be greater that hosts count minus - 1 ")

    Valid()
  }

}