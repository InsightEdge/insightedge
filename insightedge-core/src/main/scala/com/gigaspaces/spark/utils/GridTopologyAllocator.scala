package com.gigaspaces.spark.utils

import scala.collection.mutable
import scala.util.Try

/**
  * Utility used from bash script to map(allocate) primary and backup instances to given hosts.
  * Outputs result with a fixed format to stdout.
  *
  * Exits with 1 if something goes wrong.
  *
  * @author Danylo_Hurin.
  */
object GridTopologyAllocator {

  case class Host(ip: String) {
    override def toString: String = ip
  }

  trait SpaceInstance {
    def partId: Int
  }

  case class PrimaryInstance(override val partId: Int) extends SpaceInstance

  case class BackupInstance(override val partId: Int, backupId: Int) extends SpaceInstance {
    override def toString: String = s"id=$partId,backup_id=$backupId"
  }

  case class Topology(partitionsCount: Int, backupsCount: Int)

  // we use Seq rather than Set to make the output deterministic and testable, perf. doesn't matter here
  type Allocation = mutable.Map[Host, Seq[SpaceInstance]]

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
    val emptyAllocation = hosts.map(h => (h, Seq[SpaceInstance]()))
    val mutableAllocation: Allocation = mutable.Map(emptyAllocation: _*)

    // prepare primaries and backups
    val primaries = (1 to topology.partitionsCount).map(id => PrimaryInstance(id)).toList
    val backups = primaries.flatMap { p =>
      (1 to topology.backupsCount).map(backupId => BackupInstance(p.partId, backupId))
    }

    // allocate
    (primaries ++ backups).foreach { instance =>
      val host = findBestHostFor(instance)
      val currInstances = mutableAllocation(host)
      mutableAllocation(host) = instance +: currInstances
    }

    def findBestHostFor(instance: SpaceInstance): Host = {
      instance match {
        case PrimaryInstance(_) => lessBusyHost(mutableAllocation)
        case BackupInstance(partId, backupId) =>
          val hostsWithoutPart = hostsWithoutPartition(partId)
          if (hostsWithoutPart.nonEmpty) {
            lessBusyHost(hostsWithoutPart)
          } else {
            lessBusyHost(mutableAllocation)
          }
      }
    }

    def hostsWithoutPartition(partId: Int): Allocation = {
      mutableAllocation.filter { case (host, instances) => !instances.map(_.partId).contains(partId) }
    }

    def lessBusyHost(allocation: Allocation): Host = {
      allocation.mapValues(_.size).toList.map { case (host, size) => (size, host) }.sortBy(_._1).map(_._2).head
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

