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

package org.apache.spark.storage

import java.nio.ByteBuffer

import com.gigaspaces.query.IdQuery
import com.j_spaces.core.client.SQLQuery
import org.apache.spark.internal.Logging
import org.insightedge.spark.context.InsightEdgeConfig
import org.insightedge.spark.utils.GridProxyFactory
import org.openspaces.core.GigaSpace

/**
  * @author Oleksiy_Dyagilev
  */
private[spark] class InsightEdgeBlockManager
//  extends ExternalBlockManager
//    with Logging
{

//  private val clazz = classOf[GridBlock]
//
//  private var clusteredProxy: GigaSpace = _
//  private var appExecutorId: String = _
//
//  override def init(blockManager: BlockManager, executorId: String): Unit = {
//    super.init(blockManager, executorId)
//    val sparkConf = blockManager.conf
//
//    val ieConfig = InsightEdgeConfig.fromSparkConf(sparkConf)
//    clusteredProxy = GridProxyFactory.getOrCreateClustered(ieConfig)
//
//    val appName = sparkConf.get(ExternalBlockStore.FOLD_NAME)
//
//    appExecutorId = s"$appName/$executorId"
//  }
//
//  override def removeBlock(blockId: BlockId): Boolean = {
//    logInfo("removeBlock " + blockId)
//
//    val query = idQuery(blockId)
//    query.setProjections("")
//
//    clusteredProxy.takeById(query) != null
//  }
//
//  override def putBytes(blockId: BlockId, bytes: ByteBuffer): Unit = {
//    logInfo("putBytes " + blockId)
//    val block = new GridBlock(gridBlockId(blockId), bytes.array(), bytes.array().length)
//    clusteredProxy.write(block)
//  }
//
//  override def shutdown(): Unit = {
//    logInfo("shutdown")
//    val query = new SQLQuery[GridBlock](clazz, "id.appExecutorId = ?")
//    query.setParameters(appExecutorId)
//    query.setProjections("")
//
//    clusteredProxy.takeMultiple(query)
//  }
//
//  override def getSize(blockId: BlockId): Long = {
//    logInfo("getSize " + blockId)
//    val query = idQuery(blockId).setProjections("size")
//    val maybeBlock = Option(clusteredProxy.read(query))
//    maybeBlock.map(_.getSize.toLong).getOrElse(0)
//  }
//
//  override def blockExists(blockId: BlockId): Boolean = {
//    val query = idQuery(blockId)
//    val exists = clusteredProxy.count(query) == 1
//    logInfo("blockExists " + blockId + " " + exists)
//    exists
//  }
//
//  override def getBytes(blockId: BlockId): Option[ByteBuffer] = {
//    logInfo("getBytes " + blockId)
//    val query = idQuery(blockId)
//    Option(clusteredProxy.read(query)).map(block => ByteBuffer.wrap(block.getBytes))
//  }
//
//  override def toString(): String = {
//    "InsightEdge External Block Store"
//  }
//
//  private def gridBlockId(blockId: BlockId): GridBlockId = {
//    new GridBlockId(blockId.name, appExecutorId)
//  }
//
//  private def idQuery(blockId: BlockId): IdQuery[GridBlock] = {
//    new IdQuery[GridBlock](clazz, gridBlockId(blockId))
//  }
}
