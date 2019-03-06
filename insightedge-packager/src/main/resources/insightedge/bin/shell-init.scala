import org.insightedge.spark.context.InsightEdgeConfig
import org.insightedge.spark.implicits.all._

System.setProperty("net.jini.discovery.LookupLocatorDiscovery.level", "OFF")

val ieConfig = InsightEdgeConfig(sys.env("INSIGHTEDGE_SPACE_NAME"))
sc.initializeInsightEdgeContext(ieConfig)