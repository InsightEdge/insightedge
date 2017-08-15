#!/usr/bin/env bash

# Starts the Apache Zeppelin on the machine this script is executed on.
DIRNAME=$(dirname ${BASH_SOURCE[0]})
source ${DIRNAME}/../conf/insightedge-env.sh

#BASIC_IE_CLASSPATH="$(get_ie_lib ':')"
#XAP_SPATIAL_JARS="$(get_xap_spatial_libs ':')"
#XAP_REQUIRED_JARS="$(get_xap_required_jars ':')"
#
# add spark, datagrid and InsightEdge JARs to Zeppelin classpath
#export ZEPPELIN_INTP_CLASSPATH_OVERRIDES="${BASIC_IE_CLASSPATH}:${XAP_SPATIAL_JARS}:${XAP_REQUIRED_JARS}"

. "${XAP_HOME}/insightedge/zeppelin/bin/zeppelin-daemon.sh" start