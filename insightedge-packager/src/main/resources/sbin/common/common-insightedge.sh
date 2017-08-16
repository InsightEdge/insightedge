#!/usr/bin/env bash
DIRNAME=$(dirname ${BASH_SOURCE[0]})
source ${DIRNAME}/../conf/insightedge-env.sh

local_zeppelin() {
    local zeppelin_host=$1
    echo ""
    step_title "--- Restarting Zeppelin server"
    stop_zeppelin
    start_zeppelin
    step_title "--- Zeppelin server can be accessed at http://${zeppelin_host}:8090"
}

stop_zeppelin() {
    step_title "--- Stopping Zeppelin"
    "${XAP_HOME}/insightedge/zeppelin/bin/zeppelin-daemon.sh" stop
}

start_zeppelin() {
    step_title "--- Starting Zeppelin"
    "${XAP_HOME}/insightedge/zeppelin/bin/zeppelin-daemon.sh" start
}

start_spark_master() {
    local master_hostname=$1
    echo ""
    step_title "--- Starting Spark master at ${master_hostname}"
    ${SPARK_HOME}/sbin/start-master.sh -h ${master_hostname}
    step_title "--- Spark master started"
}

stop_spark_master() {
    echo ""
    step_title "--- Stopping Spark master"
    ${SPARK_HOME}/sbin/stop-master.sh
    step_title "--- Spark master stopped"
}

start_spark_slave() {
    local master=$1

    echo ""
    step_title "--- Starting Spark slave"
    ${SPARK_HOME}/sbin/start-slave.sh spark://${master}:7077
    step_title "--- Spark slave started"
}

stop_spark_slave() {
    echo ""
    step_title "--- Stopping Spark slave"
    ${SPARK_HOME}/sbin/stop-slave.sh
    step_title "--- Spark slave stopped"
}

display_demo_help() {
    local zeppelin_host=$1

    printf '\e[0;34m\n'
    echo "Demo steps:"
    echo "1. make sure steps above were successfully executed"
    echo "2. Open Web Notebook at http://${zeppelin_host}:8090 and run any of the available examples"
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
