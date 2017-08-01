#!/bin/bash

DIRNAME=$(dirname ${BASH_SOURCE[0]})
source ${DIRNAME}/../../bin/setenv.sh
source ${XAP_HOME}/insightedge/sbin/common-insightedge.sh

IE_DIR_INTERNAL="${XAP_HOME}/insightedge"
EMPTY="[]"
THIS_SCRIPT_NAME=`basename "$0"`
IE_VERSION=`grep -w "Version" ${IE_DIR_INTERNAL}/VERSION | awk -F  ":" '{print $2}' | sed 's/ //'`
EDITION=`grep -w "Edition" ${IE_DIR_INTERNAL}/VERSION | awk -F  ":" '{print $2}' | sed 's/ //'`

main() {
    define_defaults
    parse_options $@
    check_options
    redefine_defaults
    display_logo
    case "$MODE" in
      "master")
        local_master
        ;;
      "slave")
        local_slave $CLUSTER_MASTER $GSC_COUNT
        ;;
      "zeppelin")
        local_zeppelin $IE_PATH $CLUSTER_MASTER
        ;;
      "demo")
        local_master
        local_slave $CLUSTER_MASTER $GSC_COUNT
        deploy_space $SPACE_NAME $SPACE_TOPOLOGY
        local_zeppelin $IE_PATH $CLUSTER_MASTER
        display_demo_help $CLUSTER_MASTER
        ;;
      "deploy")
        deploy_space $SPACE_NAME $SPACE_TOPOLOGY
        ;;
      "undeploy")
        undeploy_space $SPACE_NAME
        ;;
      "shutdown")
        shutdown_all $IE_PATH
        ;;
    esac
}

display_usage() {
    sleep 3
    echo ""
    display_logo
    echo ""
    echo "Usage: * - required, ** - required in some modes"
    echo "     --mode      |  * insightedge mode:"
    echo "                 |       master:        locally restarts spark master and grid manager"
    echo "                 |       slave:         locally restarts spark slave and grid containers"
    echo "                 |       deploy:        deploys empty space to grid"
    echo "                 |       undeploy:      undeploys space from grid"
    echo "                 |       zeppelin:      locally starts zeppelin"
    echo "                 |       demo:          locally starts datagrid master, datagrid slave and zeppelin, deploys empty space"
    echo "                 |       shutdown:      stops 'master', 'slave' and 'zeppelin'"
    echo " -m, --master    |  * cluster master IP or hostname"
    echo " -l, --locator   |    lookup locators for the grid components                    | default master:4174"
    echo " -g, --group     |    lookup groups for the grid components                      | default insightedge"
    echo "                 |               usage: if you have several clusters in one LAN,"
    echo "                 |                      it's recommended to have unique group per cluster"
    echo " -s, --size      |    grid container/manager heap size                           | default 1G"
    echo "                 |             format:  java-style heap size string"
    echo "                 |             example: '1G', '4096M'"
    echo " -c, --container |    (slave modes) number of grid containers to start           | default 2"
    echo " -n, --name      |    (deploy/undeploy modes) name of the deployed space         | default insightedge-space"
    echo " -t, --topology  |    (deploy mode) number of space primary and backup instances | default 2,0"
    echo "                 |             format:  <num-of-primaries>,<num-of-backups-per-primary>"
    echo "                 |             example: '4,1' will deploy 8 instances - 4 primary and 4 backups"
    echo ""
    local script="./sbin/$THIS_SCRIPT_NAME"
    echo "Examples:"
    echo "  Restart master |  restarts spark master at spark://127.0.0.1:7077"
    echo "        on local |  restarts spark master web UI at http://127.0.0.1:8080"
    echo "     environment |  restarts grid manager with 1G heap size"
    echo "                 |  restarts grid lookup service at 127.0.0.1:4174 with group 'insightedge'"
    echo ""
    echo " $script --mode master --path \$XAP_HOME --master 127.0.0.1"
    echo ""
    echo "   Restart slave |  restarts spark slave that points to master at spark://127.0.0.1:7077"
    echo "        on local |  restarts 2 grid containers with 1G heap size"
    echo "     environment |"
    echo ""
    echo " $script --mode slave --path \$XAP_HOME --master 127.0.0.1"
    echo ""
    echo "    Deploy empty |  deploys insightedge-space with 2 primary instances"
    echo "           space |  deploys insightedge-space with 2 primary instances"
    echo "                 |  cluster is searched with 127.0.0.1:4174 locator and 'insightedge' group"
    echo ""
    echo " $script --mode deploy --path \$XAP_HOME --master 127.0.0.1"
    echo ""
    echo "  Undeploy space |  undeploys insightedge-space"
    echo "                 |  cluster is searched with 127.0.0.1:4174 locator and 'insightedge' group"
    echo ""
    echo " $script --mode undeploy --path \$XAP_HOME --master 127.0.0.1"
    echo ""
    exit 1
}

define_defaults() {
    # '[]' means 'empty'
    MODE=$EMPTY
    IE_PATH=$EMPTY
    IE_INSTALL="false"
    CLUSTER_MASTER=$EMPTY
    GRID_LOCATOR=$EMPTY
    GRID_GROUP="insightedge"
    GSC_SIZE="1G"
    GSC_COUNT="2"
    SPACE_NAME="insightedge-space"
    SPACE_TOPOLOGY="2,0"
}

parse_options() {
    while [ "$1" != "" ]; do
      case $1 in
        "--mode")
          shift
          MODE=$1
          ;;
        "-p" | "--path")
          shift
          IE_PATH=$1
          ;;
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
    if [ "$MODE" == "$EMPTY" ]; then
      error_line "--mode is required"
      display_usage
    fi

    if [ $MODE != "master" ] && \
       [ $MODE != "slave" ] && \
       [ $MODE != "zeppelin" ] && \
       [ $MODE != "demo" ] && \
       [ $MODE != "shutdown" ] && \
       [ $MODE != "deploy" ] && \
       [ $MODE != "undeploy" ]; then
         error_line "unknown mode selected with --mode: $MODE"
         display_usage
    fi

}

redefine_defaults() {
    if [ "$CLUSTER_MASTER" = "$EMPTY" ]; then
        CLUSTER_MASTER="127.0.0.1:7077"
    fi
    if [ "$GRID_LOCATOR" = "$EMPTY" ]; then
        GRID_LOCATOR="$CLUSTER_MASTER:4174"
    fi
    if [ "$IE_PATH" = "$EMPTY" ]; then
        IE_PATH="$XAP_HOME"
    fi
}

local_master() {
    stop_grid_master
    stop_spark_master
    start_grid_master
    start_spark_master
}

local_slave() {
    local master=$1
    local containers=$2

    stop_grid_slave
    stop_spark_slave
    start_grid_slave ${containers}
    start_spark_slave ${master}
}

deploy_space() {
    local space=$1
    local topology=$2

    echo ""
    step_title "--- Deploying space: $space [$topology]"
    ${XAP_HOME}/insightedge/sbin/deploy-datagrid.sh --name $space --topology $topology
    step_title "--- Done deploying space: $space"
}

undeploy_space() {
    local space=$1

    echo ""
    step_title "--- Undeploying space: $space"
    ${XAP_HOME}/insightedge/sbin/undeploy-datagrid.sh --name $space
    step_title "--- Done undeploying space: $space"
}

shutdown_all() {
    local home=$1

    stop_zeppelin $home
    stop_grid_master $home
    stop_grid_slave
    stop_spark_master $home
    stop_spark_slave $home
}

start_grid_master() {
    echo ""
    step_title "--- Starting Gigaspaces datagrid management node"
    ${XAP_HOME}/insightedge/sbin/start-datagrid-master.sh
    step_title "--- Gigaspaces datagrid management node started"
}

stop_grid_master() {
    echo ""
    step_title "--- Stopping datagrid master"
    ${XAP_HOME}/insightedge/sbin/stop-datagrid-master.sh
    step_title "--- Datagrid master stopped"
}

start_grid_slave() {
    local containers=$1

    echo ""
    step_title "--- Starting Gigaspaces datagrid node ($containers)"
    ${XAP_HOME}/insightedge/sbin/start-datagrid-slave.sh --container ${containers}
    step_title "--- Gigaspaces datagrid node started"
}

stop_grid_slave() {

    echo ""
    step_title "--- Stopping datagrid slave instances"
    ${XAP_HOME}/insightedge/sbin/stop-datagrid-slave.sh
    step_title "--- Datagrid slave instances stopped"
}

main "$@"