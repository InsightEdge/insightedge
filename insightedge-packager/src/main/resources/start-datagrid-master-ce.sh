#!/usr/bin/env bash

# Starts the Gigaspaces Datagrid master on the machine this script is executed on.

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
    log="$INSIGHTEDGE_LOG_DIR/insightedge-datagrid-master.out"
    echo "Starting LUS (locator: $GRID_LOCATOR, group: $GRID_GROUP, heap: $GSM_SIZE)"
    export EXT_JAVA_OPTIONS="-Xmx$GSM_SIZE -Dinsightedge.marker=master"
    export LOOKUPLOCATORS=$GRID_LOCATOR
    export LOOKUPGROUPS=$GRID_GROUP
    export NIC_ADDR=$CLUSTER_MASTER
    nohup $IE_PATH/datagrid/bin/startJiniLUS.sh > $log 2>&1 &
    echo "LUS on master started (log: $log)"
}

display_usage() {
    sleep 3
    echo ""
    echo "Usage: * - required"
    echo " -m, --master    |  * cluster master IP or hostname"
    echo " -l, --locator   |    lookup locators for the grid components                    | default master:4174"
    echo " -g, --group     |    lookup groups for the grid components                      | default insightedge"
    echo "                 |               usage: if you have several clusters in one LAN,"
    echo "                 |                      it's recommended to have unique group per cluster"
    echo " -t, --topology  |    number of space primary and backup instances | default 2,0"
    echo "                 |             format:  <num-of-primaries>,<num-of-backups-per-primary>"
    echo "                 |             example: '4,1' will deploy 8 instances - 4 primary and 4 backups"
    echo " -s, --size      |    grid manager heap size                                     | default 1G"
    echo "                 |             format:  java-style heap size string"
    echo "                 |             example: '1G', '4096M'"
    echo ""
    local script="./sbin/$THIS_SCRIPT_NAME"
    echo "Examples:"
    echo "  Start datagrid |  starts grid manager with 2G heap with lookup service at 10.0.0.1:4174"
    echo ""
    echo " $script -m 10.0.0.1 -s 2G"
    echo ""
    exit 1
}

define_defaults() {
    # '[]' means 'empty'
    IE_PATH="[]"
    CLUSTER_MASTER="[]"
    SPACE_NAME="insightedge-space"
    TOPOLOGY="2,0"
    GRID_LOCATOR="[]"
    GRID_GROUP="insightedge"
    GSM_SIZE="1G"
}

parse_options() {
    while [ "$1" != "" ]; do
      case $1 in
        "-m" | "--master")
          shift
          CLUSTER_MASTER=$1
          ;;
        "-n" | "--name")
          shift
          SPACE_NAME=$1
          ;;
        "-l" | "--locator")
          shift
          GRID_LOCATOR=$1
          ;;
        "-g" | "--group")
          shift
          GRID_GROUP=$1
          ;;
        "-t" | "--topology")
          shift
          TOPOLOGY=$1
          ;;
        "-s" | "--size")
          shift
          GSM_SIZE=$1
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
    if [ $CLUSTER_MASTER == "[]" ]; then
      echo "Error: --master must be specified"
      display_usage
    fi
}

redefine_defaults() {
    if [ $GRID_LOCATOR == "[]:4174" ]; then
        GRID_LOCATOR="$CLUSTER_MASTER:4174"
    fi
    if [ $IE_PATH == "[]" ]; then
        IE_PATH="$INSIGHTEDGE_HOME"
    fi
}

check_already_started() {
    pid=`ps aux | grep -v grep | grep insightedge.marker=master | awk '{print $2}'`
    if [ ! -z $pid ]; then
        echo "Datagrid master is already running. pid: $pid"
        exit
    fi
}

main "$@"