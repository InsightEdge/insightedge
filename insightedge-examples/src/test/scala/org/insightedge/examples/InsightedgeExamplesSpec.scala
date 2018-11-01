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

package org.insightedge.examples

import org.insightedge.examples.basic._
import org.insightedge.examples.geospatial.{LoadDataFrameWithGeospatial, LoadRddWithGeospatial}
import org.insightedge.examples.mllib.SaveAndLoadMLModel
import org.openspaces.core.space.EmbeddedSpaceConfigurer
import org.openspaces.core.{GigaSpace, GigaSpaceConfigurer}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSpec}

class InsightedgeExamplesSpec extends FunSpec with BeforeAndAfterAll with BeforeAndAfterEach {
  val spaceName = "insightedge-examples-space"
  val args = Array("local[2]", spaceName)

  var datagrid: GigaSpace = _

  it("should successfully save RDD to Data Grid") {
    SaveRdd.main(args)
  }

  it("should successfully load RDD from Data Grid") {
    LoadRdd.main(args)
  }

  it("should successfully load RDD from Data Grid with SQL") {
    LoadRddWithSql.main(args)
  }

  it("should successfully load DataFrame from Data Grid") {
    LoadDataFrame.main(args)
  }

  it("should successfully persist DataFrame to Data Grid") {
    PersistDataFrame.main(args)
  }

  it("should successfully save and load MLModel to/from from Data Grid") {
    SaveAndLoadMLModel.main(args)
  }

  it("should successfully load rdd with geospatial SQL") {
    LoadRddWithGeospatial.main(args)
  }

  it("should successfully load dataframe with geospatial SQL") {
    LoadDataFrameWithGeospatial.main(args)
  }

  override protected def beforeAll() = {
    datagrid = new GigaSpaceConfigurer(new EmbeddedSpaceConfigurer("insightedge-examples-space")).create()
  }

}
