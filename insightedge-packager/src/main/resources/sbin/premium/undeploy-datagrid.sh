#!/usr/bin/env bash

# Deploys the Gigaspaces Datagrid space on specified cluster.
DIRNAME=$(dirname ${BASH_SOURCE[0]})
source ${DIRNAME}/../../bin/setenv.sh

THIS_SCRIPT_NAME=`basename "$0"`

main() {
    define_defaults
    parse_options $@

    echo "Undeploying space: $SPACE_NAME"
    ${XAP_HOME}/bin/gs.sh undeploy $SPACE_NAME
}

display_usage() {
    sleep 3
    echo ""
    echo "Usage: * - required"
    echo " -n, --name      |   name of the deployed space                                 | default insightedge-space"
    echo ""
    local script="./sbin/$THIS_SCRIPT_NAME"
    echo "Examples:"
    echo "  Undeploy space |  undeploys 'my-space' from cluster"
    echo ""
    echo " $script -m 10.0.0.1 -n my-space"
    echo ""
    exit 1
}

define_defaults() {
    SPACE_NAME="insightedge-space"
}

parse_options() {
    while [ "$1" != "" ]; do
      case $1 in
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


main "$@"