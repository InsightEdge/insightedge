package com.gigaspaces.spark.packager

import com.gigaspaces.spark.packager.model.{Topology, Host, BackupSpaceInstance, SpaceInstance}
import org.scalatest._

/**
  * @author Danylo_Hurin.
  */
class InstancesToHostResolverSpec extends FlatSpec {

  val HOST_1 = Host("10.0.0.1")
  val HOST_2 = Host("10.0.0.2")
  val HOST_3 = Host("10.0.0.3")
  val HOST_4 = Host("10.0.0.4")

  val resolver = new InstancesToHostResolver

  "Resolver" should "resolve instances for single host and 1,0 topology" in {
    val expected = Set(
      SpaceInstance(1)
    )
    val result = resolver.resolveForSingleHost(Topology(1, 0))
    assert(expected == result)
  }

  "Resolver" should "resolve instances for single host and 1,1 topology" in {
    val expected = Set(
      SpaceInstance(1), BackupSpaceInstance(1, 1)
    )
    val result = resolver.resolveForSingleHost(Topology(1, 1))
    assert(expected == result)
  }

  "Resolver" should "resolve instances for single host and 2,0 topology" in {
    val expected = Set(
      SpaceInstance(1),
      SpaceInstance(2)
    )
    val result = resolver.resolveForSingleHost(Topology(2, 0))
    assert(expected == result)
  }

  "Resolver" should "resolve instances for single host and 2,1 topology" in {
    val expected = Set(
        SpaceInstance(1), BackupSpaceInstance(1, 1),
        SpaceInstance(2), BackupSpaceInstance(2, 1)
    )
    val result = resolver.resolveForSingleHost(Topology(2, 1))
    assert(expected == result)
  }

  "Resolver" should "resolve instances for single host and 3,0 topology" in {
    val expected = Set(
      SpaceInstance(1),
      SpaceInstance(2),
      SpaceInstance(3)
    )
    val result = resolver.resolveForSingleHost(Topology(3, 0))
    assert(expected == result)
  }

  "Resolver" should "resolve instances for single host and 3,1 topology" in {
    val expected = Set(
      SpaceInstance(1), BackupSpaceInstance(1, 1),
      SpaceInstance(2), BackupSpaceInstance(2, 1),
      SpaceInstance(3), BackupSpaceInstance(3, 1)
    )
    val result = resolver.resolveForSingleHost(Topology(3, 1))
    assert(expected == result)
  }

  "Resolver" should "resolve instances for single host and 3,2 topology" in {
    val expected = Set(
      SpaceInstance(1), BackupSpaceInstance(1, 1), BackupSpaceInstance(1, 2),
      SpaceInstance(2), BackupSpaceInstance(2, 1), BackupSpaceInstance(2, 2),
      SpaceInstance(3), BackupSpaceInstance(3, 1), BackupSpaceInstance(3, 2)
    )
    val result = resolver.resolveForSingleHost(Topology(3, 2))
    assert(expected == result)
  }

  "Resolver" should "resolve instances for single host and 3,3 topology" in {
    val expected = Set(
      SpaceInstance(1), BackupSpaceInstance(1, 1), BackupSpaceInstance(1, 2), BackupSpaceInstance(1, 3),
      SpaceInstance(2), BackupSpaceInstance(2, 1), BackupSpaceInstance(2, 2), BackupSpaceInstance(2, 3),
      SpaceInstance(3), BackupSpaceInstance(3, 1), BackupSpaceInstance(3, 2), BackupSpaceInstance(3, 3)
    )
    val result = resolver.resolveForSingleHost(Topology(3, 3))
    assert(expected == result)
  }

  "Resolver" should "resolve instances for 1 host and 1,0 topology" in {
    val expected = SpaceInstancesToHosts(Map(
      HOST_1 -> Set(SpaceInstance(1)))
    )
    val result = resolver.resolveForMultipleHosts(Set(HOST_1), Topology(1, 0))
    assert(expected == result)
  }

  "Resolver" should "resolve instances for 2 hosts and 2,0 topology" in {
    val expected = SpaceInstancesToHosts(Map(
      HOST_1 -> Set(SpaceInstance(1)),
      HOST_2 -> Set(SpaceInstance(2)))
    )
    val result = resolver.resolveForMultipleHosts(Set(HOST_1, HOST_2), Topology(2, 0))
    assert(expected == result)
  }

  "Resolver" should "resolve instances for 2 hosts and 2,1 topology" in {
    val expected = SpaceInstancesToHosts(Map(
      HOST_1 -> Set(SpaceInstance(1), BackupSpaceInstance(2, 1)),
      HOST_2 -> Set(SpaceInstance(2), BackupSpaceInstance(1, 1)))
    )
    val result = resolver.resolveForMultipleHosts(Set(HOST_1, HOST_2), Topology(2, 1))
    assert(expected == result)
  }

  "Resolver" should "resolve instances for 3 hosts and 3,0 topology" in {
    val expected = SpaceInstancesToHosts(Map(
      HOST_1 -> Set(SpaceInstance(1)),
      HOST_2 -> Set(SpaceInstance(2)),
      HOST_3 -> Set(SpaceInstance(3)))
    )
    val result = resolver.resolveForMultipleHosts(Set(HOST_1, HOST_2, HOST_3), Topology(3, 0))
    assert(expected == result)
  }

  "Resolver" should "resolve instances for 3 hosts and 3,1 topology" in {
    val expected = SpaceInstancesToHosts(Map(
      HOST_1 -> Set(SpaceInstance(1), BackupSpaceInstance(3, 1)),
      HOST_2 -> Set(SpaceInstance(2), BackupSpaceInstance(1, 1)),
      HOST_3 -> Set(SpaceInstance(3), BackupSpaceInstance(2, 1)))
    )
    val result = resolver.resolveForMultipleHosts(Set(HOST_1, HOST_2, HOST_3), Topology(3, 1))
    assert(expected == result)
  }

  "Resolver" should "resolve instances for 3 hosts and 3,2 topology" in {
    val expected = SpaceInstancesToHosts(Map(
      HOST_1 -> Set(SpaceInstance(1), BackupSpaceInstance(3, 1), BackupSpaceInstance(2, 2)),
      HOST_2 -> Set(SpaceInstance(2), BackupSpaceInstance(1, 1), BackupSpaceInstance(3, 2)),
      HOST_3 -> Set(SpaceInstance(3), BackupSpaceInstance(2, 1), BackupSpaceInstance(1, 2)))
    )
    val result = resolver.resolveForMultipleHosts(Set(HOST_1, HOST_2, HOST_3), Topology(3, 2))
    assert(expected == result)
  }

  "Resolver" should "resolve instances for 4 hosts and 4,3 topology" in {
    val expected = SpaceInstancesToHosts(Map(
      HOST_1 -> Set(SpaceInstance(1), BackupSpaceInstance(4, 1), BackupSpaceInstance(3, 2), BackupSpaceInstance(2, 3)),
      HOST_2 -> Set(SpaceInstance(2), BackupSpaceInstance(1, 1), BackupSpaceInstance(4, 2), BackupSpaceInstance(3, 3)),
      HOST_3 -> Set(SpaceInstance(3), BackupSpaceInstance(2, 1), BackupSpaceInstance(1, 2), BackupSpaceInstance(4, 3)),
      HOST_4 -> Set(SpaceInstance(4), BackupSpaceInstance(3, 1), BackupSpaceInstance(2, 2), BackupSpaceInstance(1, 3)))
    )
    val result = resolver.resolveForMultipleHosts(Set(HOST_1, HOST_2, HOST_3, HOST_4), Topology(4, 3))
    assert(expected == result)
  }

}
