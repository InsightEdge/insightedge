#!/usr/bin/env bash

# Starts the Apache Zeppelin on the machine this script is executed on.

if [ -z "${INSIGHTEDGE_HOME}" ]; then
  export INSIGHTEDGE_HOME="$(cd "`dirname "$0"`"/..; pwd)"
fi

. "${INSIGHTEDGE_HOME}/zeppelin/bin/zeppelin-daemon.sh" start
