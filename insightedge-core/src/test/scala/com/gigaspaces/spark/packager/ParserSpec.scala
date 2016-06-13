package com.gigaspaces.spark.packager

import com.gigaspaces.spark.packager.exception.ParserException
import com.gigaspaces.spark.packager.model.{Topology, Host}
import org.scalatest._

import scala.util.Failure

/**
  * @author Danylo_Hurin.
  */
class ParserSpec extends FlatSpec with Matchers {

  val HOST_1 = "10.0.0.1"
  val HOST_2 = "10.0.0.2"
  val HOST_3 = "10.0.0.3"

  val parser = new Parser

  "Parser" should "work out correctly if there is only one host" in {
    val hosts = parser.parseHosts(HOST_1).get
    assert(hosts.size == 1, "Invalid hosts count")
    assert(hosts.contains(Host(HOST_1)), "Invalid host")
  }

  "Parser" should "work out correctly if there is more that one host" in {
    val hosts = parser.parseHosts(s"$HOST_1,$HOST_2,$HOST_3").get
    assert(hosts.size == 3, "Invalid hosts count")
    assert(hosts.contains(Host(HOST_1)), s"Couldn't find $HOST_1")
    assert(hosts.contains(Host(HOST_2)), s"Couldn't find $HOST_2")
    assert(hosts.contains(Host(HOST_3)), s"Couldn't find $HOST_3")
  }

  "Parser" should "fail if there is no hosts" in {
    val result = parser.parseHosts("")
    result should matchPattern { case Failure(_) => }
  }

  "Parser" should "fail if ',' is not a delimiter for topology" in {
    val result = parser.parseTopology("2;1")
    result should matchPattern { case Failure(_) => }
  }

  "Parser" should "fail if space instances is not a number" in {
    val result = parser.parseTopology("a,1")
    result should matchPattern { case Failure(_) => }
  }

  "Parser" should "fail if space instances is absent" in {
    val result = parser.parseTopology(",1")
    result should matchPattern { case Failure(_) => }
  }

  "Parser" should "fail if backup space instances is not a number" in {
    val result = parser.parseTopology("2,b")
    result should matchPattern { case Failure(_) => }
  }

  "Parser" should "fail if backup space instances is absent" in {
    val result = parser.parseTopology("2,")
    result should matchPattern { case Failure(_) => }
  }

  "Parser" should "work out correctly" in {
    val topology1_0 = parser.parseTopology("1,0").get
    assert(topology1_0 == Topology(1,0), s"Topology '$topology1_0' parser incorrectly")

    val topology2_0 = parser.parseTopology("2,0").get
    assert(topology2_0 == Topology(2,0), s"Topology '$topology2_0' parser incorrectly")

    val topology2_1 = parser.parseTopology("2,1").get
    assert(topology2_1 == Topology(2,1), s"Topology '$topology2_1' parser incorrectly")
  }


}
