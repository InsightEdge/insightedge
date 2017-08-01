#!/usr/bin/env bash

# Starts the Gigaspaces Datagrid master on the machine this script is executed on.

DIRNAME=$(dirname ${BASH_SOURCE[0]})
source ${DIRNAME}/../../bin/setenv.sh

if [ -z "$INSIGHTEDGE_LOG_DIR" ]; then
  export INSIGHTEDGE_LOG_DIR="${XAP_HOME}/insightedge/logs"
fi

THIS_SCRIPT_NAME=`basename "$0"`

start_datagrid_master() {
    display_usage() {
        sleep 3
        echo ""
        echo "Usage: "
        echo ""
        local script="./sbin/$THIS_SCRIPT_NAME"
        echo " $script"
        echo ""
        exit 1
    }

    parse_options() {
        while [ "$1" != "" ]; do
          case $1 in
            "--mode")
              shift
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
        pid=`ps aux | grep -v grep | grep insightedge.marker=master | awk '{print $2}'`
        if [ ! -z $pid ]; then
            echo "Datagrid master is already running. pid: $pid"
            exit
        fi
    }

    parse_options $@
    check_already_started

    mkdir -p "$INSIGHTEDGE_LOG_DIR"
    log="$INSIGHTEDGE_LOG_DIR/insightedge-datagrid-master.out"
    echo "Starting datagrid master"
    export XAP_GSA_OPTIONS="$XAP_GSA_OPTIONS -Dinsightedge.marker=master"
    nohup ${XAP_HOME}/bin/gs-agent.sh gsa.gsc 0 gsa.global.gsm 0 gsa.gsm 1 gsa.global.lus 0 gsa.lus 1 > $log 2>&1 &
    echo "Datagrid master started (log: $log)"
}

start_datagrid_master "$@"