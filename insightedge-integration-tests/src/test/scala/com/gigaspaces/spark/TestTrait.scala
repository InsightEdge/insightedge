package com.gigaspaces.spark

import scala.collection.mutable.ArrayBuffer

/**
  * @author Oleksiy_Dyagilev
  */
object TestTrait extends App{

  abstract class IntQueue {
    def get(): Int
    def put(x: Int)
  }

  class BasicIntQueue extends IntQueue {
    private val buf = new ArrayBuffer[Int]
    def get() = buf.remove(0)
    def put(x: Int) { buf += x }
  }

  trait Doubling extends IntQueue {
    abstract override def put(x: Int) { super.put(2 * x) }
  }

  trait Incrementing extends IntQueue {
    abstract override def put(x: Int) { super.put(x + 1) }
  }

  trait Filtering extends IntQueue {
    abstract override def put(x: Int) {
      if (x >= 0) super.put(x)
    }
  }

  trait A {
    val aa = 3
  }

  val cc = new A {
    println(aa)
  }

  val queue = new BasicIntQueue with Incrementing with Filtering

  queue.put(0)
  queue.put(-1)
  println(queue.get())
}
