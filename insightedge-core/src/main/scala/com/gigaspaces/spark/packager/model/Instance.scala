package com.gigaspaces.spark.packager.model

/**
  * @author Danylo_Hurin.
  */
trait Instance
case class SpaceInstance(id: Int) extends Instance {
  override def toString: String = s"id=$id"
}
case class BackupSpaceInstance(val id: Int, backupId: Int) extends Instance {
  override def toString: String = s"id=$id,backup_id=$backupId"
}