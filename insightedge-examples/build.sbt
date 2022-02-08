name := "insightedge-examples"

val insightEdgeVersion = sys.props.getOrElse("insightEdgeVersion", "16.2.0-m13-tue-19")

version := insightEdgeVersion

scalaVersion := "2.11.8"


resolvers += Resolver.mavenLocal
resolvers += "Openspaces Maven Repository" at "http://maven-repository.openspaces.org"

libraryDependencies ++= Seq(
  "org.gigaspaces.insightedge" % "insightedge-core" % insightEdgeVersion % "provided",
  "org.apache.bahir" %% "spark-streaming-twitter" % "2.4.0",
  "org.scalatest" % "scalatest_2.11" % "3.0.3" % "test"
)

test in assembly := {}

assemblyOutputPath in assembly := new File("target/insightedge-examples.jar")

assemblyMergeStrategy in assembly := {
  case PathList("org", "apache", "spark", "unused", "UnusedStubClass.class") => MergeStrategy.first
  case x => (assemblyMergeStrategy in assembly).value(x)
}

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)