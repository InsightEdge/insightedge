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
        local_master $@
        ;;
      "slave")
        local_slave $@
        ;;
      "zeppelin")
        local_zeppelin 127.0.0.1 #TODO
        ;;
      "demo")
        local_master $@
        local_slave $@
        deploy_space $@
        local_zeppelin 127.0.0.1 #TODO
        display_demo_help 127.0.0.1 #TODO
        ;;
      "deploy")
        deploy_space $@
        ;;
      "undeploy")
        undeploy_space $@
        ;;
      "shutdown")
        shutdown_all
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
    echo " $script --mode master"
    echo ""
    echo "   Restart slave |  restarts spark slave that points to master at spark://127.0.0.1:7077"
    echo "        on local |  restarts 2 grid containers with 1G heap size"
    echo "     environment |"
    echo ""
    echo " $script --mode slave --master 127.0.0.1"
    echo ""
    echo "    Deploy empty |  deploys insightedge-space with 2 primary instances"
    echo "           space |  deploys insightedge-space with 2 primary instances"
    echo "                 |  cluster is searched with 127.0.0.1:4174 locator and 'insightedge' group"
    echo ""
    echo " $script --mode deploy --master 127.0.0.1"
    echo ""
    echo "  Undeploy space |  undeploys insightedge-space"
    echo "                 |  cluster is searched with 127.0.0.1:4174 locator and 'insightedge' group"
    echo ""
    echo " $script --mode undeploy"
    echo ""
    exit 1
}

define_defaults() {
    # '[]' means 'empty'
    MODE=$EMPTY
    CLUSTER_MASTER=$EMPTY
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
        "-m" | "--master")
          shift
          CLUSTER_MASTER=$1
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
        CLUSTER_MASTER="127.0.0.1"
    fi
}

local_master() {
    stop_grid_master
    stop_spark_master
    start_grid_master $@
    start_spark_master ${CLUSTER_MASTER}
}

local_slave() {

    stop_grid_slave
    stop_spark_slave
    start_grid_slave $@
    start_spark_slave ${CLUSTER_MASTER}
}

deploy_space() {
    echo ""
#    step_title "--- Deploying space: $space [$topology]"
    ${XAP_HOME}/insightedge/sbin/deploy-datagrid.sh $@
#    step_title "--- Done deploying space: $space"
}

undeploy_space() {
    echo ""
#    step_title "--- Undeploying space: $space"
    ${XAP_HOME}/insightedge/sbin/undeploy-datagrid.sh $@
#    step_title "--- Done undeploying space: $space"
}

shutdown_all() {
    stop_zeppelin
    stop_grid_master
    stop_grid_slave
    stop_spark_master
    stop_spark_slave
}

start_grid_master() {
    echo ""
    step_title "--- Starting Gigaspaces datagrid management node"
    ${XAP_HOME}/insightedge/sbin/start-datagrid-master.sh $@
    step_title "--- Gigaspaces datagrid management node started"
}

stop_grid_master() {
    echo ""
    step_title "--- Stopping datagrid master"
    ${XAP_HOME}/insightedge/sbin/stop-datagrid-master.sh
    step_title "--- Datagrid master stopped"
}

start_grid_slave() {
    echo ""
#    step_title "--- Starting Gigaspaces datagrid node ($containers)"
    ${XAP_HOME}/insightedge/sbin/start-datagrid-slave.sh $@
#    step_title "--- Gigaspaces datagrid node started"
}

stop_grid_slave() {

    echo ""
    step_title "--- Stopping datagrid slave instances"
    ${XAP_HOME}/insightedge/sbin/stop-datagrid-slave.sh
    step_title "--- Datagrid slave instances stopped"
}

main "$@"