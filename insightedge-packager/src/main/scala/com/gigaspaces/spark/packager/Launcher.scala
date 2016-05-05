package com.gigaspaces.spark.packager

import java.text.SimpleDateFormat
import java.util.Calendar

import com.gigaspaces.spark.packager.Utils._
import org.apache.commons.io.filefilter.{AbstractFileFilter, TrueFileFilter}

/**
  * @author Leonid_Poliakov
  */
object Launcher {
  def main(args: Array[String]) {
    val project = parameter("Project folder" -> "project.directory")
    val version = parameter("Project version" -> "project.version")
    val lastCommitHash = optionalParameter("Last commit hash" -> "last.commit.hash")
    val output = parameter("Output folder" -> "output.exploded.directory")
    val outputFile = parameter("Output file" -> "output.compressed.file")
    val outputPrefix = parameter("Output contents prefix" -> "output.contents.prefix")
    val spark = parameter("Spark distribution" -> "dist.spark")
    val grid = parameter("Xap distribution" -> "dist.xap")
    val zeppelin = parameter("Zeppelin distribution" -> "dist.zeppelin")
    val examples = parameter("Examples jar" -> "dist.examples")
    val resources = s"$project/insightedge-packager/src/main/resources"

    validateHash(lastCommitHash)

    run("Unpacking spark") {
      untgz(spark, output, cutRootFolder = true)
    }

    run("Adding docs to insightedge") {
      copy(s"$project/README.md", s"$output/RELEASE")
      val timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(Calendar.getInstance().getTime)
      val versionInfo = s"Version: $version\nHash: ${lastCommitHash.getOrElse("")}\nTimestamp: $timestamp"
      writeToFile(s"$output/VERSION", versionInfo)
    }

    run("Adding integration libs") {
      copy(s"$project/insightedge-core/target", s"$output/lib", nameFilter(n => n.startsWith("insightedge-core") && !n.contains("test") && !n.contains("sources") && !n.contains("javadoc")))
      copy(s"$project/gigaspaces-scala/target", s"$output/lib", nameFilter(n => n.startsWith("gigaspaces-scala") && !n.contains("test") && !n.contains("sources") && !n.contains("javadoc")))
    }

    run("Adding poms of integration libs") {
      copy(s"$project/pom.xml", s"$output/tools/maven/poms/insightedge-package/pom.xml")
      copy(s"$project/insightedge-core/pom.xml", s"$output/tools/maven/poms/insightedge-core/pom.xml")
      copy(s"$project/gigaspaces-scala/pom.xml", s"$output/tools/maven/poms/gigaspaces-scala/pom.xml")
    }

    run("Adding integration scripts") {
      copy(s"$resources/common-insightedge.sh", s"$output/sbin/common-insightedge.sh")
      copy(s"$resources/insightedge-shell", s"$output/bin/insightedge-shell")
      copy(s"$resources/insightedge-submit", s"$output/bin/insightedge-submit")
      copy(s"$resources/insightedge-class", s"$output/bin/insightedge-class")
      copy(s"$resources/shell-init.scala", s"$output/bin/shell-init.scala")
      copy(s"$resources/insightedge.sh", s"$output/sbin/insightedge.sh")
      copy(s"$resources/insightedge-maven.sh", s"$output/sbin/insightedge-maven.sh")
    }

    run("Adding examples") {
      copy(s"$examples", s"$output/quickstart/insightedge-examples.jar")
    }

    run("Unpacking Gigaspaces datagrid") {
      unzip(grid, s"$output/datagrid", cutRootFolder = true)
    }
    run("Adding Gigaspaces datagrid license key") {
      copy(s"$resources/gslicense.xml", s"$output/datagrid/gslicense.xml")
    }
    run("Removing Gigaspaces datagrid folders") {
      remove(s"$output/datagrid/docs")
      remove(s"$output/datagrid/examples")
      remove(s"$output/datagrid/tools/alert-integration")
      remove(s"$output/datagrid/tools/apache")
      remove(s"$output/datagrid/tools/benchmark")
      remove(s"$output/datagrid/tools/groovy")
      remove(s"$output/datagrid/tools/scala")
      remove(s"$output/datagrid/tools/xap-font.json")
    }
    run("Adding Datagrid scripts") {
      copy(s"$resources/start-datagrid-master.sh", s"$output/sbin/start-datagrid-master.sh")
      copy(s"$resources/start-datagrid-slave.sh", s"$output/sbin/start-datagrid-slave.sh")
      copy(s"$resources/stop-datagrid-master.sh", s"$output/sbin/stop-datagrid-master.sh")
      copy(s"$resources/stop-datagrid-slave.sh", s"$output/sbin/stop-datagrid-slave.sh")
      copy(s"$resources/deploy-datagrid.sh", s"$output/sbin/deploy-datagrid.sh")
      copy(s"$resources/undeploy-datagrid.sh", s"$output/sbin/undeploy-datagrid.sh")
    }

    run("Unpacking Zeppelin") {
      untgz(zeppelin, s"$output/zeppelin", cutRootFolder = true)
    }
    run("Configuring Zeppelin") {
      copy(s"$resources/zeppelin-site.xml", s"$output/zeppelin/conf/zeppelin-site.xml")
      copy(s"$resources/zeppelin-env.sh", s"$output/zeppelin/conf/zeppelin-env.sh")
      remove(s"$output/zeppelin/interpreter/spark/dep")
    }
    run("Adding Zeppelin scripts") {
      copy(s"$resources/start-zeppelin.sh", s"$output/sbin/start-zeppelin.sh")
      copy(s"$resources/stop-zeppelin.sh", s"$output/sbin/stop-zeppelin.sh")
    }
    run("Adding Zeppelin notes") {
      copy(s"$resources/zeppelin", s"$output/zeppelin/notebook")
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
      zip(output, outputFile, outputPrefix)
    }

    run("Clearing temporary output") {
      remove(output)
    }
  }

  def parameter(tuple: (String, String)): String = {
    val (label, key) = tuple
    val value = optionalParameter(tuple)
    require(value.isDefined, s"$key ($label) must be set as environment variable")
    value.get
  }

  def optionalParameter(tuple: (String, String)): Option[String] = {
    val (label, key) = tuple
    val value = Option(System.getProperty(key))
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