/*
 * Copyright (c) 2016, GigaSpaces Technologies, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.insightedge.examples.streaming

import java.util
import java.util.Date

import org.apache.spark.SparkConf
import org.apache.spark.streaming.twitter._
import org.apache.spark.streaming.{Minutes, Seconds, StreamingContext}
import org.insightedge.spark.context.InsightEdgeConfig
import org.insightedge.spark.implicits.all._

import scala.collection.JavaConverters._


/**
  * A modified version of Spark's example that saves tags (all and popular) to Data Grid.<br/>
  * To run the example, you have to create application tokens at https://apps.twitter.com/<br/>
  * Make sure you set "Callback URL" to any valid URL, e.g. http://insightedge.io/, otherwise Twitter4j may not work
  *
  * @author Oleksiy_Dyagilev
  */
object TwitterPopularTags {

  def main(args: Array[String]) {
    val initConfig = InsightEdgeConfig.fromSparkConf(new SparkConf())

    //args: <spark master url> <consumer key> <consumer secret> <access token> <access token secret> <space name>
    //or  : <consumer key> <consumer secret> <access token> <access token secret>
    val settings =  if (args.length > 4) args
    else if (args.length == 4) Array( new SparkConf().get("spark.master", InsightEdgeConfig.SPARK_MASTER_LOCAL_URL_DEFAULT),
      args(0), args(1), args(2), args(3), initConfig.spaceName)
    else Array.empty

    if (settings.length != 6) {
      System.err.println("Usage (custom cluster): TwitterPopularTags <spark master url> <consumer key> <consumer secret> <access token> <access token secret> <space name>")
      System.err.println("Usage (default cluster): TwitterPopularTags <consumer key> <consumer secret> <access token> <access token secret>")
      System.exit(1)
    }

    val Array(masterUrl, consumerKey, consumerSecret, accessToken, accessTokenSecret, space) = settings.take(6)

    // Set the system properties so that Twitter4j library used by twitter stream
    // can use them to generate OAuth credentials
    System.setProperty("twitter4j.oauth.consumerKey", consumerKey)
    System.setProperty("twitter4j.oauth.consumerSecret", consumerSecret)
    System.setProperty("twitter4j.oauth.accessToken", accessToken)
    System.setProperty("twitter4j.oauth.accessTokenSecret", accessTokenSecret)

    val ieConfig = initConfig.copy(spaceName = space)
    val sparkConf = new SparkConf().setAppName("TwitterPopularTags").setMaster(masterUrl).setInsightEdgeConfig(ieConfig)

    val ssc = new StreamingContext(sparkConf, Seconds(2))

    val sc = ssc.sparkContext
    val stream = TwitterUtils.createStream(ssc, None)

    val hashTagStrings = stream.flatMap(status => status.getText.split(" ").filter(_.startsWith("#")))
    val hashTags = hashTagStrings.map(tag => new HashTag(tag))

    // saving DStream to Data Grid
    hashTags.saveToGrid()

    val topCounts = hashTagStrings
      .map((_, 1))
      .reduceByKeyAndWindow(_ + _, Seconds(10))
      .map { case (topic, count) => (count, topic) }
      .transform(_.sortByKey(ascending = false))

    topCounts.foreachRDD { rdd =>
      val topList = rdd.take(10)
      val topTags = new TopTags(new util.HashMap(topList.toMap.asJava))
      // saving from driver to Data Grid
      sc.saveToGrid(topTags)
    }

    ssc.start()
    ssc.awaitTerminationOrTimeout(Seconds(30).milliseconds)

    // loads saved tags as RDD, usually it's done in a separate application
    val lastHourTopTags = sc.gridSql[TopTags]("batchTime > ?", Seq(System.currentTimeMillis - Minutes(60).milliseconds))
    println("Popular topics for each 10 seconds (last hour):")
    lastHourTopTags
      .sortBy(_.batchTime, ascending = false)
      .foreach { top => println(s"${new Date(top.batchTime)} - top tag (${maxKey(top.tagsCount)}): ${top.tagsCount.getOrDefault(maxKey(top.tagsCount), "none")}") }
  }

  def maxKey(map: java.util.Map[Int, String]): Int = {
    map.keySet().asScala match {
      case keys if keys.isEmpty => 0
      case keys => keys.max
    }
  }
}