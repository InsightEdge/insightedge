import org.apache.spark.{SparkConf, SparkContext}
import org.insightedge.spark.context.InsightEdgeConfig
import org.insightedge.spark.implicits.all._
import org.apache.spark.sql.SparkSession

System.setProperty("net.jini.discovery.LookupLocatorDiscovery.level", "OFF")
val settings = Array("spark://127.0.0.1:7077", "insightedge-space", "insightedge", "127.0.0.1:4174")
val Array(master, space, groups, locators) = settings
val ieConfig = InsightEdgeConfig(space, Some(groups), Some(locators))
spark.stop()

val spark = SparkSession.builder.master(master).appName("my init shell").insightEdgeConfig(ieConfig).getOrCreate()
val conf = new SparkConf().setAll(sc.getConf.getAll).setInsightEdgeConfig(ieConfig)
val sc = spark.sparkContext
