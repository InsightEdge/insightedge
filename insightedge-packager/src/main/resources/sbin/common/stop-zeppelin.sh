#!/usr/bin/env bash

# Stops the Apache Zeppelin on the machine this script is executed on.

if [ -z "${I9E_HOME}" ]; then
  export I9E_HOME="$(cd $(dirname ${BASH_SOURCE[0]})/../..; pwd)"
fi

. "${I9E_HOME}/insightedge/zeppelin/bin/zeppelin-daemon.sh" stop
