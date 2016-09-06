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

import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar

import org.apache.commons.io.filefilter.TrueFileFilter
import org.insightedge.spark.packager.Utils._

/**
  * @author Leonid_Poliakov
  */
object Launcher {

  def main(args: Array[String]) {
    val project = parameter("Project folder" -> "project.directory")
    val version = parameter("Project version" -> "project.version")
    val edition = parameter("Distribution edition" -> "dist.edition")
    val lastCommitHash = optionalParameter("Last commit hash" -> "last.commit.hash")
    val output = parameter("Output folder" -> "output.exploded.directory")
    val outputFile = parameter("Output file" -> "output.compressed.file")
    val outputPrefix = parameter("Output contents prefix" -> "output.contents.prefix")
    val spark = parameter("Spark distribution" -> "dist.spark")
    val grid = parameter("Xap distribution" -> "dist.xap")
    val zeppelin = parameter("Zeppelin distribution" -> "dist.zeppelin")
    val examples = parameter("Examples zip" -> "dist.examples")
    val resources = s"$project/insightedge-packager/src/main/resources"
    val templates = s"datagrid/deploy/templates"

    validateHash(lastCommitHash)

    run("Unpacking spark") {
      untgz(spark, output, cutRootFolder = true)
    }

    run("Adding InsightEdge license and VERSION file") {
      copy(s"$project/LICENSE.md", s"$output/INSIGHTEDGE-LICENSE.md")
      val timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(Calendar.getInstance().getTime)
      val versionInfo = s"Version: $version\nHash: ${lastCommitHash.getOrElse("")}\nTimestamp: $timestamp\nEdition: $edition"
      writeToFile(s"$output/VERSION", versionInfo)
    }

    run("Replacing README.md") {
      remove(s"$output/README.md")
      copy(s"$project/README.md", s"$output/README.md")
    }

    run("Removing CHANGES.txt and RELEASE") {
      remove(s"$output/CHANGES.txt")
      remove(s"$output/RELEASE")
    }

    run("Adding integration libs") {
      copy(s"$project/insightedge-core/target", s"$output/lib", nameFilter(n => n.startsWith("insightedge-core") && !n.contains("test") && !n.contains("sources") && !n.contains("javadoc")))
      copy(s"$project/insightedge-scala/target", s"$output/lib", nameFilter(n => n.startsWith("insightedge-scala") && !n.contains("test") && !n.contains("sources") && !n.contains("javadoc")))
    }

    run("Adding poms of integration libs") {
      copy(s"$project/pom.xml", s"$output/tools/maven/poms/insightedge-package/pom.xml")
      copy(s"$project/insightedge-core/pom.xml", s"$output/tools/maven/poms/insightedge-core/pom.xml")
      copy(s"$project/insightedge-scala/pom.xml", s"$output/tools/maven/poms/insightedge-scala/pom.xml")
    }

    run("Adding integration scripts") {
      copy(s"$resources/bin", s"$output/bin")
      copy(s"$resources/sbin/common/", s"$output/sbin/")
      copy(s"$resources/sbin/$edition/", s"$output/sbin/")
    }

    run("Adding examples") {
      unzip(s"$examples", s"$output/quickstart", cutRootFolder = false)
    }

    run("Adding datasets") {
      copy(s"$resources/data/", s"$output/data")
    }

    run("Unpacking datagrid") {
      unzip(grid, s"$output/datagrid", cutRootFolder = true)
    }
    run("Removing datagrid folders") {
      remove(s"$output/datagrid/docs")
      remove(s"$output/datagrid/examples")
      remove(s"$output/datagrid/tools/alert-integration")
      remove(s"$output/datagrid/tools/apache")
      remove(s"$output/datagrid/tools/benchmark")
      remove(s"$output/datagrid/tools/groovy")
      remove(s"$output/datagrid/tools/scala")
      remove(s"$output/datagrid/tools/xap-font.json")
    }
    if (edition.equals("community")) {
      run("Adding template space configuration (community only)") {
        copy(s"$resources/sbin/community/template/insightedge-datagrid.xml", s"$output/datagrid/deploy/templates/insightedge-datagrid/META-INF/spring/pu.xml")
      }
      run("Adding geospatial jars to pu-common (community only)") {
        copy(s"$output/datagrid/lib/optional/spatial", s"$output/datagrid/lib/optional/pu-common")
      }
    }

    run("Unpacking Zeppelin") {
      untgz(zeppelin, s"$output/zeppelin", cutRootFolder = true)
    }
    run("Configuring Zeppelin") {
      copy(s"$resources/zeppelin/config/zeppelin-site.xml", s"$output/zeppelin/conf/zeppelin-site.xml")
      copy(s"$resources/zeppelin/config/zeppelin-env.sh", s"$output/zeppelin/conf/zeppelin-env.sh")
      copy(s"$resources/zeppelin/config/zeppelin-env.cmd", s"$output/zeppelin/conf/zeppelin-env.cmd")
      remove(s"$output/zeppelin/interpreter/spark/dep")
    }
    run("Adding Zeppelin notes") {
      copy(s"$resources/zeppelin/notes", s"$output/zeppelin/notebook")
    }

    run("Adding Hadoop winutils") {
      unzip(s"$resources/winutils/hadoop-winutils-2.6.0.zip", s"$output/winutils", cutRootFolder = true)
    }

    run("Removing spark R integration") {
      remove(s"$output/R")
      remove(s"$output/bin/sparkR")
      remove(s"$output/bin/sparkR.cmd")
      remove(s"$output/bin/sparkR2.cmd")
    }

    run("Removing Hadoop examples") {
      remove(s"$output/lib/spark-examples-1.6.0-hadoop2.6.0.jar")
    }

    run("Making scripts executable") {
      permissions(s"$output/bin/", read = Some(true), write = Some(true), execute = Some(true))
      permissions(output, fileFilter = nameFilter(n => n.endsWith(".sh") || n.endsWith(".cmd") || n.endsWith(".bat")), dirFilter = TrueFileFilter.INSTANCE, read = Some(true), write = None, execute = Some(true))
    }

    run("Packing installation") {
      new File(outputFile).getParentFile.mkdirs()
      zip(output, outputFile, outputPrefix)
    }
  }

  def parameter(tuple: (String, String)): String = {
    val (label, key) = tuple
    val value = Option(System.getProperty(key))
    require(value.isDefined, s"$key ($label) must be set as environment variable")
    println(s"$label: ${value.get}")
    value.get
  }

  def optionalParameter(tuple: (String, String)): Option[String] = {
    val (label, key) = tuple
    val value = Option(System.getProperty(key)).filter(!_.isEmpty)
    println(s"$label: ${value.getOrElse("")}")
    value
  }

  def run(name: String)(block: => Unit): Unit = {
    println(name + "...")
    val start = System.currentTimeMillis()
    block
    println("\tdone in " + (System.currentTimeMillis() - start) + " ms")
  }

}
