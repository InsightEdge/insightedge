package com.gigaspaces.spark.utils

import org.scalatest.FlatSpec

/**
  * @author Oleksiy_Dyagilev
  */
class GridTopologyAllocatorSpec extends FlatSpec {

  it should "allocate 2,0 for 1 host" in {
    val args = Array("2,0", "hostA")
    val expected = "hostA:id=2;id=1"
    assert(GridTopologyAllocator.allocateAndRender(args) == expected)
  }

  it should "allocate 2,1 for 1 hosts" in {
    val args = Array("2,1", "hostA")
//    GridTopologyAllocator.main(args)
    val expected = "hostA:id=2,backup_id=1;id=1,backup_id=1;id=2;id=1"
    assert(GridTopologyAllocator.allocateAndRender(args) == expected)
  }

  it should "allocate 2,2 for 1 hosts" in {
    val args = Array("2,2", "hostA")
//        GridTopologyAllocator.main(args)
    val expected = "hostA:id=2,backup_id=2;id=2,backup_id=1;id=1,backup_id=2;id=1,backup_id=1;id=2;id=1"
    assert(GridTopologyAllocator.allocateAndRender(args) == expected)
  }

  it should "allocate 2,0 for 2 hosts" in {
    val args = Array("2,0", "hostA,hostB")
//            GridTopologyAllocator.main(args)
    val expected = "hostB:id=1 hostA:id=2"
    assert(GridTopologyAllocator.allocateAndRender(args) == expected)
  }

}
