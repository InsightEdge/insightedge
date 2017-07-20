#!/usr/bin/env bash

# Starts the Apache Zeppelin on the machine this script is executed on.

if [ -z "${I9E_HOME}" ]; then
  export I9E_HOME="$(cd $(dirname ${BASH_SOURCE[0]})/../..; pwd)"
fi

# add spark, datagrid and InsightEdge JARs to Zeppelin classpath
. ${I9E_HOME}/insightedge/sbin/common-insightedge.sh

BASIC_IE_CLASSPATH="$(get_ie_lib ':')"
XAP_SPATIAL_JARS="$(get_xap_spatial_libs ':')"
XAP_REQUIRED_JARS="$(get_xap_required_jars ':')"

export ZEPPELIN_INTP_CLASSPATH_OVERRIDES="${BASIC_IE_CLASSPATH}:${XAP_SPATIAL_JARS}:${XAP_REQUIRED_JARS}"

. "${I9E_HOME}/insightedge/zeppelin/bin/zeppelin-daemon.sh" start