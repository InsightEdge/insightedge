#!/usr/bin/env bash

# Starts the Gigaspaces Datagrid slave on the machine this script is executed on.

if [ -z "${INSIGHTEDGE_HOME}" ]; then
  export INSIGHTEDGE_HOME="$(cd "`dirname "$0"`"/..; pwd)"
fi
if [ -z "$INSIGHTEDGE_LOG_DIR" ]; then
  export INSIGHTEDGE_LOG_DIR="${INSIGHTEDGE_HOME}/logs"
fi
THIS_SCRIPT_NAME=`basename "$0"`
export XAP_HOME=${INSIGHTEDGE_HOME}/datagrid

main() {
    define_defaults
    parse_options $@
    check_options
    redefine_defaults
    check_already_started

    mkdir -p "$INSIGHTEDGE_LOG_DIR"
    log_template="$INSIGHTEDGE_LOG_DIR/insightedge-datagrid-slave"
    echo "Starting datagrid instances (locator: $GRID_LOCATOR, group: $GRID_GROUP, heap: $GSC_SIZE, space name: $SPACE_NAME, topology: $TOPOLOGY, instances: $INSTANCES)"
    export EXT_JAVA_OPTIONS="-server -Xms$GSC_SIZE -Xmx$GSC_SIZE -XX:+UseG1GC -XX:MaxGCPauseMillis=500 -XX:InitiatingHeapOccupancyPercent=50 -XX:+UseCompressedOops -Dinsightedge.marker=slave"
    export XAP_LOOKUP_LOCATORS=$GRID_LOCATOR
    export XAP_LOOKUP_GROUPS=$GRID_GROUP
    for parsed_instance in ${INSTANCES//;/ }; do
        log_file="$log_template-${parsed_instance//,/_}.log"
        instance=${parsed_instance//,/ }
        `nohup   $IE_PATH/datagrid/bin/pu-instance.sh \
                    -cluster schema=partitioned-sync2backup total_members=$TOPOLOGY $instance \
                    -properties space embed://dataGridName=$SPACE_NAME \
                    $IE_PATH/datagrid/deploy/templates/insightedge-datagrid > $log_file 2>&1 &`
        sleep 3
        echo "Datagrid instance started (log: $log_file)"
    done
    echo "All datagrid instances started"
}

display_usage() {
    sleep 3
    echo ""
    echo "Usage: * - required"
    echo " -m, --master    |  * cluster master IP or hostname (required if locator is not specified)"
    echo " -n, --name      |    space name                                                 | default insightedge-space"
    echo " -l, --locator   |    lookup locators for the grid components                    | default master:4174"
    echo " -g, --group     |    lookup groups for the grid components                      | default insightedge"
    echo "                 |             usage: if you have several clusters in one LAN,"
    echo "                 |                    it's recommended to have unique group per cluster"
    echo " -s, --size      |    grid container heap size                                   | default 1G"
    echo "                 |             format:  java-style heap size string"
    echo "                 |             example: '1G', '4096M'"
    echo " -t, --topology  |  * number of space primary and backup instances"
    echo " -i, --instances |  * space instances to start"
    echo "                 |             example: id=1;id=2,backup_id=1;id=2;id=1,backup_id=1"
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
    EMPTY="[]"
    IE_PATH=$EMPTY
    CLUSTER_MASTER=$EMPTY
    SPACE_NAME="insightedge-space"
    TOPOLOGY="2,0"
    INSTANCES="id=1;id=2"
    GRID_LOCATOR=$EMPTY
    GRID_GROUP="insightedge"
    GSC_SIZE="1G"
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
        "-s" | "--size")
          shift
          GSC_SIZE=$1
          ;;
        "-t" | "--topology")
          shift
          TOPOLOGY=$1
          ;;
        "-i" | "--instances")
          shift
          INSTANCES=$1
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
    if [ "$CLUSTER_MASTER" == "$EMPTY" ] && [ "$GRID_LOCATOR" == "$EMPTY" ]; then
      echo "Error: --master, --locator must be specified"
      display_usage
    fi

    if [[ "$TOPOLOGY" != "$EMPTY"  && "$INSTANCES" == "$EMPTY" ]]; then
      echo "Error: --instances instances must be specified"
      display_usage
    fi

    if [[ "$TOPOLOGY" == "$EMPTY"  && "$INSTANCES" != "$EMPTY" ]]; then
      echo "Error: --topology must be specified"
      display_usage
    fi


}

redefine_defaults() {
    if [ "$GRID_LOCATOR" == "$EMPTY" ]; then
        GRID_LOCATOR="$CLUSTER_MASTER:4174"
    fi
    if [ "$IE_PATH" == "$EMPTY" ]; then
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