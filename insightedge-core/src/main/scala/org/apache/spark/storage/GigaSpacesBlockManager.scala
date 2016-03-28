package org.apache.spark.storage

import java.nio.ByteBuffer

import com.gigaspaces.query.IdQuery
import com.gigaspaces.spark.context.GigaSpacesConfig
import com.gigaspaces.spark.utils.GigaSpaceFactory
import com.j_spaces.core.client.SQLQuery
import org.apache.spark.Logging
import org.openspaces.core.GigaSpace

/**
  * @author Oleksiy_Dyagilev
  */
private[spark] class GigaSpacesBlockManager extends ExternalBlockManager with Logging {

  private val clazz = classOf[GridBlock]

  private var clusteredProxy: GigaSpace = _
  private var appExecutorId: String = _

  override def init(blockManager: BlockManager, executorId: String): Unit = {
    super.init(blockManager, executorId)
    val sparkConf = blockManager.conf

    val gsConfig = GigaSpacesConfig.fromSparkConf(sparkConf)
    clusteredProxy = GigaSpaceFactory.getOrCreateClustered(gsConfig)

    val appName = sparkConf.get(ExternalBlockStore.FOLD_NAME)

    appExecutorId = s"$appName/$executorId"
  }

  override def removeBlock(blockId: BlockId): Boolean = {
    logInfo("removeBlock " + blockId)

    val query = idQuery(blockId)
    query.setProjections("")

    clusteredProxy.takeById(query) != null
  }

  override def putBytes(blockId: BlockId, bytes: ByteBuffer): Unit = {
    logInfo("putBytes " + blockId)
    val block = new GridBlock(gridBlockId(blockId), bytes.array(), bytes.array().length)
    clusteredProxy.write(block)
  }

  override def shutdown(): Unit = {
    logInfo("shutdown")
    val query = new SQLQuery[GridBlock](clazz, "id.appExecutorId = ?")
    query.setParameters(appExecutorId)
    query.setProjections("")

    clusteredProxy.takeMultiple(query)
  }

  override def getSize(blockId: BlockId): Long = {
    logInfo("getSize " + blockId)
    val query = idQuery(blockId).setProjections("size")
    val maybeBlock = Option(clusteredProxy.read(query))
    maybeBlock.map(_.getSize.toLong).getOrElse(0)
  }

  override def blockExists(blockId: BlockId): Boolean = {
    val query = idQuery(blockId)
    val exists = clusteredProxy.count(query) == 1
    logInfo("blockExists " + blockId + " " + exists)
    exists
  }

  override def getBytes(blockId: BlockId): Option[ByteBuffer] = {
    logInfo("getBytes " + blockId)
    val query = idQuery(blockId)
    Option(clusteredProxy.read(query)).map(block => ByteBuffer.wrap(block.getBytes))
  }

  override def toString(): String = {
    "GigaSpaces External Block Store"
  }

  private def gridBlockId(blockId: BlockId): GridBlockId = {
    new GridBlockId(blockId.name, appExecutorId)
  }

  private def idQuery(blockId: BlockId): IdQuery[GridBlock] = {
    new IdQuery[GridBlock](clazz, gridBlockId(blockId))
  }
}
