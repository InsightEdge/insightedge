#!/usr/bin/env bash

# Deploys the Gigaspaces Datagrid space on specified cluster.

DIRNAME=$(dirname ${BASH_SOURCE[0]})
source ${DIRNAME}/../../bin/setenv.sh
THIS_SCRIPT_NAME=`basename "$0"`

deploy_datagrid() {
    define_defaults() {
        SPACE_NAME="insightedge-space"
        SPACE_TOPOLOGY="2,0"
    }

    parse_options() {
        while [ "$1" != "" ]; do
          case $1 in
            "-n" | "--name")
              shift
              SPACE_NAME=$1
              ;;
            "-t" | "--topology")
              shift
              SPACE_TOPOLOGY=$1
              ;;
            *)
              echo "Unknown option: $1"
              display_usage
              ;;
          esac
          shift
        done
    }

    await_master_start() {
        TIMEOUT=60
        echo "  awaiting datagrid master ..."
        while [ -z "$(${XAP_HOME}/bin/gs.sh list 2>/dev/null | grep GSM)" ] ; do
            if [ $TIMEOUT -le 0 ]; then
              echo "Datagrid master is not available within timeout"
              exit 1
            fi
            TIMEOUT=$((TIMEOUT - 10))
            echo "  .. ($TIMEOUT sec)"
        done
    }

    display_usage() {
        sleep 3
        echo ""
        echo "Usage:"
        echo " -n, --name      |   name of the deployed space                                 | default insightedge-space"
        echo " -t, --topology  |   number of space primary and backup instances               | default 2,0"
        echo "                 |            format:  <num-of-primaries>,<num-of-backups-per-primary>"
        echo "                 |            example: '4,1' will deploy 8 instances - 4 primary and 4 backups"
        echo ""
        local script="./sbin/$THIS_SCRIPT_NAME"
        echo "Examples:"
        echo "    Deploy space |  deploys 8 primary and 8 backup partitions of 'my-space' on cluster"
        echo ""
        echo " $script -n my-space -t 8,1"
        echo ""
        exit 1
    }

    define_defaults
    parse_options $@

    echo "Deploying space: $SPACE_NAME [$SPACE_TOPOLOGY]"
    await_master_start #TODO: revisit in IE-87
    ${XAP_HOME}/bin/gs.sh deploy-space -cluster schema=partitioned-sync2backup total_members=$SPACE_TOPOLOGY $SPACE_NAME
}


deploy_datagrid "$@"