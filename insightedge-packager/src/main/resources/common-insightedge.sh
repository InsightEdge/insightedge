#!/usr/bin/env bash

# combines insightedge + datagrid libs into a $1-separated string
# SPARK_JAR=$(get_libs ',')    will give you    /<home>/insightedge-core-<version>.jar,/<home>/gigaspaces-scala-<version>.jar,...
# CLASSPATH=$(get_libs ':')    will give you    /<home>/insightedge-core-<version>.jar:/<home>/gigaspaces-scala-<version>.jar:...
get_libs() {
    local separator=$1

    local datagrid="$INSIGHTEDGE_HOME/datagrid"
    local result=""
    result="$result$separator$(find $INSIGHTEDGE_HOME/lib -name "insightedge-core-*.jar")"
    result="$result$separator$(find $INSIGHTEDGE_HOME/lib -name "gigaspaces-scala-*.jar")"
    result="$result$separator$(echo $datagrid/lib/required/*.jar | tr ' ' $separator)"
    echo $result
}