package org.insightedge.spark.ml

import org.apache.spark.ml.util.MLWritable
import org.apache.spark.mllib.util.Saveable
import org.insightedge.spark.context.InsightEdgeSparkContext
import org.insightedge.spark.mllib.MLInstance

trait MLImplicits {

  implicit class SaveToGridExtension(model: MLWritable) {

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