package org.insightedge.spark.mllib

import org.apache.spark.mllib.util.Saveable
import org.insightedge.spark.context.InsightEdgeSparkContext

trait MLlibImplicits {

  implicit class SaveToGridExtension(model: Saveable) {
    /**
      * Save ML instance to the grid. Limited to non-distributed models, i.e. those that can be serialized with Java serialization mechanism.
      *
      * @param sc spark context
      * @param name unique name of the ML instance
      */
    def saveToGrid(sc: InsightEdgeSparkContext, name: String): Unit = {
      sc.grid.write(MLInstance(name, model))
    }
  }

}