#!/usr/bin/env bash

# Starts the Gigaspaces Datagrid slave on the machine this script is executed on.

if [ -z "${INSIGHTEDGE_HOME}" ]; then
  export INSIGHTEDGE_HOME="$(cd "`dirname "$0"`"/..; pwd)"
fi
if [ -z "$INSIGHTEDGE_LOG_DIR" ]; then
  export INSIGHTEDGE_LOG_DIR="${INSIGHTEDGE_HOME}/logs"
fi
THIS_SCRIPT_NAME=`basename "$0"`
export JSHOMEDIR=""

main() {
    define_defaults
    parse_options $@
    check_options
    redefine_defaults
    check_already_started

    mkdir -p "$INSIGHTEDGE_LOG_DIR"
    log="$INSIGHTEDGE_LOG_DIR/insightedge-datagrid-slave.out"
    echo "Starting datagrid slave (locator: $GRID_LOCATOR, group: $GRID_GROUP, heap: $GSC_SIZE, containers: $GSC_COUNT)"
    export GSC_JAVA_OPTIONS="$GSC_JAVA_OPTIONS -server -Xms$GSC_SIZE -Xmx$GSC_SIZE -XX:+UseG1GC -XX:MaxGCPauseMillis=500 -XX:InitiatingHeapOccupancyPercent=50 -XX:+UseCompressedOops"
    export GSA_JAVA_OPTIONS="$GSA_JAVA_OPTIONS -Dinsightedge.marker=slave"
    export LOOKUPLOCATORS=$GRID_LOCATOR
    export LOOKUPGROUPS=$GRID_GROUP
    nohup $IE_PATH/datagrid/bin/gs-agent.sh gsa.gsc $GSC_COUNT gsa.global.gsm 0 gsa.global.lus 0  > $log 2>&1 &
    echo "Datagrid slave started (log: $log)"
}

display_usage() {
    sleep 3
    echo ""
    echo "Usage: * - required"
    echo " -m, --master    |  * cluster master IP or hostname (required if locator is not specified)"
    echo " -l, --locator   |    lookup locators for the grid components                    | default master:4174"
    echo " -g, --group     |    lookup groups for the grid components                      | default insightedge"
    echo "                 |               usage: if you have several clusters in one LAN,"
    echo "                 |                      it's recommended to have unique group per cluster"
    echo " -s, --size      |    grid container heap size                                   | default 1G"
    echo "                 |             format:  java-style heap size string"
    echo "                 |             example: '1G', '4096M'"
    echo " -c, --container |    (slave modes) number of grid containers to start           | default 2"
    echo ""
    local script="./sbin/$THIS_SCRIPT_NAME"
    echo "Examples:"
    echo "  Start datagrid |  starts 8 containers with 4G heap that will use 10.0.0.1 as cluster master"
    echo ""
    echo " $script -m 10.0.0.1 -s 4G -c 8"
    echo ""
    exit 1
}

define_defaults() {
    # '[]' means 'empty'
    IE_PATH="[]"
    CLUSTER_MASTER="[]"
    GRID_LOCATOR="[]"
    GRID_GROUP="insightedge"
    GSC_SIZE="1G"
    GSC_COUNT="2"
}

parse_options() {
    while [ "$1" != "" ]; do
      case $1 in
        "-m" | "--master")
          shift
          CLUSTER_MASTER=$1
          ;;
        "-l" | "--locator")
          shift
          GRID_LOCATOR=$1
          ;;
        "-g" | "--group")
          shift
          GRID_GROUP=$1
          ;;
        "-s" | "--size")
          shift
          GSC_SIZE=$1
          ;;
        "-c" | "--container")
          shift
          GSC_COUNT=$1
          ;;
        *)
          echo "Unknown option: $1"
          display_usage
          ;;
      esac
      shift
    done
}

check_options() {
    # check required options
    if [ $CLUSTER_MASTER == "[]" ] && [ $GRID_LOCATOR == "[]" ]; then
      echo "Error: --master or --locator must be specified"
      display_usage
    fi
}

redefine_defaults() {
    if [ $GRID_LOCATOR == "[]" ]; then
        GRID_LOCATOR="$CLUSTER_MASTER:4174"
    fi
    if [ $IE_PATH == "[]" ]; then
        IE_PATH="$INSIGHTEDGE_HOME"
    fi
}

check_already_started() {
    pid=`ps aux | grep -v grep | grep insightedge.marker=slave | awk '{print $2}'`
    if [ ! -z $pid ]; then
        echo "Datagrid slave is already running. pid: $pid"
        exit
    fi
}

main "$@"