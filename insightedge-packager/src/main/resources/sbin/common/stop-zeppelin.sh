#!/usr/bin/env bash

# Stops the Apache Zeppelin on the machine this script is executed on.

if [ -z "${XAP_HOME}" ]; then
  export XAP_HOME="$(cd $(dirname ${BASH_SOURCE[0]})/../..; pwd)"
fi

. "${XAP_HOME}/insightedge/zeppelin/bin/zeppelin-daemon.sh" stop
