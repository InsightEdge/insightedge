import org.insightedge.spark.implicits.all._
import org.insightedge.spark.context.InsightEdgeConfig
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.sql.SQLContext

System.setProperty("net.jini.discovery.LookupLocatorDiscovery.level", "OFF")
val ieConfig = InsightEdgeConfig("insightedge-space", Some("insightedge"), Some("127.0.0.1:4174"))
val conf = new SparkConf().setAll(sc.getConf.getAll).setInsightEdgeConfig(ieConfig)
sc.stop()
val sc = new SparkContext(conf)
val sqlContext = new SQLContext(sc)