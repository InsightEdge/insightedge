package com.gigaspaces.spark.utils

import java.io.File
import java.net.{URL, URLClassLoader}
import java.nio.file.Files

import org.scalatest.{BeforeAndAfterEach, FunSpec}

class StringCompilerSpec extends FunSpec with BeforeAndAfterEach{

  var compiler: StringCompiler = _
  var outputPath: String = _

  override protected def beforeEach() = {
    super.beforeEach()
    val classpath = StringCompiler.currentClassPath
    val outputFolder = Files.createTempDirectory("jars").toFile
    outputPath = outputFolder.getAbsolutePath
    compiler = new StringCompiler(outputFolder, classpath.toList)
  }

  it("should compile class") {
    val success = compiler.compile(
      """
        |package generated.code
        |
        |case class MyModel()
      """.stripMargin.trim)
    assert(success)
    assert(new File(outputPath + "/generated/code/MyModel.class").exists())

    val classLoader = new URLClassLoader(Array(new File(outputPath).toURI.toURL), getClass.getClassLoader)
    assert(classLoader.loadClass("generated.code.MyModel") != null)
  }

  it("should pack jar") {
    val success = compiler.compile(
      """
        |package generated.code
        |
        |case class MyModel()
      """.stripMargin.trim)
    assert(success)

    val jar = compiler.packJar()
    assert(jar.isFile)

    val classLoader = new URLClassLoader(Array(jar.toURI.toURL), getClass.getClassLoader)
    assert(classLoader.loadClass("generated.code.MyModel") != null)
  }

  it("should compile and pack two dependent classes") {
    assert(compiler.compile(
      """
        |package generated.code.address
        |
        |case class Address(state:String, city:String) {}
      """.stripMargin))

    assert(compiler.compile(
      """
        |package generated.code.person
        |
        |import generated.code.address.Address
        |
        |case class Person(address:Address, name:String, age:Int) {}
      """.stripMargin))

    val jar = compiler.packJar()
    val classLoader = new URLClassLoader(Array(jar.toURI.toURL), getClass.getClassLoader)
    assert(classLoader.loadClass("generated.code.person.Person") != null)
    assert(classLoader.loadClass("generated.code.address.Address") != null)
  }

  it("should compile and pack two classes in one file") {
    assert(compiler.compile(
      """
        |package generated.code
        |
        |case class Person(address:Address, name:String, age:Int) {}
        |case class Address(state:String, city:String) {}
      """.stripMargin))

    val jar = compiler.packJar()
    val classLoader = new URLClassLoader(Array(jar.toURI.toURL), getClass.getClassLoader)
    assert(classLoader.loadClass("generated.code.Person") != null)
    assert(classLoader.loadClass("generated.code.Address") != null)
  }

  it("should compile and pack space class") {
    assert(compiler.compile(
      """
        |package generated.code
        |
        |import com.gigaspaces.scala.annotation._
        |
        |import scala.beans.BeanProperty
        |
        |case class Person(
        |                   @BeanProperty
        |                   @SpaceId(autoGenerate = true)
        |                   var id: String,
        |
        |                   @BeanProperty
        |                   var name: String,
        |
        |                   @BeanProperty
        |                   var age: Int,
        |
        |                   @BeanProperty
        |                   var address: Address
        |                 ) {
        |
        |  def this() = this(null, null, -1, null)
        |
        |}
        |
        |case class Address(city: String, state: String)
      """.stripMargin))

    val jar = compiler.packJar()
    val classLoader = new URLClassLoader(Array(jar.toURI.toURL), getClass.getClassLoader)
    assert(classLoader.loadClass("generated.code.Person") != null)
    assert(classLoader.loadClass("generated.code.Address") != null)
  }

  it ("should fail to compile with line message") {
    assert(compiler.compile("abc") == false)
    assert(compiler.getAndRemoveMessages() == List("error: line 1: expected class or object definition", "abc", "^"))
    assert(compiler.getAndRemoveMessages() == List())
  }


}
