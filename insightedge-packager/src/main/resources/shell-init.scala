import com.gigaspaces.spark.implicits.all._
import com.gigaspaces.spark.context.GigaSpacesConfig
import org.apache.spark.{SparkContext, SparkConf}

val gsConfig = GigaSpacesConfig("insightedge-space", Some("insightedge"), Some("127.0.0.1:4174"))
val conf = new SparkConf().setAll(sc.getConf.getAll).setGigaSpaceConfig(gsConfig)
sc.stop()
val sc = new SparkContext(conf)
