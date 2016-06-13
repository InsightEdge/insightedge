package com.gigaspaces.spark.packager

import com.gigaspaces.spark.packager.exception.ParserException
import com.gigaspaces.spark.packager.model.{Topology, Host}

import scala.util.Try

/**
  * @author Danylo_Hurin.
  */
class Parser {

  def parseHosts(hostsRaw: String): Try[Set[Host]] = {
    Try{
      val hostsArr = hostsRaw.split(",")
      if (hostsArr.length == 1 && hostsArr(0).isEmpty) throw new ParserException("Should be at least one host")
      hostsArr.map(Host).toSet
    }
  }

  def parseTopology(topologyRaw: String): Try[Topology] = {
    Try {
      val rawValues = topologyRaw.split(",")
      if (rawValues.length != 2) throw new IllegalArgumentException("Wrong topology format.")
      Topology(rawValues(0).toInt, rawValues(1).toInt)
    }
  }

}
