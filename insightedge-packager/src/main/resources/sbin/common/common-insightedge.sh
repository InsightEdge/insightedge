#!/usr/bin/env bash

DIRNAME=$(dirname ${BASH_SOURCE[0]})
source ${DIRNAME}/../../bin/setenv.sh

if [ -z "$INSIGHTEDGE_LOG_DIR" ]; then
  export INSIGHTEDGE_LOG_DIR="${XAP_HOME}/insightedge/logs"
fi


get_spark_path() {
    echo ${XAP_HOME}/insightedge/spark
}
get_ie_path() {
    echo ${XAP_HOME}/insightedge
}
get_xap_path() {
    echo ${XAP_HOME}
}
# combines insightedge + datagrid libs into a $1-separated string
# SPARK_JAR=$(get_libs ',')    will give you    /<home>/insightedge-core-<version>.jar,/<home>/gigaspaces-scala-<version>.jar,...
# CLASSPATH=$(get_libs ':')    will give you    /<home>/insightedge-core-<version>.jar:/<home>/gigaspaces-scala-<version>.jar:...
get_libs() {
    local separator=$1

    local datagrid="$(get_xap_path)"
    local result="$(find $(get_ie_path)/lib -name "insightedge-core.jar")"
    result="$result$separator$(find $(get_ie_path)/lib -name "insightedge-scala.jar")"
    result="$result$separator$(echo ${datagrid}/lib/required/*.jar | tr ' ' $separator)"
    result="$result$separator$(echo ${datagrid}/lib/optional/spatial/*.jar | tr ' ' $separator)"
    echo $result
}

# split get_libs function for zeppelin interpreter
get_xap_required_jars() {
    local separator=$1
    local datagrid="$(get_xap_path)"
    local result="$result$separator$(echo $datagrid/lib/required/*.jar | tr ' ' $separator)"
    echo $result
}

get_xap_spatial_libs() {
    local separator=$1
    local datagrid="$(get_xap_path)"
    local result="$result$separator$(echo $datagrid/lib/optional/spatial/*.jar | tr ' ' $separator)"
    echo $result
}

get_spark_basic_jars() {
    local separator=$1
    local spark_jars="$(get_spark_path)/jars"
    local result="$result$separator$(echo $spark_jars/*.jar | tr ' ' $separator)"
    echo $result
}

get_ie_lib() {
    local separator=$1
    local ie_lib="$(get_ie_path)/lib"
    local result="$result$separator$(echo $ie_lib/*.jar | tr ' ' $separator)"
    echo $result
}

local_zeppelin() {
    local master=$1
    echo ""
    step_title "--- Restarting Zeppelin server"
    stop_zeppelin
    start_zeppelin
    step_title "--- Zeppelin server can be accessed at http://${XAP_NIC_ADDRESS}:8090"
}

stop_zeppelin() {
    step_title "--- Stopping Zeppelin"
    ${XAP_HOME}/insightedge/sbin/stop-zeppelin.sh
}

start_zeppelin() {
    step_title "--- Starting Zeppelin"
    ${XAP_HOME}/insightedge/sbin/start-zeppelin.sh
}

start_spark_master() {
    echo ""
    local master_hostname=${XAP_NIC_ADDRESS}
    step_title "--- Starting Spark master at ${master_hostname}"
    ${XAP_HOME}/insightedge/spark/sbin/start-master.sh -h ${master_hostname}
    step_title "--- Spark master started"
}

stop_spark_master() {
    echo ""
    step_title "--- Stopping Spark master"
    ${XAP_HOME}/insightedge/spark/sbin/stop-master.sh
    step_title "--- Spark master stopped"
}

start_spark_slave() {
    local master=$1

    echo ""
    step_title "--- Starting Spark slave"
    ${XAP_HOME}/insightedge/spark/sbin/start-slave.sh -h ${XAP_NIC_ADDRESS} spark://$master #TODO how to provide multiple spark master urls
    step_title "--- Spark slave started"
}

stop_spark_slave() {
    echo ""
    step_title "--- Stopping Spark slave"
    ${XAP_HOME}/insightedge/spark/sbin/stop-slave.sh
    step_title "--- Spark slave stopped"
}

display_demo_help() {
    local master=$1

    printf '\e[0;34m\n'
    echo "Demo steps:"
    echo "1. make sure steps above were successfully executed"
    echo "2. Open Web Notebook at http://$master:8090 and run any of the available examples"
    printf "\e[0m\n"
}

step_title() {
    printf "\e[32m$1\e[0m\n"
}

error_line() {
    printf "\e[31mError: $1\e[0m\n"
}

display_logo() {
    echo "   _____           _       _     _   ______    _            "
    echo "  |_   _|         (_)     | |   | | |  ____|  | |           "
    echo "    | |  _ __  ___ _  __ _| |__ | |_| |__   __| | __ _  ___ "
    echo "    | | | '_ \\/ __| |/ _\` | '_ \\| __|  __| / _\` |/ _\` |/ _ \\"
    echo "   _| |_| | | \\__ \\ | (_| | | | | |_| |___| (_| | (_| |  __/"
    echo "  |_____|_| |_|___/_|\\__, |_| |_|\\__|______\\__,_|\\__, |\\___|"
    echo "                      __/ |                       __/ |     "
    echo "                     |___/                       |___/   version: $IE_VERSION"
    echo "                                                         edition: $EDITION"
}
