#!/bin/bash
# ***********************************************************************************************************
# * This script is used to initialize common environment to GigaSpaces InsightEdge platform.                *
# * It is highly recommended NOT TO MODIFY THIS SCRIPT, to simplify future upgrades.                        *
# * If you need to override the defaults, please modify $GS_HOME\bin\setenv-overrides.sh or set            *
# * the GS_SETTINGS_FILE environment variable to your custom script.                                       *
# * For more information see https://docs.gigaspaces.com/latest/started/common-environment-variables.html     *
# ***********************************************************************************************************
# Source XAP environment:
DIRNAME=$(dirname ${BASH_SOURCE[0]})
source "${DIRNAME}/../../bin/setenv.sh"

export JACKSON_CLASSPATH="${GS_HOME}/tools/jdbc/lib/jackson-databind-2.6.3.jar:${GS_HOME}/tools/jdbc/lib/jackson-core-2.6.3.jar:${GS_HOME}/tools/jdbc/lib/jackson-annotations-2.6.3.jar"
# Set InsightEdge defaults:
export INSIGHTEDGE_CLASSPATH="${GS_HOME}/insightedge/lib/*:${JACKSON_CLASSPATH}:${GS_HOME}/lib/platform/jdbc/*:${GS_HOME}/lib/required/*:${GS_HOME}/lib/optional/spatial/*"

if [ -n "${INSIGHTEDGE_CLASSPATH_EXT}" ]; then
    export INSIGHTEDGE_CLASSPATH="${INSIGHTEDGE_CLASSPATH_EXT}:${INSIGHTEDGE_CLASSPATH}"
fi

# Set SPARK_HOME if not set
if [ -z "${SPARK_HOME}" ]; then
    export SPARK_HOME="${GS_HOME}/insightedge/spark"
fi

#Add InsightEdge dependencies to Spark
if [ -z "${SPARK_DIST_CLASSPATH}" ]; then
    export SPARK_DIST_CLASSPATH="${INSIGHTEDGE_CLASSPATH}"
fi

# Zeppelin
# Spark jars are added to interpreter classpath because of Analytics Xtreme
export ZEPPELIN_INTP_CLASSPATH_OVERRIDES="${INSIGHTEDGE_CLASSPATH}"

if [ -z "${ZEPPELIN_PORT}" ]; then
    export ZEPPELIN_PORT=9090
fi

if [ -z "${ZEPPELIN_LOG_DIR}" ]; then
	export ZEPPELIN_LOG_DIR="${GS_HOME}/logs/"
fi

if [ -z "${INSIGHTEDGE_SPACE_NAME}" ]; then
    export INSIGHTEDGE_SPACE_NAME="demo"
fi

#### PYSPARK_PYTHON is also defined in insightedge-pyspark
# Determine the Python executable to use for the executors:
if [[ -z "$PYSPARK_PYTHON" ]]; then
  if [[ $PYSPARK_DRIVER_PYTHON == *ipython* && ! $WORKS_WITH_IPYTHON ]]; then
    echo "IPython requires Python 2.7+; please install python2.7 or set PYSPARK_PYTHON" 1>&2
    exit 1
  else
    PYSPARK_PYTHON=python
  fi
fi
export PYSPARK_PYTHON

#### PYTHONPATH is also defined in insightedge-pyspark
# Add the PySpark classes to the Python path:
export PYTHONPATH="${SPARK_HOME}/python/:$PYTHONPATH"
export PYTHONPATH="${SPARK_HOME}/python/lib/py4j-0.10.7-src.zip:$PYTHONPATH"

# GS-14003 installing new interpreters fails due to Zeppelin accessing maven via http
if [[ -z "$ZEPPELIN_INTERPRETER_DEP_MVNREPO" ]]; then
    export ZEPPELIN_INTERPRETER_DEP_MVNREPO="https://repo1.maven.org/maven2"
fi
