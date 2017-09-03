# InsightEdge

**Documentation:** [User Guide](http://insightedge.io/docs/010/index.html)<br/>
**Community:** [Slack Channel](http://insightedge-slack.herokuapp.com/), [StackOverflow tag](http://stackoverflow.com/questions/tagged/insightedge), [Email](mailto:hello@insightedge.io)<br/>
**Contributing:** [Contribution Guide](https://github.com/InsightEdge/insightedge/blob/branch-1.0/CONTRIBUTING.md)<br/>
**Issue Tracker:** [Jira](https://insightedge.atlassian.net)<br/>
**License:** [Apache 2.0](https://github.com/InsightEdge/insightedge/blob/master/LICENSE.md)


**InsightEdge** is a Spark distribution on top of in-memory [Data Grid](https://github.com/InsightEdge/insightedge-datagrid). A single platform for analytical and transactional workloads.

## Features
* Exposes Data Grid as Spark RDDs
* Saves Spark RDDs to Data Grid
* Full DataFrames and Dataset API support with persistence
* Geospatial API for RDD and DataFrames. Geospatial indexes.
* Transparent integration with SparkContext using Scala implicits
* Data Grid side filtering with ability apply indexes
* Running SQL queries in Spark over Data Grid
* Data locality between Spark and Data Grid nodes
* Storing MLlib models in Data Grid
* Continuously saving Spark Streaming computation to Data Grid
* Off-Heap persistence
* Interactive Web Notebook
* Python support

## Building InsightEdge

InsightEdge is built using [Apache Maven](https://maven.apache.org/). 

First, compile and install InsightEdge Core libraries:

```bash
# without unit tests
mvn clean install -DskipTests=true

# with unit tests
mvn clean install
```

To build InsightEdge zip distribution you need the following binary dependencies:

* [insightedge-datagrid 12.1.1](https://xap.github.io/): download a copy of the XAP 12.x Open Source Edition
* [insightedge-examples](https://github.com/InsightEdge/insightedge-examples): use the same branch as in this repo, find build instructions in repository readme
* [insightedge-zeppelin](https://github.com/InsightEdge/insightedge-zeppelin): use the same branch as in this repo, run `./dev/change_scala_version.sh 2.11`, then build with `mvn clean install -DskipTests -P spark-2.1 -P scala-2.11 -P build-distr -Dspark.version=2.1.1`
* [Apache Spark 2.1.1](http://spark.apache.org/downloads.html): download zip

Package InsightEdge distribution:

```bash
mvn clean package -P package-community -DskipTests=true -Ddist.spark=<path to spark.tgz> -Ddist.xap=file:///<path to xap.zip> -Ddist.zeppelin=<path to zeppelin.tar.gz> -Ddist.examples.target=<path to examples target>
```

The archive is generated under `insightedge-packager/target/community` directory. The archive content is under `insightedge-packager/target/contents-community`.

To run integration tests refer to the [wiki page](https://github.com/InsightEdge/insightedge/wiki/Integration-tests)

## Quick Start

Build the project and start InsightEdge demo mode with 
```bash
cd insightedge-packager/target/contents-community
./sbin/insightedge.sh --mode demo
```

It starts Zeppelin at http://127.0.0.1:8090 with InsightEdge tutorial and example notebooks you can play with. The full documentation is available at [website](http://insightedge.io/docs/010/index.html).
