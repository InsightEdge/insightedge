#!/usr/bin/env bash

# combines insightedge + datagrid libs into a $1-separated string
# SPARK_JAR=$(get_libs ',')    will give you    /<home>/insightedge-core-<version>.jar,/<home>/gigaspaces-scala-<version>.jar,...
# CLASSPATH=$(get_libs ':')    will give you    /<home>/insightedge-core-<version>.jar:/<home>/gigaspaces-scala-<version>.jar:...
get_libs() {
    local separator=$1

    local datagrid="$INSIGHTEDGE_HOME/datagrid"
    local result=""
    result="$result$separator$(find $INSIGHTEDGE_HOME/lib -name "insightedge-core-*.jar")"
    result="$result$separator$(find $INSIGHTEDGE_HOME/lib -name "gigaspaces-scala-*.jar")"
    result="$result$separator$(echo $datagrid/lib/required/*.jar | tr ' ' $separator)"
    echo $result
}

install_insightedge() {
    local install=$1
    local artifact=$2
    local command=$3
    local home=$4

    if [ $install == "false" ]; then
        return
    fi

    echo ""
    step_title "--- Installing InsightEdge ($artifact) at $home"
    echo "- Cleaning up $home"
    rm -rf $home
    mkdir $home
    cd $home
    echo "- Downloading $artifact"
    eval $command
    echo "- Unpacking $artifact"
    unzip ${artifact}.zip > insightedge-unzip.log
    rm ${artifact}.zip
    echo "- Extracting files from subfolder"
    mv ${artifact}/** .
    echo "- Removing $artifact bundle"
    rm -rf ${artifact}
    step_title "--- Installation complete"
}

local_zeppelin() {
    local home=$1
    local master=$2
    echo ""
    step_title "--- Restarting Zeppelin server"
    stop_zeppelin $1
    start_zeppelin $1
    step_title "--- Zeppelin server can be accessed at http://$master:8090"
}

stop_zeppelin() {
    local home=$1
    step_title "--- Stopping Zeppelin"
    $home/sbin/stop-zeppelin.sh
}

start_zeppelin() {
    local home=$1
    step_title "--- Starting Zeppelin"
    $home/sbin/start-zeppelin.sh
}

start_spark_master() {
    local home=$1
    local master=$2

    echo ""
    step_title "--- Starting Spark master at $master"
    $home/sbin/start-master.sh -h $master
    step_title "--- Spark master started"
}

stop_spark_master() {
    local home=$1

    echo ""
    step_title "--- Stopping Spark master"
    $home/sbin/stop-master.sh
    step_title "--- Spark master stopped"
}

start_spark_slave() {
    local home=$1
    local master=$2

    echo ""
    step_title "--- Starting Spark slave"
    $home/sbin/start-slave.sh spark://$master:7077
    step_title "--- Spark slave started"
}

stop_spark_slave() {
    local home=$1

    echo ""
    step_title "--- Stopping Spark slave"
    $home/sbin/stop-slave.sh
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
    echo "                     |___/                       |___/   version: $VERSION"
    echo "                                                         edition: $EDITION"
}
