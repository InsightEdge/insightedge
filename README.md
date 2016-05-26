# InsightEdge
##### _InsightEdge GigaSpaces convergence platform_
-----------------------------------------

[_Introduction_](#introduction)

[1. Building project](#building-project)

[2. Features](#features)

[3. Documentation](#documentation)

## Introduction

Hybrid transactional/analytical processing platform built on top of Spark and GigaSpaces Data Grid.

## Building project

This project is based on Maven, so to build it you run the next command:

```bash
# without unit tests
mvn clean install -DskipTests=true

# with unit tests
mvn clean install -Dcom.gs.home={PATH_TO_YOUR_XAP_FOLDER}

# package the distribution
mvn clean package -DskipTests=true -P package-deployment -Ddist.spark=<path to spark.tgz> -Ddist.xap=<path to xap.zip> -Ddist.zeppelin=<path to zeppelin.tar.gz>

# run integration tests with Docker (depends on build zip distribution file)
mvn -pl insightedge-integration-tests -P run-integration-tests clean verify
```


## Features
* Exposes Data Grid as Spark RDDs
* Saves Spark RDDs to Data Grid
* Full DataFrames API support with persistence
* Transparent integration with SparkContext using Scala implicits
* Ability to select and filter data from Data Grid with arbitrary SQL and leverage Data Grid indexes
* Running SQL queries in Spark over Data Grid
* Data locality between Spark and Data Grid nodes
* Storing MLlib models in Data Grid
* Saving Spark Streaming computation results in Data Grid
* Off-Heap persistence
* Interactive Web Notebook

## Documentation

Please refer to the [isightedge.io](http://insightedge.io/docs) site.
