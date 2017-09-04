package org.insightedge.spark.jobs

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}
import org.insightedge.spark.context.InsightEdgeConfig
import org.insightedge.spark.implicits.all._

import scala.collection.mutable
import scala.util.Random

/**
  * Created by kobikis on 23/11/16.
  *
  * @since 12.0.1
  */
object StreamExample {

  def main(args: Array[String]): Unit = {
    val settings = if (args.length > 0) args else Array("spark://127.0.0.1:7077", "insightedge-space", "xap-12.2.0", "127.0.0.1:4174")
    if (settings.length < 4) {
      System.err.println("Usage: SaveRdd <spark master url> <space name> <space groups> <space locator>")
      System.exit(1)
    }

    val Array(master, space, groups, locators) = settings
    val ieConfig = InsightEdgeConfig(space, Some(groups), Some(locators))
    val sparkConf = new SparkConf().setAppName("StreamExample").setMaster(master).setInsightEdgeConfig(ieConfig)

    val ssc = new StreamingContext(sparkConf, Seconds(1))

    val sc = ssc.sparkContext
    val sqlContext = new SQLContext(sc)
    val df = sqlContext.read.grid[Data]
    df.printSchema()


    val stream: InputDStream[String] = ssc.queueStream(stringQueue(sc))

    stream.foreachRDD { rdd =>
      val str = rdd.first()
      val data = new Data(Random.nextLong(), str)
      sc.saveToGrid(data)
      val count = df.count()
    }

    ssc.start()

    Thread.sleep(120000)

   sc.stopInsightEdgeContext()
  }


  def stringQueue(sc: SparkContext) = {
    val q = mutable.Queue[RDD[String]]()
    implicit class Rep(n: Int) {
      def times[A](f: => A) { 1 to n foreach(_ => f) }
    }

    100000.times {
      q += sc.makeRDD(Seq("aa", "bb", "cc"))
    }
    q
  }
}
