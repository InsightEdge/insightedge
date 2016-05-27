#!/usr/bin/env bash

# combines insightedge + datagrid libs into a $1-separated string
# SPARK_JAR=$(get_libs ',')    will give you    /<home>/insightedge-core-<version>.jar,/<home>/gigaspaces-scala-<version>.jar,...
# CLASSPATH=$(get_libs ':')    will give you    /<home>/insightedge-core-<version>.jar:/<home>/gigaspaces-scala-<version>.jar:...
get_libs() {
    local separator=$1

    local datagrid="$INSIGHTEDGE_HOME/datagrid"
    local result="$(find $INSIGHTEDGE_HOME/lib -name "insightedge-core-*.jar")"
    result="$result$separator$(find $INSIGHTEDGE_HOME/lib -name "gigaspaces-scala-*.jar")"
    result="$result$separator$(echo $datagrid/lib/required/*.jar | tr ' ' $separator)"
    echo $result
}

get_relative_libs() {
    local separator=$1
    local prefix=$2

    local result=$(get_libs $separator)
    echo ${result//$INSIGHTEDGE_HOME/$prefix}
}