package com.gigaspaces.spark.packager

import com.gigaspaces.spark.packager.model._

import scala.collection.immutable.IndexedSeq

/**
  * @author Danylo_Hurin.
  */
class InstancesToHostResolver {

  def resolveForMultipleHosts(hosts: Set[Host], topology: Topology): SpaceInstancesToHosts = {
    val instances = 1 to topology.spaceInstancesCount
    val backups: IndexedSeq[Seq[Int]] = (1 to topology.backupSpaceInstancesCount).map(shift => shiftRange(instances, shift).toSeq)

    val instancesToHosts = instances.zip(hosts).map { i =>
      var backupId = 0
      val backupInstances: Seq[Instance] = backups.map(seq => {
        backupId += 1
        BackupSpaceInstance(seq(i._1 - 1), backupId)
      })
      i._2 -> (SpaceInstance(i._1) +: backupInstances).toSet
    }.toMap

    SpaceInstancesToHosts(instancesToHosts)
  }

  def resolveForSingleHost(topology: Topology): Set[Instance] = {
    val instances = 1 to topology.spaceInstancesCount
    val backups = 1 to topology.backupSpaceInstancesCount

    val instancesToHosts = instances.flatMap { instanceId =>
      val backupInstances = backups.map(backupId => {
        BackupSpaceInstance(instanceId, backupId)
      })
      SpaceInstance(instanceId) +: backupInstances
    }

    instancesToHosts.toSet
  }

  private def shiftRange(range: Iterable[Int], shift: Int): Iterable[Int] = {
    val shifted = new Array[Int](range.size)

    var index = 0
    for (value <- range) {
      val i: Int = shiftedIndex(index, range.size, shift)
      shifted(i) = value
      index += 1
    }
    shifted.toIterable
  }

  private def shiftedIndex(index: Int, max: Int, shift: Int): Int = {
    if ((index + shift) >= max) index + shift - max else index + shift
  }
}

case class SpaceInstancesToHosts(instancesToHosts: Map[Host, Set[Instance]])


//
//def resolveForMultipleHosts(hosts: Set[Host], topology: Topology): SpaceInstancesToHosts = {
//  val partitionsCount = topology.spaceInstancesCount
//  val backupsCount = topology.backupSpaceInstancesCount
//
//  val instances = 1 to partitionsCount
//  //    for (shift <- 1 to backupsCount) {
//  //      val shifted = shiftRange(instances, shift)
//  //      println(shifted)
//  //    }
//
//  val backupsList: IndexedSeq[Seq[Int]] = (1 to backupsCount).map(shift => shiftRange(instances, shift).toSeq)
//  //    backupsList.map(list => {
//  //      val toSeq: Seq[Int] = list.toSeq
//  //      val list1: Iterable[Int] = toSeq
//  //      BackupSpaceInstance(1, toSeq(3))
//  //    })
//  val instancesToHosts = instances.zip(hosts).map { i =>
//  var backup_id = 0
//  val backupInstances: IndexedSeq[BackupSpaceInstance] = backupsList.map(seq => {
//  backup_id += 1
//  BackupSpaceInstance(seq(i._1 - 1), backup_id)
//})
//
//  val instances1: mutable.ArraySeq[Instance] = SpaceInstance(i._1) +: backupInstances.toArray
//  i._2 -> Set[Instance](
//  instances1: _*
//  )
//}.toMap
//
//  SpaceInstancesToHosts(instancesToHosts)
//}
