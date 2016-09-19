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
mvn clean install

# package the distribution (depends on insightedge-zeppelin, insightedge-examples)
mvn clean package -P package-community -DskipTests=true -Ddist.spark=<path to spark.tgz> -Ddist.xap=<path to xap.zip> -Ddist.zeppelin=<path to zeppelin.tar.gz> -Ddist.examples=<path to examples.zip>
mvn clean package -P package-premium   -DskipTests=true -Ddist.spark=<path to spark.tgz> -Ddist.xap=<path to xap.zip> -Ddist.zeppelin=<path to zeppelin.tar.gz> -Ddist.examples=<path to examples.zip>
# you can also run both profiles at the same time using '-P package-community,package-premium'

# run integration tests with Docker (depends on built zip distribution file; on Mac export DOCKER_HOST=unix:///var/run/docker.sock)
mvn clean verify -P run-integration-tests-community -pl insightedge-integration-tests
mvn clean verify -P run-integration-tests-premium   -pl insightedge-integration-tests
# you cannot run both profiles simultaneously
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
* Interactive Web Notebook

## Documentation

Please refer to the [insightedge.io](http://insightedge.io/docs) site.
