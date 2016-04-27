#!/usr/bin/env bash

# Starts the Apache Zeppelin on the machine this script is executed on.

if [ -z "${INSIGHTEDGE_HOME}" ]; then
  export INSIGHTEDGE_HOME="$(cd "`dirname "$0"`"/..; pwd)"
fi

# add spark, datagrid and InsightEdge JARs to Zeppelin classpath
. ${INSIGHTEDGE_HOME}/sbin/common-insightedge.sh
export CLASSPATH="$(get_libs ':'):$(find ${INSIGHTEDGE_HOME}/lib -name "spark-assembly-*.jar")"

. "${INSIGHTEDGE_HOME}/zeppelin/bin/zeppelin-daemon.sh" start