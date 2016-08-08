package org.insightedge.spark.utils

import org.scalatest.FlatSpec

/**
  * @author Oleksiy_Dyagilev
  */
class GridTopologyAllocatorSpec extends FlatSpec {

  it should "allocate 2,0 for 1 host" in {
    val args = Array("2,0", "hostA")
    val expected = "hostA:id=1;id=2"
    assert(GridTopologyAllocator.allocateAndRender(args) == expected)
  }

  it should "allocate 2,1 for 1 hosts" in {
    val args = Array("2,1", "hostA")
    val expected = "hostA:id=1;id=2;id=1,backup_id=1;id=2,backup_id=1"
    assert(GridTopologyAllocator.allocateAndRender(args) == expected)
  }

  it should "allocate 2,2 for 1 hosts" in {
    val args = Array("2,2", "hostA")
    val expected = "hostA:id=1;id=2;id=1,backup_id=1;id=2,backup_id=1;id=1,backup_id=2;id=2,backup_id=2"
    assert(GridTopologyAllocator.allocateAndRender(args) == expected)
  }

  it should "allocate 2,0 for 2 hosts" in {
    val args = Array("2,0", "hostA,hostB")
    val expected = "hostA:id=1 hostB:id=2"
    assert(GridTopologyAllocator.allocateAndRender(args) == expected)
  }

  it should "allocate 2,1 for 2 hosts" in {
    val args = Array("2,1", "hostA,hostB")
    val expected = "hostA:id=1;id=2,backup_id=1 hostB:id=2;id=1,backup_id=1"
    assert(GridTopologyAllocator.allocateAndRender(args) == expected)
  }

  it should "allocate 3,1 for 3 hosts" in {
    val args = Array("3,1", "hostA,hostB,hostC")
    val expected = "hostA:id=1;id=3,backup_id=1 hostB:id=2;id=1,backup_id=1 hostC:id=3;id=2,backup_id=1"
    assert(GridTopologyAllocator.allocateAndRender(args) == expected)
  }

  it should "allocate 3,2 for 3 hosts" in {
    val args = Array("3,2", "hostA,hostB,hostC")
    val expected = "hostA:id=1;id=3,backup_id=1;id=2,backup_id=2 hostB:id=2;id=1,backup_id=1;id=3,backup_id=2 hostC:id=3;id=2,backup_id=1;id=1,backup_id=2"
    assert(GridTopologyAllocator.allocateAndRender(args) == expected)
  }

  it should "allocate 6,1 for 3 hosts" in {
    val args = Array("6,1", "hostA,hostB,hostC")
    val expected = "hostA:id=1;id=4;id=3,backup_id=1;id=6,backup_id=1 hostB:id=2;id=5;id=1,backup_id=1;id=4,backup_id=1 hostC:id=3;id=6;id=2,backup_id=1;id=5,backup_id=1"
    assert(GridTopologyAllocator.allocateAndRender(args) == expected)
  }

  it should "allocate 2,0 for 1 empty host" in {
    val args = Array("2,0", "")
    val expected = "id=1;id=2"
    assert(GridTopologyAllocator.allocateAndRender(args) == expected)
  }

  it should "allocate 2,0 for 2 hosts where one is empty" in {
    val args = Array("2,0", "hostA,")
    val expected = "hostA:id=1;id=2"
    assert(GridTopologyAllocator.allocateAndRender(args) == expected)
  }

  it should "fail to parse topology abc" in {
    val args = Array("abc", "hostA,hostB,hostC")
    intercept[IllegalArgumentException] {
      GridTopologyAllocator.allocateAndRender(args)
    }
  }

  it should "allocate 2,2 for 2 hosts" in {
    val args = Array("2,2", "hostA,hostB")
    val expected = "hostA:id=1;id=2,backup_id=1;id=1,backup_id=2 hostB:id=2;id=1,backup_id=1;id=2,backup_id=2"
    assert(GridTopologyAllocator.allocateAndRender(args) == expected)
  }

  it should "allocate 2,3 for 2 hosts" in {
    val args = Array("2,3", "hostA,hostB")
    val expected = "hostA:id=1;id=2,backup_id=1;id=1,backup_id=2;id=2,backup_id=3 hostB:id=2;id=1,backup_id=1;id=2,backup_id=2;id=1,backup_id=3"
    assert(GridTopologyAllocator.allocateAndRender(args) == expected)
  }
}
