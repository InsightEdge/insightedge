import org.apache.spark.{SparkConf, SparkContext}
import org.insightedge.spark.context.InsightEdgeConfig
import org.insightedge.spark.implicits.all._
import org.apache.spark.sql.SparkSession
import org.insightedge.spark.implicits.all._

System.setProperty("net.jini.discovery.LookupLocatorDiscovery.level", "OFF")

val ieConfig = InsightEdgeConfig(sys.env("INSIGHTEDGE_SPACE_NAME"))
val sparkMaster = spark.conf.get("spark.master", "local[*]")
spark.stopInsightEdgeContext()
val spark = SparkSession.builder.master(sparkMaster).appName("my init shell").insightEdgeConfig(ieConfig).getOrCreate()
val sc = spark.sparkContext
import spark.implicits._
