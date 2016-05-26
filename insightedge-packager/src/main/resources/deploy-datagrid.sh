#!/usr/bin/env bash

# Deploys the Gigaspaces Datagrid space on specified cluster.

if [ -z "${INSIGHTEDGE_HOME}" ]; then
  export INSIGHTEDGE_HOME="$(cd "`dirname "$0"`"/..; pwd)"
fi
THIS_SCRIPT_NAME=`basename "$0"`

main() {
    define_defaults
    parse_options $@
    check_options
    redefine_defaults

    echo "Deploying space: $SPACE_NAME [$SPACE_TOPOLOGY] (locator: $GRID_LOCATOR, group: $GRID_GROUP)"
    export LOOKUPLOCATORS=$GRID_LOCATOR
    export LOOKUPGROUPS=$GRID_GROUP
    #$IE_PATH/datagrid/bin/gs.sh deploy-space -cluster schema=partitioned-sync2backup total_members=$SPACE_TOPOLOGY $SPACE_NAME
    $IE_PATH/datagrid/bin/puInstance.sh -cluster schema=partitioned-sync2backup total_members=2,1 id=1 $IE_PATH/datagrid/deploy/templates/insightedge-datagrid &
    $IE_PATH/datagrid/bin/puInstance.sh -cluster schema=partitioned-sync2backup total_members=2,1 id=2 backup_id=1 $IE_PATH/datagrid/deploy/templates/insightedge-datagrid &
    $IE_PATH/datagrid/bin/puInstance.sh -cluster schema=partitioned-sync2backup total_members=2,1 id=2 $IE_PATH/datagrid/deploy/templates/insightedge-datagrid &
    $IE_PATH/datagrid/bin/puInstance.sh -cluster schema=partitioned-sync2backup total_members=2,1 id=1 backup_id=1 $IE_PATH/datagrid/deploy/templates/insightedge-datagrid &
}

display_usage() {
    sleep 3
    echo ""
    echo "Usage: * - required"
    echo " -m, --master    | * cluster master IP or hostname (required if locator is not specified)"
    echo " -l, --locator   |   lookup locators for the grid components                    | default master:4174"
    echo " -g, --group     |   lookup groups for the grid components                      | default insightedge"
    echo "                 |              usage: if you have several clusters in one LAN,"
    echo "                 |                     it's recommended to have unique group per cluster"
    echo " -n, --name      |   name of the deployed space                                 | default insightedge-space"
    echo " -t, --topology  |   number of space primary and backup instances               | default 2,0"
    echo "                 |            format:  <num-of-primaries>,<num-of-backups-per-primary>"
    echo "                 |            example: '4,1' will deploy 8 instances - 4 primary and 4 backups"
    echo ""
    local script="./sbin/$THIS_SCRIPT_NAME"
    echo "Examples:"
    echo "    Deploy space |  deploys 8 primary and 8 backup partitions of 'my-space' on cluster with 10.0.0.1 as a master"
    echo ""
    echo " $script -m 10.0.0.1 -n my-space -t 8,1"
    echo ""
    exit 1
}

define_defaults() {
    # '[]' means 'empty'
    IE_PATH="[]"
    CLUSTER_MASTER="[]"
    GRID_LOCATOR="[]"
    GRID_GROUP="insightedge"
    SPACE_NAME="insightedge-space"
    SPACE_TOPOLOGY="2,0"
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

main "$@"