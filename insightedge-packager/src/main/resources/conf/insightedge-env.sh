#!/bin/bash
# ***********************************************************************************************************
# * This script is used to initialize common environment to GigaSpaces InsightEdge platform.                *
# * It is highly recommended NOT TO MODIFY THIS SCRIPT, to simplify future upgrades.                        *
# * If you need to override the defaults, please modify $XAP_HOME\bin\setenv-overrides.sh or set           *
# * the XAP_SETTINGS_FILE environment variable to your custom script.                                       *
# * For more information see http://docs.gigaspaces.com/xap/12.2/dev-java/common-environment-variables.html *
# ***********************************************************************************************************
# Source XAP environment:

DIRNAME=$(dirname ${BASH_SOURCE[0]})
source "${DIRNAME}/../../bin/setenv.sh"

# Set InsightEdge defaults:
# Set SPARK_HOME if not set
if [ -z "${SPARK_HOME}" ]; then
    export SPARK_HOME="${XAP_HOME}/insightedge/spark"
fi

export INSIGHTEDGE_CORE_CP="${XAP_HOME}/insightedge/lib/*:${XAP_HOME}/lib/required/*:${XAP_HOME}/lib/optional/spatial/*"

# Spark Submit
if [ -z "$SPARK_SUBMIT_OPTS" ]; then
    export SPARK_SUBMIT_OPTS="-Dspark.driver.extraClassPath=${INSIGHTEDGE_CORE_CP} -Dspark.executor.extraClassPath=${INSIGHTEDGE_CORE_CP}"
fi
# Zeppelin
export ZEPPELIN_INTP_CLASSPATH_OVERRIDES="${INSIGHTEDGE_CORE_CP}"

if [ -z "$INSIGHTEDGE_LOG_DIR" ]; then
  export INSIGHTEDGE_LOG_DIR="${XAP_HOME}/insightedge/logs"
fi

if [ -z "${SPARK_LOCAL_IP}" ]; then
   export SPARK_LOCAL_IP="${XAP_NIC_ADDRESS}"
fi