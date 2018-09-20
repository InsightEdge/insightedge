package org.insightedge.spark.jobs

import org.apache.spark.{SparkConf, SparkContext}
import org.insightedge.spark.context.InsightEdgeConfig
import org.insightedge.spark.implicits.basic._

object ContinuosLoadRdd {

  def main(args: Array[String]): Unit = {
    val settings = if (args.length > 0) args else Array("spark://127.0.0.1:7077", sys.env("INSIGHTEDGE_SPACE_NAME"))
    if (settings.length != 2) {
      System.err.println("Usage: LoadRdd <spark master url> <space name>")
      System.exit(1)
    }
    val Array(master, space) = settings
    val config = InsightEdgeConfig(space)
    val sc = new SparkContext(new SparkConf().setAppName("example-load-rdd").setMaster(master).setInsightEdgeConfig(config))

    val rdd = sc.gridRdd[Product]()
    println(s"Products RDD count: ${rdd.countByValue()(QuantityOrdering).values.sum}")
    sc.stopInsightEdgeContext()
  }

  object QuantityOrdering extends Ordering[Product]{
    override def compare(x: Product, y: Product): Int = x.quantity compare y.quantity
  }

}

