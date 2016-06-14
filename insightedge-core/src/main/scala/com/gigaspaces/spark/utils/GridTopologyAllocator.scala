package com.gigaspaces.spark.utils

import scala.annotation.tailrec
import scala.collection.mutable
import scala.util.Try

/**
  * Utility used from bash script to map(allocate) primary and backup instances to given hosts.
  * Outputs result with a fixed format to stdout. E.g. for input
  * "2,1", "hostA,hostB"
  * outputs
  * "hostA:id=1;id=2,backup_id=1 hostB:id=2;id=1,backup_id=1"
  *
  * Exits with 1 if something goes wrong.
  *
  * @author Danylo_Hurin.
  */
object GridTopologyAllocator {

  case class Host(ip: String)

  trait SpaceInstance {
    def partId: Int
  }

  case class PrimaryInstance(override val partId: Int) extends SpaceInstance

  case class BackupInstance(override val partId: Int, backupId: Int) extends SpaceInstance

  case class Topology(partitionsCount: Int, backupsCount: Int)

  // we use LinkedHashMap/Seq to make the output deterministic and testable
  type Allocation = mutable.LinkedHashMap[Host, Seq[SpaceInstance]]

  def main(args: Array[String]) = {
    val output = allocateAndRender(args)
    println(output)
  }

  def allocateAndRender(args: Array[String]): String = {
    if (args.length != 2) {
      System.err.println("Usage: ResolverRunner <topology> <hosts>")
      System.exit(1)
    }
    val Array(topologyStr, hostsStr) = args

    val topology = Try(parseTopology(topologyStr)).getOrElse(throw new IllegalArgumentException(s"Unable to parse topology: $topologyStr"))
    val hosts = Try(parseHosts(hostsStr)).getOrElse(throw new IllegalArgumentException(s"Unable to parse hosts: $hostsStr"))

    val allocation = allocate(topology, hosts)
    allocationToString(allocation)
  }

  def allocate(topology: Topology, hosts: Seq[Host]): Allocation = {

    @tailrec
    def allocateInstances(allocation: Allocation, instancesLeft: List[SpaceInstance], currHostIndex: Int): Allocation = {
      instancesLeft match {
        case Nil => allocation
        case instance :: instancesTail =>
          val host = hosts(currHostIndex)
          val currInstances = allocation(host)
          val updatedInstances = currInstances ++ Seq(instance)
          allocation.put(host, updatedInstances)
          val nextHostIndex = (currHostIndex + 1) % hosts.length
          allocateInstances(allocation, instancesTail, nextHostIndex)
      }
    }

    val initialMap = hosts.map(h => (h, Seq[SpaceInstance]()))
    val mutableAllocation: Allocation = mutable.LinkedHashMap(initialMap: _*)

    // allocate primaries
    val primaries = (1 to topology.partitionsCount).map(id => PrimaryInstance(id)).toList
    allocateInstances(mutableAllocation, primaries, 0)

    // allocate backups
    (1 to topology.backupsCount).foreach { backupId =>
      val backups = primaries.map(p => BackupInstance(p.partId, backupId))
      val currHostIndex = if (hosts.length == 1) 0 else backupId
      allocateInstances(mutableAllocation, backups, currHostIndex)
    }

    mutableAllocation
  }

  def validateTopology(t: Topology): Unit = {
    require(t.partitionsCount >= 1, "Space partitions count should be >= 1")
    require(t.backupsCount >= 0, "Backups count couldn't be negative")
  }

  def parseHosts(hostsStr: String): Seq[Host] = {
    hostsStr.split(",").map(h => Host(h))
  }

  def parseTopology(topologyStr: String): Topology = {
    val split = topologyStr.split(",")
    val Array(partitionsStr, backupsStr) = split
    Topology(partitionsStr.toInt, backupsStr.toInt)
  }

  def allocationToString(allocation: Allocation): String = {
    allocation.map { case (host, instances) =>
      val instancesStr = instances.map {
        case PrimaryInstance(partId) => s"id=$partId"
        case BackupInstance(partId, backupId) => s"id=$partId,backup_id=$backupId"
      }.mkString(";")

      s"$host:$instancesStr"
    }.mkString(" ")
  }

}

