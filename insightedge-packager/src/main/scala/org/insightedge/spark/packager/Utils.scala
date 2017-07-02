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

package org.insightedge.spark.packager

import java.io._

import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.zip.{ZipArchiveEntry, ZipArchiveOutputStream}
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.utils.IOUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FileUtils._
import org.apache.commons.io.filefilter.{AbstractFileFilter, IOFileFilter}
import org.apache.commons.io.filefilter.TrueFileFilter._

import scala.collection.Iterator.continually
import scala.collection.JavaConversions._

/**
  * @author Leonid_Poliakov
  */
object Utils {
  def untgz(from: String, to: String, cutRootFolder: Boolean) {
    val fileStream = new FileInputStream(from)
    val bufferedStream = new BufferedInputStream(fileStream)
    val compressorStream = new GzipCompressorInputStream(bufferedStream)
    unpack(compressorStream, to, "tar", cutRootFolder)
  }

  def unzip(from: String, to: String, cutRootFolder: Boolean) {
    unpack(new FileInputStream(from), to, "zip", cutRootFolder)
  }

  private def unpack(source: InputStream, destination: String, format: String, cutRootFolder: Boolean) {
    val packStream = new ArchiveStreamFactory().createArchiveInputStream(format, source)
    continually(packStream.getNextEntry).takeWhile(_ != null).foreach { entry =>
      val outputPath = if (cutRootFolder) cutOneLevel(entry.getName) else entry.getName
      val outputFile = new File(destination, outputPath)
      if (entry.isDirectory && !outputFile.exists && !outputFile.mkdirs) {
        throw new IOException("Couldn't create directory: " + outputFile.getAbsolutePath)
      }
      if (!entry.isDirectory) {
        val parent = outputFile.getParentFile
        if (!parent.isDirectory && !parent.mkdirs()) {
          throw new IOException("Couldn't create directory: " + parent.getAbsolutePath)
        }
        val outputStream = new FileOutputStream(outputFile)
        IOUtils.copy(packStream, outputStream)
        outputStream.close()
      }
    }
    IOUtils.closeQuietly(packStream)
  }

  def zip(from: String, to: String, prefix: String) {
    val zip = new ZipArchiveOutputStream(new FileOutputStream(to))
    val source = new File(from)
    val sourceFolder = source.getAbsolutePath
    for (file <- listFiles(source, TRUE, TRUE)) {
      if (file.isFile) {
        val entry: ZipArchiveEntry = new ZipArchiveEntry(prefix + file.getAbsolutePath.replace(sourceFolder, ""))
        entry.setUnixMode(getUnixMode(file))
        zip.putArchiveEntry(entry)
        val stream: FileInputStream = new FileInputStream(file)
        IOUtils.copy(stream, zip)
        IOUtils.closeQuietly(stream)
        zip.closeArchiveEntry()
      }
    }
    IOUtils.closeQuietly(zip)
  }

  private def getUnixMode(file: File): Int = {
    var mode = 0
    if (file.canRead) mode |= 292
    if (file.canWrite) mode |= 144
    if (file.canExecute) mode |= 73
    mode
  }

  private def cutOneLevel(relativePath: String): String = {
    var index = relativePath.indexOf("/")
    if (index < 0) index = relativePath.indexOf("\\")
    if (index < 0) relativePath else relativePath.substring(index + 1)
  }

  def copy(from: String, to: String, fileFilter: IOFileFilter) {
    val source = FileUtils.getFile(from)
    val destination = FileUtils.getFile(to)
    if (!source.exists) {
      throw new FileNotFoundException(from)
    }
    if (source.isFile) {
      FileUtils.copyFile(source, destination)
    }
    else {
      FileUtils.copyDirectory(source, destination, fileFilter)
    }
  }

  def copy(from: String, to: String): Unit = {
    copy(from, to, null)
  }

  def remove(path: String) {
    FileUtils.deleteQuietly(new File(path))
  }

  def permissions(path: String, read: Option[Boolean], write: Option[Boolean], execute: Option[Boolean]) {
    permissions(new File(path), TRUE, null, read, write, execute)
  }

  def permissions(path: String, fileFilter: IOFileFilter, dirFilter: IOFileFilter, read: Option[Boolean], write: Option[Boolean], execute: Option[Boolean]) {
    permissions(new File(path), fileFilter, dirFilter, read, write, execute)
  }

  def permissions(file: File, fileFilter: IOFileFilter, dirFilter: IOFileFilter, read: Option[Boolean], write: Option[Boolean], execute: Option[Boolean]) {
    if (!file.exists) {
      throw new FileNotFoundException("Failed to apply permissions: " + file.getAbsolutePath)
    }
    if (file.isDirectory) {
      for (actualFile <- FileUtils.listFiles(file, fileFilter, dirFilter)) {
        permissions(actualFile, fileFilter, dirFilter, read, write, execute)
      }
      return
    }
    if (file.isFile) {
      if (read.isDefined && !file.setReadable(read.get, false)) {
        throw new IOException("Failed to apply read permissions = " + read + " to file: " + file.getAbsolutePath)
      }
      if (write.isDefined && !file.setWritable(write.get, false)) {
        throw new IOException("Failed to apply write permissions = " + read + " to file: " + file.getAbsolutePath)
      }
      if (execute.isDefined && !file.setExecutable(execute.get, false)) {
        throw new IOException("Failed to apply execute permissions = " + read + " to file: " + file.getAbsolutePath)
      }
    }
  }

  def nameFilter(call: String => Boolean): AbstractFileFilter = {
    new AbstractFileFilter {
      override def accept(dir: File, name: String): Boolean = {
        call.apply(name)
      }
    }
  }

  def writeToFile(name: String, str: String): Unit = {
    val pW = new PrintWriter(new File(name))
    pW.append(str)
    pW.close()
  }

  def validateHash(hash: Option[String]): Unit = {
    if (hash.isDefined && hash.get.length == 0) {
      throw new IllegalArgumentException("Invalid commit hash: " + hash.get)
    }
  }

}