import org.apache.spark.{SparkConf, SparkContext}
import org.insightedge.spark.context.InsightEdgeConfig
import org.insightedge.spark.implicits.all._
import org.apache.spark.sql.SparkSession
import org.insightedge.spark.implicits.all._

System.setProperty("net.jini.discovery.LookupLocatorDiscovery.level", "OFF")
val settings = Array("local[*]", "insightedge-space", "insightedge", "127.0.0.1:4174")
val Array(master, space, groups, locators) = settings
val ieConfig = InsightEdgeConfig(space, Some(groups), Some(locators))
spark.stopInsightEdgeContext()

val spark = SparkSession.builder.master(master).appName("my init shell").insightEdgeConfig(ieConfig).getOrCreate()
val sc = spark.sparkContext
import spark.implicits._
