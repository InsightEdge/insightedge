#!/usr/bin/env bash

# Starts the Gigaspaces Datagrid slave on the machine this script is executed on.

DIRNAME=$(dirname ${BASH_SOURCE[0]})
source ${DIRNAME}/../../bin/setenv.sh

if [ -z "$INSIGHTEDGE_LOG_DIR" ]; then
  export INSIGHTEDGE_LOG_DIR="${XAP_HOME}/insightedge/logs"
fi
THIS_SCRIPT_NAME=`basename "$0"`

start_datagrid_slave() {
    display_usage() {
        sleep 3
        echo ""
        echo "Usage: "
        echo " -c, --container |    (slave modes) number of grid containers to start           | default 2"
        echo ""
        local script="./sbin/$THIS_SCRIPT_NAME"
        echo "Examples:"
        echo "  Start datagrid |  starts 8 containers"
        echo ""
        echo " $script -c 8"
        echo ""
        exit 1
    }

    define_defaults() {
        GSC_COUNT="2"
    }

    parse_options() {
        while [ "$1" != "" ]; do
          case $1 in
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


    check_already_started() {
        pid=`ps aux | grep -v grep | grep insightedge.marker=slave | awk '{print $2}'`
        if [ ! -z $pid ]; then
            echo "Datagrid slave is already running. pid: $pid"
            exit
        fi
    }

    define_defaults
    parse_options $@
    check_already_started

    mkdir -p "$INSIGHTEDGE_LOG_DIR"
    log="$INSIGHTEDGE_LOG_DIR/insightedge-datagrid-slave.out"
    echo "Starting datagrid slave (containers: $GSC_COUNT)"
    export XAP_GSA_OPTIONS="$XAP_GSA_OPTIONS -Dinsightedge.marker=slave"
    nohup ${XAP_HOME}/bin/gs-agent.sh gsa.gsc $GSC_COUNT gsa.global.gsm 0 gsa.global.lus 0  > $log 2>&1 &
    echo "Datagrid slave started (log: $log)"
}

start_datagrid_slave "$@"