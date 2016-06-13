package com.gigaspaces.spark.packager

import com.gigaspaces.spark.packager.model._

import scala.language.implicitConversions
import scala.util.Try

/**
  * @author Danylo_Hurin.
  */
object ResolverRunner {

  val MULTIPLE="multiple"
  val SINGLE="single"

  val resolver = new InstancesToHostResolver
  val validator = new Validator
  val parser = new Parser

  def main(args: Array[String]) = {
    try {
      if (args.length != 3) {
        Writer.writeErrorAndExit("TODO add args description")
      }

      val hostsRaw = args(0)
      val topologyRaw = args(1)
      val resolveFor = args(2)

      resolveFor match {
        case MULTIPLE => resolveForMultiple(hostsRaw, topologyRaw)
        case SINGLE => resolveForSingle(topologyRaw)
        case unknown: String => Writer.writeErrorAndExit(s"Unknown parameter $unknown. Possible values: $MULTIPLE, $SINGLE")
      }
    } catch {
      case t: Throwable => Writer.writeErrorAndExit(t)
    }
  }

  def resolveForMultiple(hostsRaw: String, topologyRaw: String): Unit = {
    val topology: Topology = parser.parseTopology(topologyRaw)
    val hosts: Set[Host] = parser.parseHosts(hostsRaw)

    validator.validateInput(hosts, topology) match {
      case Invalid(msg) => Writer.writeErrorAndExit(msg);
      case Valid() =>
    }

    val instancesToHosts = resolver.resolveForMultipleHosts(hosts, topology)
    Writer.writeToConsole(instancesToHosts)
  }

  def resolveForSingle(topologyRaw: String): Unit = {
    val topology: Topology = parser.parseTopology(topologyRaw)

    validator.validateTopology(topology) match {
      case Invalid(msg) => Writer.writeErrorAndExit(msg);
      case Valid() =>
    }

    val instances = resolver.resolveForSingleHost(topology)
    Writer.writeToConsole(instances)
  }

  implicit def try2Topology(t: Try[Topology]): Topology = {
    if (t.isFailure) {
      Writer.writeErrorAndExit(s"Could not parse topology. Right format: X,Y " +
        s"where \n X - number of main partitions\n Y - number of backup partitions.\n " +
        "Error:", t.failed.get)
    }
    t.get
  }

  implicit def try2Host(t: Try[Set[Host]]): Set[Host] = {
    if (t.isFailure) {
      Writer.writeErrorAndExit(s"Could not parse hosts. Error:", t.failed.get)
    }
    t.get
  }

}

