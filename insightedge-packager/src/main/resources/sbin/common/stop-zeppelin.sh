#!/usr/bin/env bash

# Stops the Apache Zeppelin on the machine this script is executed on.
DIRNAME=$(dirname ${BASH_SOURCE[0]})
source ${DIRNAME}/common-insightedge.sh

. "${XAP_HOME}/insightedge/zeppelin/bin/zeppelin-daemon.sh" stop
