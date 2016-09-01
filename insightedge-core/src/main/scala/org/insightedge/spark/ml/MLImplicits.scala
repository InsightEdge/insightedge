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