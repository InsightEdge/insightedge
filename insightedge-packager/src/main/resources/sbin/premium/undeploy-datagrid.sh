#!/usr/bin/env bash

# Deploys the Datagrid space on specified cluster.

if [ -z "${INSIGHTEDGE_HOME}" ]; then
  export INSIGHTEDGE_HOME="$(cd "`dirname "$0"`"/..; pwd)"
fi
THIS_SCRIPT_NAME=`basename "$0"`

main() {
    define_defaults
    parse_options $@
    check_options
    redefine_defaults

    echo "Undeploying space: $SPACE_NAME (locator: $GRID_LOCATOR, group: $GRID_GROUP)"
    export XAP_LOOKUP_LOCATORS=$GRID_LOCATOR
    export XAP_LOOKUP_GROUPS=$GRID_GROUP
    $IE_PATH/datagrid/bin/gs.sh undeploy $SPACE_NAME
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
    echo ""
    local script="./sbin/$THIS_SCRIPT_NAME"
    echo "Examples:"
    echo "  Undeploy space |  undeploys 'my-space' from cluster with 10.0.0.1 as a master"
    echo ""
    echo " $script -m 10.0.0.1 -n my-space"
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