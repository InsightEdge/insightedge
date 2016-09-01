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

package org.insightedge.spark.utils

import java.io.{FileInputStream, File, FileOutputStream}
import java.net.URLClassLoader
import java.util.jar.{Attributes, JarEntry, JarOutputStream, Manifest}

import org.apache.commons.compress.utils.IOUtils

import scala.collection.mutable
import scala.reflect.internal.util.{BatchSourceFile, Position}
import scala.tools.nsc.reporters.AbstractReporter
import scala.tools.nsc.{Global, Settings}
import scala.util.Try

class StringCompiler(outputFolder: File, classpath: List[String]) {

  private val compilerPath = classPathOf("scala.tools.nsc.Interpreter", "Unable to load Scala interpreter from classpath (scala-compiler jar is missing?)")

  private val libPath = classPathOf("scala.AnyVal", "Unable to load scala base object from classpath (scala-library jar is missing?)")

  private val compilationSettings = {
    if (!outputFolder.isDirectory) {
      throw new IllegalArgumentException(s"$outputFolder is not a directory")
    }
    val settings = new Settings()
    settings.nowarnings.value = true
    settings.outputDirs.setSingleOutput(outputFolder.getAbsolutePath)
    val compilerAndLib = compilerPath ::: libPath
    settings.bootclasspath.value = compilerAndLib.mkString(File.pathSeparator)
    settings.classpath.value = (compilerAndLib ::: classpath).mkString(File.pathSeparator)
    settings
  }

  val compilationCollector = new AbstractReporter {
    val settings = compilationSettings
    val messages = new mutable.ListBuffer[String]

    override def display(pos: Position, message: String, severity: Severity) {
      severity.count += 1
      val severityName = severity match {
        case ERROR => "error: "
        case WARNING => "warning: "
        case _ => ""
      }
      // the line number is not always available
      val lineMessage = Try("line " + pos.line + ": ").getOrElse("")
      messages ++= (severityName + lineMessage + message) ::
        (if (pos.isDefined) {
          pos.inUltimateSource(pos.source).lineContent.stripLineEnd ::
            (" " * (pos.column - 1) + "^") ::
            Nil
        } else {
          Nil
        })
    }

    override def displayPrompt {
      // no-op
    }

    override def reset() {
      super.reset
      messages.clear()
    }
  }

  private val global = new Global(compilationSettings, compilationCollector)

  private def classPathOf(className: String, errorMessage: String = ""): List[String] = {
    try {
      val resource = className.split('.').mkString("/", "/", ".class")
      val path = getClass.getResource(resource).getPath
      if (path.indexOf("file:") >= 0) {
        val indexOfFile = path.indexOf("file:") + 5
        val indexOfSeparator = path.lastIndexOf('!')
        List(path.substring(indexOfFile, indexOfSeparator))
      } else {
        require(path.endsWith(resource))
        List(path.substring(0, path.length - resource.length + 1))
      }
    } catch {
      case up: Throwable => throw new RuntimeException(errorMessage, up)
    }
  }

  def compile(code: String): Boolean = {
    val compiler = new global.Run
    val sourceFiles = List(new BatchSourceFile("(inline)", code))
    compiler.compileSources(sourceFiles)

    !compilationCollector.hasErrors && compilationCollector.WARNING.count == 0
  }

  def getAndRemoveMessages(): List[String] = {
    val messages = compilationCollector.messages.toList
    compilationCollector.reset()
    messages
  }

  def packJar(): File = {
    val name = "generated-" + randomString(5, 'a' to 'z') + ".jar"
    val jarFile = new File(outputFolder.getPath + File.separator + name)
    val manifest = new Manifest()
    manifest.getMainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0")
    val jarStream = new JarOutputStream(new FileOutputStream(jarFile.asInstanceOf[File]), manifest)
    recursiveAddToJar(outputFolder.getAbsolutePath, outputFolder, jarStream)
    jarStream.close()
    jarFile
  }

  private def recursiveAddToJar(root: String, file: File, jarStream: JarOutputStream): Unit = {
    val path = file.getAbsolutePath.replace(root, "").replace("\\", "/")
    val trimmedPath = if (path.startsWith("/")) path.substring(1) else path
    if (file.isDirectory) {

      if (trimmedPath.nonEmpty) {
        val corrected = if (trimmedPath.endsWith("/")) trimmedPath else trimmedPath + "/"
        val entry = new JarEntry(corrected)
        entry.setTime(file.lastModified())
        jarStream.putNextEntry(entry)
        jarStream.closeEntry()
      }
      file.listFiles().foreach(child => recursiveAddToJar(root, child, jarStream))

    } else {

      val entry = new JarEntry(trimmedPath)
      entry.setTime(file.lastModified())
      jarStream.putNextEntry(entry)

      IOUtils.copy(new FileInputStream(file), jarStream)

    }
  }

  private def randomString(length: Int, chars: Seq[Char]): String = {
    val sb = new StringBuilder
    for (i <- 1 to length) {
      val randomNum = util.Random.nextInt(chars.length)
      sb.append(chars(randomNum))
    }
    sb.toString
  }

}

object StringCompiler {
  def currentClassPath: List[String] = {
    val threadClassPath = classPath(Thread.currentThread.getContextClassLoader)
    val appClassPath: Array[String] = sys.props("java.class.path").split(File.pathSeparator)
    threadClassPath ++ appClassPath
  }

  def classPath(classLoader: ClassLoader): List[String] = {
    classLoader match {
      case urlClassLoader: URLClassLoader => urlClassLoader.getURLs.map(url => url.getPath).toList
      case _ => List()
    }
  }
}